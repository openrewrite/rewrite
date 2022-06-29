      *HEADER,COBOL,IC222A                                                            
000100 IDENTIFICATION DIVISION.                                         IC2224.2
000200 PROGRAM-ID.                                                      IC2224.2
000300     IC222A.                                                      IC2224.2
000400****************************************************************  IC2224.2
000500*                                                              *  IC2224.2
000600*    VALIDATION FOR:-                                          *  IC2224.2
000700*                                                              *  IC2224.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2224.2
000900*                                                              *  IC2224.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2224.2
001100*                                                              *  IC2224.2
001200****************************************************************  IC2224.2
001300*                                                              *  IC2224.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2224.2
001500*                                                              *  IC2224.2
001600*            X-55   SYSTEM PRINTER                             *  IC2224.2
001700*            X-82   SOURCE-COMPUTER                            *  IC2224.2
001800*            X-83   OBJECT-COMPUTER.                           *  IC2224.2
001900*                                                              *  IC2224.2
002000****************************************************************  IC2224.2
002100*                                                              *  IC2224.2
002200*    THE SOURCE FILE CONTAINS TWO PROGRAMS, IC222A AND         *  IC2224.2
002300*    IC222A-1, WHICH TEST LANGUAGE ELEMENTS FROM  LEVEL 2 OF   *  IC2224.2
002400*    THE INTER-PROGRAM COMMUNICATION MODULE.  THE LANGUAGE     *  IC2224.2
002500*    ELEMENTS TESTED ARE:                                      *  IC2224.2
002600*          "ON EXCEPTION"     PHRASE                           *  IC2224.2
002700*          "NOT ON EXCEPTION" PHRASE                           *  IC2224.2
002800*          "END-CALL"         PHRASE                           *  IC2224.2
002900*          "ON OVERFLOW"      PHRASE                           *  IC2224.2
003000*                                                              *  IC2224.2
003100*    THE TWO PROGRAMS SHOULD BE COMPILED IN THE SAME           *  IC2224.2
003200*    INVOCATION OF THE COMPILER TO TEST THE BATCH COMPILATION  *  IC2224.2
003300*    FEATURE AND RECOGNITION OF THE END PROGRAM HEADER.  THE   *  IC2224.2
003400*    ARRANGEMENT OF THE PROGRAMS IN THE SOURCE FILE IS:        *  IC2224.2
003500*                                                              *  IC2224.2
003600*    IDENTIFICATION DIVISION.                                  *  IC2224.2
003700*    PROGRAM-ID. IC222A.                                       *  IC2224.2
003800*          .                                                   *  IC2224.2
003900*          .                                                   *  IC2224.2
004000*          .                                                   *  IC2224.2
004100*    END PROGRAM IC222A.                                          IC2224.2
004200*    IDENTIFICATION DIVISION.                                     IC2224.2
004300*    PROGRAM-ID. IC222A-1.                                        IC2224.2
004400*          .                                                   *  IC2224.2
004500*          .                                                   *  IC2224.2
004600*          .                                                   *  IC2224.2
004700*                                                              *  IC2224.2
004800*    IC222A, THE FIRST PROGRAM IN THE FILE, CONTAINS THE       *  IC2224.2
004900*    SUBSTANTIVE TESTS.  THE ONLY FUNCTION OF THE OTHER        *  IC2224.2
005000*    PROGRAM IS TO ENSURE THAT A PROGRAM WITH KNOWN PARAMETER  *  IC2224.2
005100*    REQUIREMENTS IS AVAILABLE TO BE CALLED.  IC222A TESTS     *  IC2224.2
005200*    CONTROL FLOW THROUGH VARIANTS OF THE CALL STATEMENT WITH  *  IC2224.2
005300*    THE "ON EXCEPTION" PHRASE PRESENT OR ABSENT; THE "NOT ON  *  IC2224.2
005400*    EXCEPTION" PHRASE PRESENT OR ABSENT; AND AVAILABLE OR     *  IC2224.2
005500*    NON-AVAIABLE TARGET PROGRAMS.  EACH CALL STATEMENT HAS AN *  IC2224.2
005600*    END-CALL PHRASE, AND THERE ARE SECONDARY TESTS WHICH      *  IC2224.2
005700*    CHECK THAT STATEMENTS FOLLOWING END-CALL ARE PROPERLY     *  IC2224.2
005800*    EXECUTED.                                                 *  IC2224.2
005900*                                                                 IC2224.2
006000*    THIS TEST SET DOES NOT EXAMINE THE RESULTS RETURNED BY    *  IC2224.2
006100*    THE CALLED PROGRAM, BUT IS WHOLLY CONCERNED WITH THE FLOW *  IC2224.2
006200*    OF CONTROL IN THE CALLING PROGRAM DURING EXECUTION OF A   *  IC2224.2
006300*    CALL STATEMENT.                                           *  IC2224.2
006400*                                                              *  IC2224.2
006500*    THERE ARE EIGHT POSIBLE COMBINATIONS OF CALL STATEMENT    *  IC2224.2
006600*    FORMAT AND CALLED PROGRAM AVAILABILITY THAT COULD BE      *  IC2224.2
006700*    TESTED.  TWO OF THESE COMBINATIONS, THOSE WHERE A PROGRAM *  IC2224.2
006800*    WHICH IS NOT AVAILABLE IS CALLED THROUGH  A STATEMENT     *  IC2224.2
006900*    WHICH DOES NOT CONTAIN AN "ON EXCEPTION" PHRASE, PRODUCE  *  IC2224.2
007000*    EFFECTS WHICH THE STANDARD LEAVES UNDEFINED.  THUS THERE  *  IC2224.2
007100*    ARE SIX CASES WHICH CAN BE TESTED.  THIS TEST SUITE TESTS *  IC2224.2
007200*    ALL SIX.  IN ADDITION, IT TESTS THE TWO CASES WHERE       *  IC2224.2
007300*    "ON OVERFLOW" CAN BE USED IN PLACE OF "ON EXCEPTION".     *  IC2224.2
007400*    EACH OF THE EIGHT MAJOR TESTS IS FOLLOWED BY A            *  IC2224.2
007500*    SUBORDINATE TEST WHICH IS INTENDED TO CHECK THE WAY       *  IC2224.2
007600*    THAT CONTROL HAS FLOWED THROUGH THE PHRASES OF THE CALL   *  IC2224.2
007700*    STATEMENT.  EVERY CALL STATEMENT IN IC222A HAS AN         *  IC2224.2
007800*    "END-CALL" SCOPE DELIMITER.  THIS SCOPE DELIMITER IS      *  IC2224.2
007900*    FOLLOWED BY ONE MORE STATEMENT IN THE SENTENCE, AND THE   *  IC2224.2
008000*    SUBORDINATE TESTS CHECK THAT THIS ADDITIONAL STATEMENT    *  IC2224.2
008100*    HAS BEEN EXECUTED.                                        *  IC2224.2
008200*                                                              *  IC2224.2
008300****************************************************************  IC2224.2
008400*                                                                 IC2224.2
008500 ENVIRONMENT DIVISION.                                            IC2224.2
008600 CONFIGURATION SECTION.                                           IC2224.2
008700 SOURCE-COMPUTER.                                                 IC2224.2
008800     XXXXX082.                                                    IC2224.2
008900 OBJECT-COMPUTER.                                                 IC2224.2
009000     XXXXX083.                                                    IC2224.2
009100 INPUT-OUTPUT SECTION.                                            IC2224.2
009200 FILE-CONTROL.                                                    IC2224.2
009300     SELECT PRINT-FILE ASSIGN TO                                  IC2224.2
009400     XXXXX055.                                                    IC2224.2
009500*                                                                 IC2224.2
009600 DATA DIVISION.                                                   IC2224.2
009700 FILE SECTION.                                                    IC2224.2
009800 FD  PRINT-FILE.                                                  IC2224.2
009900 01  PRINT-REC PICTURE X(120).                                    IC2224.2
010000 01  DUMMY-RECORD PICTURE X(120).                                 IC2224.2
010100*                                                                 IC2224.2
010200 WORKING-STORAGE SECTION.                                         IC2224.2
010300 77  DN1 PICTURE S99  VALUE ZERO.                                 IC2224.2
010400 77  DN3 PICTURE S99.                                             IC2224.2
010500 77  ID1 PICTURE X(8) VALUE "IC222A-1".                           IC2224.2
010600 77  ID2 PICTURE X(8).                                            IC2224.2
010700 77  DN2 PICTURE S99                                              IC2224.2
010800         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2224.2
010900 77  DN4 PICTURE S99                                              IC2224.2
011000         USAGE IS COMPUTATIONAL.                                  IC2224.2
011100 77  CALL-FLAG PIC 9.                                             IC2224.2
011200 01  EXCEPTION-PATH-FLAG PICTURE X.                               IC2224.2
011300*                                                                 IC2224.2
011400 01  TEST-RESULTS.                                                IC2224.2
011500     02 FILLER                   PIC X      VALUE SPACE.          IC2224.2
011600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC2224.2
011700     02 FILLER                   PIC X      VALUE SPACE.          IC2224.2
011800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC2224.2
011900     02 FILLER                   PIC X      VALUE SPACE.          IC2224.2
012000     02  PAR-NAME.                                                IC2224.2
012100       03 FILLER                 PIC X(19)  VALUE SPACE.          IC2224.2
012200       03  PARDOT-X              PIC X      VALUE SPACE.          IC2224.2
012300       03 DOTVALUE               PIC 99     VALUE ZERO.           IC2224.2
012400     02 FILLER                   PIC X(8)   VALUE SPACE.          IC2224.2
012500     02 RE-MARK                  PIC X(61).                       IC2224.2
012600 01  TEST-COMPUTED.                                               IC2224.2
012700     02 FILLER                   PIC X(30)  VALUE SPACE.          IC2224.2
012800     02 FILLER                   PIC X(17)  VALUE                 IC2224.2
012900            "       COMPUTED=".                                   IC2224.2
013000     02 COMPUTED-X.                                               IC2224.2
013100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC2224.2
013200     03 COMPUTED-N               REDEFINES COMPUTED-A             IC2224.2
013300                                 PIC -9(9).9(9).                  IC2224.2
013400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC2224.2
013500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC2224.2
013600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC2224.2
013700     03       CM-18V0 REDEFINES COMPUTED-A.                       IC2224.2
013800         04 COMPUTED-18V0                    PIC -9(18).          IC2224.2
013900         04 FILLER                           PIC X.               IC2224.2
014000     03 FILLER PIC X(50) VALUE SPACE.                             IC2224.2
014100 01  TEST-CORRECT.                                                IC2224.2
014200     02 FILLER PIC X(30) VALUE SPACE.                             IC2224.2
014300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC2224.2
014400     02 CORRECT-X.                                                IC2224.2
014500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC2224.2
014600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC2224.2
014700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC2224.2
014800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC2224.2
014900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC2224.2
015000     03      CR-18V0 REDEFINES CORRECT-A.                         IC2224.2
015100         04 CORRECT-18V0                     PIC -9(18).          IC2224.2
015200         04 FILLER                           PIC X.               IC2224.2
015300     03 FILLER PIC X(2) VALUE SPACE.                              IC2224.2
015400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC2224.2
015500 01  CCVS-C-1.                                                    IC2224.2
015600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC2224.2
015700-    "SS  PARAGRAPH-NAME                                          IC2224.2
015800-    "       REMARKS".                                            IC2224.2
015900     02 FILLER                     PIC X(20)    VALUE SPACE.      IC2224.2
016000 01  CCVS-C-2.                                                    IC2224.2
016100     02 FILLER                     PIC X        VALUE SPACE.      IC2224.2
016200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC2224.2
016300     02 FILLER                     PIC X(15)    VALUE SPACE.      IC2224.2
016400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC2224.2
016500     02 FILLER                     PIC X(94)    VALUE SPACE.      IC2224.2
016600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC2224.2
016700 01  REC-CT                        PIC 99       VALUE ZERO.       IC2224.2
016800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC2224.2
016900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC2224.2
017000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC2224.2
017100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC2224.2
017200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC2224.2
017300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC2224.2
017400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC2224.2
017500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC2224.2
017600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC2224.2
017700 01  CCVS-H-1.                                                    IC2224.2
017800     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2224.2
017900     02  FILLER                    PIC X(42)    VALUE             IC2224.2
018000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC2224.2
018100     02  FILLER                    PIC X(39)    VALUE SPACES.     IC2224.2
018200 01  CCVS-H-2A.                                                   IC2224.2
018300   02  FILLER                        PIC X(40)  VALUE SPACE.      IC2224.2
018400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC2224.2
018500   02  FILLER                        PIC XXXX   VALUE             IC2224.2
018600     "4.2 ".                                                      IC2224.2
018700   02  FILLER                        PIC X(28)  VALUE             IC2224.2
018800            " COPY - NOT FOR DISTRIBUTION".                       IC2224.2
018900   02  FILLER                        PIC X(41)  VALUE SPACE.      IC2224.2
019000                                                                  IC2224.2
019100 01  CCVS-H-2B.                                                   IC2224.2
019200   02  FILLER                        PIC X(15)  VALUE             IC2224.2
019300            "TEST RESULT OF ".                                    IC2224.2
019400   02  TEST-ID                       PIC X(9).                    IC2224.2
019500   02  FILLER                        PIC X(4)   VALUE             IC2224.2
019600            " IN ".                                               IC2224.2
019700   02  FILLER                        PIC X(12)  VALUE             IC2224.2
019800     " HIGH       ".                                              IC2224.2
019900   02  FILLER                        PIC X(22)  VALUE             IC2224.2
020000            " LEVEL VALIDATION FOR ".                             IC2224.2
020100   02  FILLER                        PIC X(58)  VALUE             IC2224.2
020200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2224.2
020300 01  CCVS-H-3.                                                    IC2224.2
020400     02  FILLER                      PIC X(34)  VALUE             IC2224.2
020500            " FOR OFFICIAL USE ONLY    ".                         IC2224.2
020600     02  FILLER                      PIC X(58)  VALUE             IC2224.2
020700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2224.2
020800     02  FILLER                      PIC X(28)  VALUE             IC2224.2
020900            "  COPYRIGHT   1985,1986 ".                           IC2224.2
021000 01  CCVS-E-1.                                                    IC2224.2
021100     02 FILLER                       PIC X(52)  VALUE SPACE.      IC2224.2
021200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC2224.2
021300     02 ID-AGAIN                     PIC X(9).                    IC2224.2
021400     02 FILLER                       PIC X(45)  VALUE SPACES.     IC2224.2
021500 01  CCVS-E-2.                                                    IC2224.2
021600     02  FILLER                      PIC X(31)  VALUE SPACE.      IC2224.2
021700     02  FILLER                      PIC X(21)  VALUE SPACE.      IC2224.2
021800     02 CCVS-E-2-2.                                               IC2224.2
021900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC2224.2
022000         03 FILLER                   PIC X      VALUE SPACE.      IC2224.2
022100         03 ENDER-DESC               PIC X(44)  VALUE             IC2224.2
022200            "ERRORS ENCOUNTERED".                                 IC2224.2
022300 01  CCVS-E-3.                                                    IC2224.2
022400     02  FILLER                      PIC X(22)  VALUE             IC2224.2
022500            " FOR OFFICIAL USE ONLY".                             IC2224.2
022600     02  FILLER                      PIC X(12)  VALUE SPACE.      IC2224.2
022700     02  FILLER                      PIC X(58)  VALUE             IC2224.2
022800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2224.2
022900     02  FILLER                      PIC X(8)   VALUE SPACE.      IC2224.2
023000     02  FILLER                      PIC X(20)  VALUE             IC2224.2
023100             " COPYRIGHT 1985,1986".                              IC2224.2
023200 01  CCVS-E-4.                                                    IC2224.2
023300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC2224.2
023400     02 FILLER                       PIC X(4)   VALUE " OF ".     IC2224.2
023500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC2224.2
023600     02 FILLER                       PIC X(40)  VALUE             IC2224.2
023700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC2224.2
023800 01  XXINFO.                                                      IC2224.2
023900     02 FILLER                       PIC X(19)  VALUE             IC2224.2
024000            "*** INFORMATION ***".                                IC2224.2
024100     02 INFO-TEXT.                                                IC2224.2
024200       04 FILLER                     PIC X(8)   VALUE SPACE.      IC2224.2
024300       04 XXCOMPUTED                 PIC X(20).                   IC2224.2
024400       04 FILLER                     PIC X(5)   VALUE SPACE.      IC2224.2
024500       04 XXCORRECT                  PIC X(20).                   IC2224.2
024600     02 INF-ANSI-REFERENCE           PIC X(48).                   IC2224.2
024700 01  HYPHEN-LINE.                                                 IC2224.2
024800     02 FILLER  PIC IS X VALUE IS SPACE.                          IC2224.2
024900     02 FILLER  PIC IS X(65)    VALUE IS "************************IC2224.2
025000-    "*****************************************".                 IC2224.2
025100     02 FILLER  PIC IS X(54)    VALUE IS "************************IC2224.2
025200-    "******************************".                            IC2224.2
025300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC2224.2
025400     "IC222A".                                                    IC2224.2
025500*                                                                 IC2224.2
025600 PROCEDURE DIVISION.                                              IC2224.2
025700 CCVS1 SECTION.                                                   IC2224.2
025800 OPEN-FILES.                                                      IC2224.2
025900     OPEN    OUTPUT PRINT-FILE.                                   IC2224.2
026000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IC2224.2
026100     MOVE    SPACE TO TEST-RESULTS.                               IC2224.2
026200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IC2224.2
026300     GO TO CCVS1-EXIT.                                            IC2224.2
026400 CLOSE-FILES.                                                     IC2224.2
026500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC2224.2
026600 TERMINATE-CCVS.                                                  IC2224.2
026700*    EXIT PROGRAM.                                                IC2224.2
026800*TERMINATE-CALL.                                                  IC2224.2
026900     STOP     RUN.                                                IC2224.2
027000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC2224.2
027100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC2224.2
027200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC2224.2
027300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC2224.2
027400     MOVE "****TEST DELETED****" TO RE-MARK.                      IC2224.2
027500 PRINT-DETAIL.                                                    IC2224.2
027600     IF REC-CT NOT EQUAL TO ZERO                                  IC2224.2
027700             MOVE "." TO PARDOT-X                                 IC2224.2
027800             MOVE REC-CT TO DOTVALUE.                             IC2224.2
027900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC2224.2
028000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC2224.2
028100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC2224.2
028200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC2224.2
028300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC2224.2
028400     MOVE SPACE TO CORRECT-X.                                     IC2224.2
028500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC2224.2
028600     MOVE     SPACE TO RE-MARK.                                   IC2224.2
028700 HEAD-ROUTINE.                                                    IC2224.2
028800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2224.2
028900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC2224.2
029000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2224.2
029100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC2224.2
029200 COLUMN-NAMES-ROUTINE.                                            IC2224.2
029300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2224.2
029400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2224.2
029500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC2224.2
029600 END-ROUTINE.                                                     IC2224.2
029700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC2224.2
029800 END-RTN-EXIT.                                                    IC2224.2
029900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2224.2
030000 END-ROUTINE-1.                                                   IC2224.2
030100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC2224.2
030200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC2224.2
030300      ADD PASS-COUNTER TO ERROR-HOLD.                             IC2224.2
030400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC2224.2
030500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC2224.2
030600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC2224.2
030700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC2224.2
030800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC2224.2
030900  END-ROUTINE-12.                                                 IC2224.2
031000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC2224.2
031100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC2224.2
031200         MOVE "NO " TO ERROR-TOTAL                                IC2224.2
031300         ELSE                                                     IC2224.2
031400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC2224.2
031500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC2224.2
031600     PERFORM WRITE-LINE.                                          IC2224.2
031700 END-ROUTINE-13.                                                  IC2224.2
031800     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC2224.2
031900         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC2224.2
032000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC2224.2
032100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC2224.2
032200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2224.2
032300      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC2224.2
032400          MOVE "NO " TO ERROR-TOTAL                               IC2224.2
032500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC2224.2
032600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC2224.2
032700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC2224.2
032800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC2224.2
032900 WRITE-LINE.                                                      IC2224.2
033000     ADD 1 TO RECORD-COUNT.                                       IC2224.2
033100*    IF RECORD-COUNT GREATER 50                                   IC2224.2
033200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC2224.2
033300*        MOVE SPACE TO DUMMY-RECORD                               IC2224.2
033400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC2224.2
033500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC2224.2
033600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC2224.2
033700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC2224.2
033800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC2224.2
033900*        MOVE ZERO TO RECORD-COUNT.                               IC2224.2
034000     PERFORM WRT-LN.                                              IC2224.2
034100 WRT-LN.                                                          IC2224.2
034200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC2224.2
034300     MOVE SPACE TO DUMMY-RECORD.                                  IC2224.2
034400 BLANK-LINE-PRINT.                                                IC2224.2
034500     PERFORM WRT-LN.                                              IC2224.2
034600 FAIL-ROUTINE.                                                    IC2224.2
034700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC2224.2
034800     IF   CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.  IC2224.2
034900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2224.2
035000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC2224.2
035100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2224.2
035200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2224.2
035300     GO TO  FAIL-ROUTINE-EX.                                      IC2224.2
035400 FAIL-ROUTINE-WRITE.                                              IC2224.2
035500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC2224.2
035600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC2224.2
035700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC2224.2
035800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC2224.2
035900 FAIL-ROUTINE-EX. EXIT.                                           IC2224.2
036000 BAIL-OUT.                                                        IC2224.2
036100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC2224.2
036200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC2224.2
036300 BAIL-OUT-WRITE.                                                  IC2224.2
036400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC2224.2
036500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC2224.2
036600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC2224.2
036700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC2224.2
036800 BAIL-OUT-EX. EXIT.                                               IC2224.2
036900 CCVS1-EXIT.                                                      IC2224.2
037000     EXIT.                                                        IC2224.2
037100*                                                                 IC2224.2
037200 SECT-IC222A-001 SECTION.                                         IC2224.2
037300 CALL-INIT-1.                                                     IC2224.2
037400****************************************************************  IC2224.2
037500*                                                              *  IC2224.2
037600*    CALL A PROGRAM WHICH EXISTS AND FOR WHICH PARAMETERS      *  IC2224.2
037700*    MATCH IN NUMBER AND TYPE.  EXECUTION SHOULD BE SUCCESSFUL *  IC2224.2
037800*    AND THE STATEMENTS IN THE "ON EXCEPTION" PATH IGNORED.    *  IC2224.2
037900*    THE STATEMENT FOLLOWING THE SCOPE TERMINATOR SHOULD BE    *  IC2224.2
038000*    EXECUTED.                                                 *  IC2224.2
038100*                                                              *  IC2224.2
038200****************************************************************  IC2224.2
038300*                                                                 IC2224.2
038400     MOVE    1 TO REC-CT.                                         IC2224.2
038500     MOVE   "CALL-TEST-1" TO PAR-NAME.                            IC2224.2
038600     MOVE   "AVAILABLE ON " TO FEATURE.                           IC2224.2
038700     MOVE    0 TO CALL-FLAG.                                      IC2224.2
038800     MOVE   "P" TO EXCEPTION-PATH-FLAG.                           IC2224.2
038900     MOVE   "X-27 5.2.4 (2)" TO ANSI-REFERENCE.                   IC2224.2
039000     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
039100     GO TO   CALL-TEST-1-1.                                       IC2224.2
039200 CALL-DELETE-1-1.                                                 IC2224.2
039300     PERFORM DE-LETE.                                             IC2224.2
039400     PERFORM PRINT-DETAIL.                                        IC2224.2
039500     ADD     1 TO REC-CT.                                         IC2224.2
039600*                                                                 IC2224.2
039700*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE IS           *  IC2224.2
039800*    AUTOMATICALLY DELETED.                                    *  IC2224.2
039900*                                                                 IC2224.2
040000     GO TO   CALL-DELETE-1-2.                                     IC2224.2
040100 CALL-TEST-1-1.                                                   IC2224.2
040200     CALL   "IC222A-1" USING DN1, DN2, DN3, DN4                   IC2224.2
040300             ON EXCEPTION                                         IC2224.2
040400                  MOVE "F" TO EXCEPTION-PATH-FLAG                 IC2224.2
040500     END-CALL                                                     IC2224.2
040600     MOVE    1 TO CALL-FLAG.                                      IC2224.2
040700     IF EXCEPTION-PATH-FLAG NOT = "P"                             IC2224.2
040800             MOVE "UNEXPECTED EXECUTION OF EXCEPTION PATH"        IC2224.2
040900                      TO RE-MARK                                  IC2224.2
041000             MOVE "P" TO CORRECT-A                                IC2224.2
041100             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
041200             PERFORM FAIL                                         IC2224.2
041300     ELSE                                                         IC2224.2
041400             PERFORM PASS.                                        IC2224.2
041500 CALL-WRITE-1-1.                                                  IC2224.2
041600     PERFORM PRINT-DETAIL.                                        IC2224.2
041700     ADD 1 TO REC-CT.                                             IC2224.2
041800*                                                                 IC2224.2
041900 CALL-INIT-1-2.                                                   IC2224.2
042000     GO TO    CALL-TEST-1-2.                                      IC2224.2
042100 CALL-DELETE-1-2.                                                 IC2224.2
042200     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
042300     PERFORM DE-LETE.                                             IC2224.2
042400     PERFORM PRINT-DETAIL.                                        IC2224.2
042500     ADD     1 TO REC-CT.                                         IC2224.2
042600     GO TO   CALL-EXIT-1.                                         IC2224.2
042700*                                                                 IC2224.2
042800 CALL-TEST-1-2.                                                   IC2224.2
042900****************************************************************  IC2224.2
043000*                                                              *  IC2224.2
043100*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
043200*    WAS EXECUTED.  IF THE PREVIOUS TEST PASSED, A PASS HERE   *  IC2224.2
043300*    INDICATES THAT THE SCOPE TERMINATOR HAS BEEN INTERPRETED  *  IC2224.2
043400*    CORRECTLY.  IF THE PREVIOUS TEST FAILED, A PASS HERE      *  IC2224.2
043500*    INDICATES THAT THE SCOPE TERMINATOR WAS NOT INTERPRETED   *  IC2224.2
043600*    AS "NOT ON EXCEPTION" OR "GO TO NEXT-SENTENCE".           *  IC2224.2
043700*                                                              *  IC2224.2
043800****************************************************************  IC2224.2
043900*                                                                 IC2224.2
044000     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
044100     IF      CALL-FLAG = 1                                        IC2224.2
044200             PERFORM PASS                                         IC2224.2
044300     ELSE                                                         IC2224.2
044400             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
044500             MOVE    1 TO CORRECT-N                               IC2224.2
044600             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
044700             PERFORM FAIL.                                        IC2224.2
044800     PERFORM PRINT-DETAIL.                                        IC2224.2
044900*                                                                 IC2224.2
045000 CALL-EXIT-1.                                                     IC2224.2
045100*                                                                 IC2224.2
045200*                                                                 IC2224.2
045300 CALL-INIT-2.                                                     IC2224.2
045400****************************************************************  IC2224.2
045500*                                                              *  IC2224.2
045600*    CALL A PROGRAM WHICH DOES NOT EXIST.  PAGE X-28, 5.2.4,   *  IC2224.2
045700*    RULE (3)A STATES THAT IF A PROGRAM CANNOT BE MADE         *  IC2224.2
045800*    AVAILABLE THEN THE STATEMENTS IN THE "ON EXCEPTION"       *  IC2224.2
045900*    PHRASE MUST BE EXECUTED.                                  *  IC2224.2
046000*                                                              *  IC2224.2
046100****************************************************************  IC2224.2
046200*                                                                 IC2224.2
046300     MOVE    1 TO REC-CT.                                         IC2224.2
046400     MOVE   "CALL-TEST-2" TO PAR-NAME.                            IC2224.2
046500     MOVE   "NO PROGRAM ON " TO FEATURE.                          IC2224.2
046600     MOVE    0 TO CALL-FLAG.                                      IC2224.2
046700     MOVE   "X" TO EXCEPTION-PATH-FLAG.                           IC2224.2
046800     MOVE   "X-28 5.2.4 (3)A" TO ANSI-REFERENCE.                  IC2224.2
046900     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
047000     GO TO   CALL-TEST-2-1.                                       IC2224.2
047100 CALL-DELETE-2-1.                                                 IC2224.2
047200     PERFORM DE-LETE.                                             IC2224.2
047300     PERFORM PRINT-DETAIL.                                        IC2224.2
047400     ADD     1 TO REC-CT.                                         IC2224.2
047500*                                                                 IC2224.2
047600*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE TEST IS      *  IC2224.2
047700*    AUTOMATICALLY DELETED.                                    *  IC2224.2
047800*                                                                 IC2224.2
047900     GO TO   CALL-DELETE-2-2.                                     IC2224.2
048000 CALL-TEST-2-1.                                                   IC2224.2
048100*    CALL "NON-EXISTING-PROGRAM"                                  IC2224.2
048200     CALL   "XXXXXXXX" USING DN1, DN2, DN3, DN4                   IC2224.2
048300             ON EXCEPTION                                         IC2224.2
048400                  MOVE "P" TO EXCEPTION-PATH-FLAG                 IC2224.2
048500     END-CALL                                                     IC2224.2
048600     MOVE    1 TO CALL-FLAG.                                      IC2224.2
048700     IF EXCEPTION-PATH-FLAG NOT = "P"                             IC2224.2
048800             MOVE "EXCEPTION SHOULD HAVE OCCURRED" TO RE-MARK     IC2224.2
048900             MOVE "P" TO CORRECT-A                                IC2224.2
049000             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
049100             PERFORM FAIL                                         IC2224.2
049200     ELSE                                                         IC2224.2
049300             PERFORM PASS.                                        IC2224.2
049400 CALL-WRITE-2-1.                                                  IC2224.2
049500     PERFORM PRINT-DETAIL.                                        IC2224.2
049600     ADD 1 TO REC-CT.                                             IC2224.2
049700*                                                                 IC2224.2
049800 CALL-INIT-2-2.                                                   IC2224.2
049900     GO TO    CALL-TEST-2-2.                                      IC2224.2
050000 CALL-DELETE-2-2.                                                 IC2224.2
050100     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
050200     PERFORM DE-LETE.                                             IC2224.2
050300     PERFORM PRINT-DETAIL.                                        IC2224.2
050400     ADD     1 TO REC-CT.                                         IC2224.2
050500     GO TO   CALL-EXIT-2.                                         IC2224.2
050600*                                                                 IC2224.2
050700 CALL-TEST-2-2.                                                   IC2224.2
050800****************************************************************  IC2224.2
050900*                                                              *  IC2224.2
051000*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
051100*    WAS EXECUTED.  IF THE PREVIOUS TEST FAILED, A PASS HERE   *  IC2224.2
051200*    INDICATES THAT THE SCOPE TERMINATOR HAS BEEN INTERPRETED  *  IC2224.2
051300*    CORRECTLY.  IF THE PREVIOUS TEST PASSED, A PASS HERE      *  IC2224.2
051400*    INDICATES THAT THE SCOPE TERMINATOR WAS NOT INTERPRETED   *  IC2224.2
051500*    AS "NOT ON EXCEPTION" OR "GO TO NEXT-SENTENCE".           *  IC2224.2
051600*                                                              *  IC2224.2
051700****************************************************************  IC2224.2
051800*                                                                 IC2224.2
051900     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
052000     IF      CALL-FLAG = 1                                        IC2224.2
052100             PERFORM PASS                                         IC2224.2
052200     ELSE                                                         IC2224.2
052300             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
052400             MOVE    1 TO CORRECT-N                               IC2224.2
052500             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
052600             PERFORM FAIL.                                        IC2224.2
052700     PERFORM PRINT-DETAIL.                                        IC2224.2
052800*                                                                 IC2224.2
052900 CALL-EXIT-2.                                                     IC2224.2
053000*                                                                 IC2224.2
053100*                                                                 IC2224.2
053200 CALL-INIT-3.                                                     IC2224.2
053300****************************************************************  IC2224.2
053400*                                                              *  IC2224.2
053500*    CALL A PROGRAM WHICH EXISTS, USING A CALL STATEMENT WITH  *  IC2224.2
053600*    BOTH AN "ON EXCEPTION" PHRASE AND A "NOT ON EXCEPTION"    *  IC2224.2
053700*    PHRASE.  EXECUTION SHOULD BE SUCCESSFUL, THE              *  IC2224.2
053800*    "ON EXCEPTION" PHRASE IGNORED, AND THE STATEMENTS IN THE  *  IC2224.2
053900*    "NOT ON EXCEPTION" PHRASE EXECUTED.  THE STATEMENT        *  IC2224.2
054000*    FOLLOWING THE SCOPE TERMINATOR SHOULD BE EXECUTED.        *  IC2224.2
054100*                                                              *  IC2224.2
054200****************************************************************  IC2224.2
054300*                                                                 IC2224.2
054400     MOVE    1 TO REC-CT.                                         IC2224.2
054500     MOVE   "CALL-TEST-3" TO PAR-NAME.                            IC2224.2
054600     MOVE   "AVAILABLE  ON NOT ON" TO FEATURE.                    IC2224.2
054700     MOVE    0 TO CALL-FLAG.                                      IC2224.2
054800     MOVE   "X" TO EXCEPTION-PATH-FLAG.                           IC2224.2
054900     MOVE   "X-28 5.2.4 (2)" TO ANSI-REFERENCE.                   IC2224.2
055000     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
055100     GO TO   CALL-TEST-3-1.                                       IC2224.2
055200 CALL-DELETE-3-1.                                                 IC2224.2
055300     PERFORM DE-LETE.                                             IC2224.2
055400     PERFORM PRINT-DETAIL.                                        IC2224.2
055500     ADD     1 TO REC-CT.                                         IC2224.2
055600*                                                                 IC2224.2
055700*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE TEST IS      *  IC2224.2
055800*    AUTOMATICALLY DELETED.                                    *  IC2224.2
055900*                                                                 IC2224.2
056000     GO TO   CALL-DELETE-3-2.                                     IC2224.2
056100 CALL-TEST-3-1.                                                   IC2224.2
056200     CALL   "IC222A-1" USING DN1, DN2, DN3, DN4                   IC2224.2
056300             ON EXCEPTION                                         IC2224.2
056400                  MOVE "F" TO EXCEPTION-PATH-FLAG                 IC2224.2
056500                  ADD 2 TO CALL-FLAG                              IC2224.2
056600             NOT ON EXCEPTION                                     IC2224.2
056700                  MOVE "P" TO EXCEPTION-PATH-FLAG                 IC2224.2
056800                  ADD 2 TO CALL-FLAG                              IC2224.2
056900     END-CALL                                                     IC2224.2
057000     ADD     1 TO CALL-FLAG.                                      IC2224.2
057100     IF EXCEPTION-PATH-FLAG NOT = "P"                             IC2224.2
057200             MOVE "NON-EXECUTION OF NOT EXCEPTION PATH"           IC2224.2
057300                   TO RE-MARK                                     IC2224.2
057400             MOVE "P" TO CORRECT-A                                IC2224.2
057500             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
057600             PERFORM FAIL                                         IC2224.2
057700     ELSE                                                         IC2224.2
057800             PERFORM PASS.                                        IC2224.2
057900 CALL-WRITE-3-1.                                                  IC2224.2
058000     PERFORM PRINT-DETAIL.                                        IC2224.2
058100     ADD 1 TO REC-CT.                                             IC2224.2
058200*                                                                 IC2224.2
058300 CALL-INIT-3-2.                                                   IC2224.2
058400     GO TO    CALL-TEST-3-2.                                      IC2224.2
058500 CALL-DELETE-3-2.                                                 IC2224.2
058600     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
058700     PERFORM DE-LETE.                                             IC2224.2
058800     PERFORM PRINT-DETAIL.                                        IC2224.2
058900     ADD     1 TO REC-CT.                                         IC2224.2
059000     GO TO   CALL-EXIT-3.                                         IC2224.2
059100*                                                                 IC2224.2
059200 CALL-TEST-3-2.                                                   IC2224.2
059300****************************************************************  IC2224.2
059400*                                                              *  IC2224.2
059500*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
059600*    WAS EXECUTED.  IF THE PREVIOUS TEST FAILED, A PASS HERE   *  IC2224.2
059700*    INDICATES THAT THE SCOPE TERMINATOR HAS BEEN INTERPRETED  *  IC2224.2
059800*    CORRECTLY.  IF THE PREVIOUS TEST PASSED, A PASS HERE      *  IC2224.2
059900*    INDICATES THAT THE SCOPE TERMINATOR WAS NOT INTERPRETED   *  IC2224.2
060000*    AS "NOT ON EXCEPTION" OR "GO TO NEXT-SENTENCE".           *  IC2224.2
060100*                                                              *  IC2224.2
060200****************************************************************  IC2224.2
060300*                                                                 IC2224.2
060400     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
060500     IF      CALL-FLAG = 3                                        IC2224.2
060600             PERFORM PASS                                         IC2224.2
060700     ELSE                                                         IC2224.2
060800             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
060900             MOVE    3 TO CORRECT-N                               IC2224.2
061000             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
061100             PERFORM FAIL.                                        IC2224.2
061200     PERFORM PRINT-DETAIL.                                        IC2224.2
061300*                                                                 IC2224.2
061400 CALL-EXIT-3.                                                     IC2224.2
061500*                                                                 IC2224.2
061600*                                                                 IC2224.2
061700 CALL-INIT-4.                                                     IC2224.2
061800****************************************************************  IC2224.2
061900*                                                              *  IC2224.2
062000*    CALL A PROGRAM WHICH IS NOT AVAILABLE FOR EXECUTION,      *  IC2224.2
062100*    USING A CALL STATEMENT WITH BOTH AN "ON EXCEPTION" PHRASE *  IC2224.2
062200*    AND A "NOT ON EXCEPTION" PHRASE.  EXECUTION SHOULD BE     *  IC2224.2
062300*    UNSUCCESSFUL, THE STATEMENTS IN THE "ON EXCEPTION" PHRASE *  IC2224.2
062400*    EXECUTED, AND THE STATEMENTS IN THE "NOT ON EXCEPTION"    *  IC2224.2
062500*    PHRASE IGNORED.  THE STATEMENT FOLLOWING THE SCOPE        *  IC2224.2
062600*    TERMINATOR SHOULD BE EXECUTED IN EITHER CASE.             *  IC2224.2
062700*                                                              *  IC2224.2
062800****************************************************************  IC2224.2
062900*                                                                 IC2224.2
063000     MOVE    1 TO REC-CT.                                         IC2224.2
063100     MOVE   "CALL-TEST-4" TO PAR-NAME.                            IC2224.2
063200     MOVE   "CALL ON EXCEPTION" TO FEATURE.                       IC2224.2
063300     MOVE    0 TO CALL-FLAG.                                      IC2224.2
063400     MOVE   "X" TO EXCEPTION-PATH-FLAG.                           IC2224.2
063500     MOVE   "X-28 5.2.4 (3)A" TO ANSI-REFERENCE.                  IC2224.2
063600     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
063700     GO TO   CALL-TEST-4-1.                                       IC2224.2
063800 CALL-DELETE-4-1.                                                 IC2224.2
063900     PERFORM DE-LETE.                                             IC2224.2
064000     PERFORM PRINT-DETAIL.                                        IC2224.2
064100     ADD     1 TO REC-CT.                                         IC2224.2
064200*                                                                 IC2224.2
064300*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE TEST IS      *  IC2224.2
064400*    AUTOMATICALLY DELETED.                                    *  IC2224.2
064500*                                                                 IC2224.2
064600     GO TO   CALL-DELETE-4-2.                                     IC2224.2
064700 CALL-TEST-4-1.                                                   IC2224.2
064800*    CALL   "NON-EXISTENT PROGRAM"                                IC2224.2
064900     CALL   "XXXXXXXX" USING DN1, DN2, DN3, DN4                   IC2224.2
065000             ON EXCEPTION                                         IC2224.2
065100                  MOVE "P" TO EXCEPTION-PATH-FLAG                 IC2224.2
065200                  ADD 2 TO CALL-FLAG                              IC2224.2
065300             NOT ON EXCEPTION                                     IC2224.2
065400                  MOVE "F" TO EXCEPTION-PATH-FLAG                 IC2224.2
065500                  ADD 2 TO CALL-FLAG                              IC2224.2
065600     END-CALL                                                     IC2224.2
065700     ADD     1 TO CALL-FLAG.                                      IC2224.2
065800     IF EXCEPTION-PATH-FLAG NOT = "P"                             IC2224.2
065900             MOVE "NON-EXECUTION OF EXCEPTION PATH"               IC2224.2
066000                   TO RE-MARK                                     IC2224.2
066100             MOVE "P" TO CORRECT-A                                IC2224.2
066200             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
066300             PERFORM FAIL                                         IC2224.2
066400     ELSE                                                         IC2224.2
066500             PERFORM PASS.                                        IC2224.2
066600 CALL-WRITE-4-1.                                                  IC2224.2
066700     PERFORM PRINT-DETAIL.                                        IC2224.2
066800     ADD 1 TO REC-CT.                                             IC2224.2
066900*                                                                 IC2224.2
067000 CALL-INIT-4-2.                                                   IC2224.2
067100     GO TO    CALL-TEST-4-2.                                      IC2224.2
067200 CALL-DELETE-4-2.                                                 IC2224.2
067300     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
067400     PERFORM DE-LETE.                                             IC2224.2
067500     PERFORM PRINT-DETAIL.                                        IC2224.2
067600     ADD     1 TO REC-CT.                                         IC2224.2
067700     GO TO   CALL-EXIT-4.                                         IC2224.2
067800*                                                                 IC2224.2
067900 CALL-TEST-4-2.                                                   IC2224.2
068000****************************************************************  IC2224.2
068100*                                                              *  IC2224.2
068200*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
068300*    WAS EXECUTED.  A PASS HERE ALSO INDICATES THAT ONE AND    *  IC2224.2
068400*    ONLY ONE OF THE "ON EXCEPTION" AND "NOT ON EXCEPTION"     *  IC2224.2
068500*    PHRASES OF THE PRECEDING CALL STATEMENT WAS EXECUTED.     *  IC2224.2
068600*                                                              *  IC2224.2
068700****************************************************************  IC2224.2
068800*                                                                 IC2224.2
068900     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
069000     IF      CALL-FLAG = 3                                        IC2224.2
069100             PERFORM PASS                                         IC2224.2
069200     ELSE                                                         IC2224.2
069300             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
069400             MOVE    3 TO CORRECT-N                               IC2224.2
069500             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
069600             PERFORM FAIL.                                        IC2224.2
069700     PERFORM PRINT-DETAIL.                                        IC2224.2
069800*                                                                 IC2224.2
069900 CALL-EXIT-4.                                                     IC2224.2
070000*                                                                 IC2224.2
070100*                                                                 IC2224.2
070200 CALL-INIT-5.                                                     IC2224.2
070300****************************************************************  IC2224.2
070400*                                                              *  IC2224.2
070500*    CALL A PROGRAM WHICH IS AVAILABLE FOR EXECUTION, USING A  *  IC2224.2
070600*    CALL STATEMENT WITH A "NOT ON EXCEPTION" PHRASE BUT NO    *  IC2224.2
070700*    "ON EXCEPTION" PHRASE.  EXECUTION SHOULD BE SUCCESSFUL,   *  IC2224.2
070800*    AND THE STATEMENTS IN THE "NOT ON EXCEPTION" PHRASE       *  IC2224.2
070900*    EXECUTED.  THE STATEMENT FOLLOWING THE SCOPE TERMINATOR   *  IC2224.2
071000*    SHOULD ALSO BE EXECUTED.                                  *  IC2224.2
071100*                                                              *  IC2224.2
071200****************************************************************  IC2224.2
071300*                                                                 IC2224.2
071400     MOVE    1 TO REC-CT.                                         IC2224.2
071500     MOVE   "CALL-TEST-5" TO PAR-NAME.                            IC2224.2
071600     MOVE   "AVAILABLE  -- NOT ON" TO FEATURE.                    IC2224.2
071700     MOVE    0 TO CALL-FLAG.                                      IC2224.2
071800     MOVE   "X" TO EXCEPTION-PATH-FLAG.                           IC2224.2
071900     MOVE   "X-28 5.2.4 (3)A" TO ANSI-REFERENCE.                  IC2224.2
072000     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
072100     GO TO   CALL-TEST-5-1.                                       IC2224.2
072200 CALL-DELETE-5-1.                                                 IC2224.2
072300     PERFORM DE-LETE.                                             IC2224.2
072400     PERFORM PRINT-DETAIL.                                        IC2224.2
072500     ADD     1 TO REC-CT.                                         IC2224.2
072600*                                                                 IC2224.2
072700*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE TEST IS      *  IC2224.2
072800*    AUTOMATICALLY DELETED.                                    *  IC2224.2
072900*                                                                 IC2224.2
073000     GO TO   CALL-DELETE-5-2.                                     IC2224.2
073100 CALL-TEST-5-1.                                                   IC2224.2
073200     CALL   "IC222A-1" USING DN1, DN2, DN3, DN4                   IC2224.2
073300             NOT ON EXCEPTION                                     IC2224.2
073400                  MOVE "P" TO EXCEPTION-PATH-FLAG                 IC2224.2
073500                  ADD 2 TO CALL-FLAG                              IC2224.2
073600     END-CALL                                                     IC2224.2
073700     ADD     1 TO CALL-FLAG.                                      IC2224.2
073800     IF EXCEPTION-PATH-FLAG NOT = "P"                             IC2224.2
073900             MOVE "NON-EXECUTION OF NOT ON EXCEPTION PATH"        IC2224.2
074000                   TO RE-MARK                                     IC2224.2
074100             MOVE "P" TO CORRECT-A                                IC2224.2
074200             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
074300             PERFORM FAIL                                         IC2224.2
074400     ELSE                                                         IC2224.2
074500             PERFORM PASS.                                        IC2224.2
074600 CALL-WRITE-5-1.                                                  IC2224.2
074700     PERFORM PRINT-DETAIL.                                        IC2224.2
074800     ADD 1 TO REC-CT.                                             IC2224.2
074900*                                                                 IC2224.2
075000 CALL-INIT-5-2.                                                   IC2224.2
075100     GO TO    CALL-TEST-5-2.                                      IC2224.2
075200 CALL-DELETE-5-2.                                                 IC2224.2
075300     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
075400     PERFORM DE-LETE.                                             IC2224.2
075500     PERFORM PRINT-DETAIL.                                        IC2224.2
075600     ADD     1 TO REC-CT.                                         IC2224.2
075700     GO TO   CALL-EXIT-5.                                         IC2224.2
075800*                                                                 IC2224.2
075900 CALL-TEST-5-2.                                                   IC2224.2
076000****************************************************************  IC2224.2
076100*                                                              *  IC2224.2
076200*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
076300*    WAS EXECUTED.  A PASS HERE ALSO INDICATES THAT THE        *  IC2224.2
076400*    "NOT ON EXCEPTION" PHRASE OF THE PRECEDING CALL STATEMENT *  IC2224.2
076500*    WAS EXECUTED.                                             *  IC2224.2
076600*                                                              *  IC2224.2
076700****************************************************************  IC2224.2
076800*                                                                 IC2224.2
076900     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
077000     IF      CALL-FLAG = 3                                        IC2224.2
077100             PERFORM PASS                                         IC2224.2
077200     ELSE                                                         IC2224.2
077300             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
077400             MOVE    3 TO CORRECT-N                               IC2224.2
077500             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
077600             PERFORM FAIL.                                        IC2224.2
077700     PERFORM PRINT-DETAIL.                                        IC2224.2
077800*                                                                 IC2224.2
077900 CALL-EXIT-5.                                                     IC2224.2
078000*                                                                 IC2224.2
078100*                                                                 IC2224.2
078200 CALL-INIT-6.                                                     IC2224.2
078300****************************************************************  IC2224.2
078400*                                                              *  IC2224.2
078500*    CALL A PROGRAM WHICH IS AVAILABLE FOR EXECUTION, USING A  *  IC2224.2
078600*    CALL STATEMENT WITH NEITHER AN "ON EXCEPTION" PHRASE NOR  *  IC2224.2
078700*    A "NOT ON EXCEPTION" PHRASE.  EXECUTION SHOULD BE         *  IC2224.2
078800*    SUCCESSFUL. THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
078900*    SHOULD BE EXECUTED.                                       *  IC2224.2
079000*                                                              *  IC2224.2
079100****************************************************************  IC2224.2
079200*                                                                 IC2224.2
079300     MOVE    1 TO REC-CT.                                         IC2224.2
079400     MOVE   "CALL-TEST-6" TO PAR-NAME.                            IC2224.2
079500     MOVE   "AVAILABLE  -- ---" TO FEATURE.                       IC2224.2
079600     MOVE    0 TO CALL-FLAG.                                      IC2224.2
079700     MOVE   "X" TO EXCEPTION-PATH-FLAG.                           IC2224.2
079800     MOVE   "X-28 5.2.4 (2)" TO ANSI-REFERENCE.                   IC2224.2
079900     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
080000     GO TO   CALL-TEST-6-1.                                       IC2224.2
080100 CALL-DELETE-6-1.                                                 IC2224.2
080200     PERFORM DE-LETE.                                             IC2224.2
080300     PERFORM PRINT-DETAIL.                                        IC2224.2
080400     ADD     1 TO REC-CT.                                         IC2224.2
080500*                                                                 IC2224.2
080600*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE TEST IS      *  IC2224.2
080700*    AUTOMATICALLY DELETED.                                    *  IC2224.2
080800*                                                                 IC2224.2
080900     GO TO   CALL-DELETE-6-2.                                     IC2224.2
081000 CALL-TEST-6-1.                                                   IC2224.2
081100     CALL   "IC222A-1" USING DN1, DN2, DN3, DN4                   IC2224.2
081200     END-CALL                                                     IC2224.2
081300     ADD     1 TO CALL-FLAG.                                      IC2224.2
081400     IF EXCEPTION-PATH-FLAG NOT = "X"                             IC2224.2
081500             MOVE "EXCEPTION-PATH-FLAG ALTERED" TO RE-MARK        IC2224.2
081600             MOVE "X" TO CORRECT-A                                IC2224.2
081700             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
081800             PERFORM FAIL                                         IC2224.2
081900     ELSE                                                         IC2224.2
082000             PERFORM PASS.                                        IC2224.2
082100 CALL-WRITE-6-1.                                                  IC2224.2
082200     PERFORM PRINT-DETAIL.                                        IC2224.2
082300     ADD 1 TO REC-CT.                                             IC2224.2
082400*                                                                 IC2224.2
082500 CALL-INIT-6-2.                                                   IC2224.2
082600     GO TO    CALL-TEST-6-2.                                      IC2224.2
082700 CALL-DELETE-6-2.                                                 IC2224.2
082800     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
082900     PERFORM DE-LETE.                                             IC2224.2
083000     PERFORM PRINT-DETAIL.                                        IC2224.2
083100     ADD     1 TO REC-CT.                                         IC2224.2
083200     GO TO   CALL-EXIT-6.                                         IC2224.2
083300*                                                                 IC2224.2
083400 CALL-TEST-6-2.                                                   IC2224.2
083500****************************************************************  IC2224.2
083600*                                                              *  IC2224.2
083700*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
083800*    WAS EXECUTED.                                             *  IC2224.2
083900*                                                              *  IC2224.2
084000****************************************************************  IC2224.2
084100*                                                                 IC2224.2
084200     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
084300     IF      CALL-FLAG = 1                                        IC2224.2
084400             PERFORM PASS                                         IC2224.2
084500     ELSE                                                         IC2224.2
084600             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
084700             MOVE    1 TO CORRECT-N                               IC2224.2
084800             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
084900             PERFORM FAIL.                                        IC2224.2
085000     PERFORM PRINT-DETAIL.                                        IC2224.2
085100*                                                                 IC2224.2
085200 CALL-EXIT-6.                                                     IC2224.2
085300*                                                                 IC2224.2
085400*                                                                 IC2224.2
085500 CALL-INIT-7.                                                     IC2224.2
085600****************************************************************  IC2224.2
085700*                                                              *  IC2224.2
085800*    CALL A PROGRAM WHICH EXISTS AND FOR WHICH PARAMETERS      *  IC2224.2
085900*    MATCH IN NUMBER AND TYPE.  THIS TEST IS A DUPLICATION OF  *  IC2224.2
086000*    CALL-TEST-1, WITH "ON OVERFLOW" SUBSTITUTED FOR           *  IC2224.2
086100*    "ON EXCEPTION" IN THE CALL STATEMENT.                     *  IC2224.2
086200*                                                              *  IC2224.2
086300****************************************************************  IC2224.2
086400*                                                                 IC2224.2
086500     MOVE    1 TO REC-CT.                                         IC2224.2
086600     MOVE   "CALL-TEST-7" TO PAR-NAME.                            IC2224.2
086700     MOVE   "AVAILABLE  OV ---" TO FEATURE.                       IC2224.2
086800     MOVE    0 TO CALL-FLAG.                                      IC2224.2
086900     MOVE   "P" TO EXCEPTION-PATH-FLAG.                           IC2224.2
087000     MOVE   "X-27 5.2.4 (2)" TO ANSI-REFERENCE.                   IC2224.2
087100     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
087200     GO TO   CALL-TEST-7-1.                                       IC2224.2
087300 CALL-DELETE-7-1.                                                 IC2224.2
087400     PERFORM DE-LETE.                                             IC2224.2
087500     PERFORM PRINT-DETAIL.                                        IC2224.2
087600     ADD     1 TO REC-CT.                                         IC2224.2
087700*                                                                 IC2224.2
087800*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE TEST IS      *  IC2224.2
087900*    AUTOMATICALLY DELETED.                                    *  IC2224.2
088000*                                                                 IC2224.2
088100     GO TO   CALL-DELETE-7-2.                                     IC2224.2
088200 CALL-TEST-7-1.                                                   IC2224.2
088300     CALL   "IC222A-1" USING DN1, DN2, DN3, DN4                   IC2224.2
088400             ON OVERFLOW                                          IC2224.2
088500                  MOVE "F" TO EXCEPTION-PATH-FLAG                 IC2224.2
088600     END-CALL                                                     IC2224.2
088700     MOVE    1 TO CALL-FLAG.                                      IC2224.2
088800     IF EXCEPTION-PATH-FLAG NOT = "P"                             IC2224.2
088900             MOVE "UNEXPECTED EXECUTION OF EXCEPTION PATH"        IC2224.2
089000                      TO RE-MARK                                  IC2224.2
089100             MOVE "P" TO CORRECT-A                                IC2224.2
089200             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
089300             PERFORM FAIL                                         IC2224.2
089400     ELSE                                                         IC2224.2
089500             PERFORM PASS.                                        IC2224.2
089600 CALL-WRITE-7-1.                                                  IC2224.2
089700     PERFORM PRINT-DETAIL.                                        IC2224.2
089800     ADD 1 TO REC-CT.                                             IC2224.2
089900*                                                                 IC2224.2
090000 CALL-INIT-7-2.                                                   IC2224.2
090100     GO TO    CALL-TEST-7-2.                                      IC2224.2
090200 CALL-DELETE-7-2.                                                 IC2224.2
090300     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
090400     PERFORM DE-LETE.                                             IC2224.2
090500     PERFORM PRINT-DETAIL.                                        IC2224.2
090600     ADD     1 TO REC-CT.                                         IC2224.2
090700     GO TO   CALL-EXIT-7.                                         IC2224.2
090800*                                                                 IC2224.2
090900 CALL-TEST-7-2.                                                   IC2224.2
091000****************************************************************  IC2224.2
091100*                                                              *  IC2224.2
091200*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
091300*    WAS EXECUTED.  IF THE PREVIOUS TEST PASSED, A PASS HERE   *  IC2224.2
091400*    INDICATES THAT THE SCOPE TERMINATOR HAS BEEN INTERPRETED  *  IC2224.2
091500*    CORRECTLY.  IF THE PREVIOUS TEST FAILED, A PASS HERE      *  IC2224.2
091600*    INDICATES THAT THE SCOPE TERMINATOR WAS NOT INTERPRETED   *  IC2224.2
091700*    AS "NOT ON EXCEPTION" OR "GO TO NEXT-SENTENCE".           *  IC2224.2
091800*                                                              *  IC2224.2
091900****************************************************************  IC2224.2
092000*                                                                 IC2224.2
092100     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
092200     IF      CALL-FLAG = 1                                        IC2224.2
092300             PERFORM PASS                                         IC2224.2
092400     ELSE                                                         IC2224.2
092500             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
092600             MOVE    1 TO CORRECT-N                               IC2224.2
092700             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
092800             PERFORM FAIL.                                        IC2224.2
092900     PERFORM PRINT-DETAIL.                                        IC2224.2
093000*                                                                 IC2224.2
093100 CALL-EXIT-7.                                                     IC2224.2
093200*                                                                 IC2224.2
093300*                                                                 IC2224.2
093400 CALL-INIT-8.                                                     IC2224.2
093500****************************************************************  IC2224.2
093600*                                                              *  IC2224.2
093700*    CALL A PROGRAM WHICH DOES NOT EXIST.  PAGE X-28, 5.2.4    *  IC2224.2
093800*    RULE (3)A STATES THAT IF A PROGRAM CANNOT BE MADE        *   IC2224.2
093900*    AVAILABLE THEN THE STATEMENTS IN THE "ON EXCEPTION" OR    *  IC2224.2
094000*    "ON OVERFLOW" PHRASE MUST BE EXECUTED.  THIS TEST IS A    *  IC2224.2
094100*    DUPLICATION OF CALL-TEST-2 WITH "ON OVERFLOW" SUBSTITUTED *  IC2224.2
094200*    FOR "ON EXCEPTION" IN THE CALL STATEMENT.                 *  IC2224.2
094300*                                                              *  IC2224.2
094400****************************************************************  IC2224.2
094500*                                                                 IC2224.2
094600     MOVE    1 TO REC-CT.                                         IC2224.2
094700     MOVE   "CALL-TEST-8" TO PAR-NAME.                            IC2224.2
094800     MOVE   "NO PROGRAM OV ---" TO FEATURE.                       IC2224.2
094900     MOVE    0 TO CALL-FLAG.                                      IC2224.2
095000     MOVE   "X" TO EXCEPTION-PATH-FLAG.                           IC2224.2
095100     MOVE   "X-28 5.2.4 (3)A" TO ANSI-REFERENCE.                  IC2224.2
095200     MOVE    ZERO TO DN3, DN4.                                    IC2224.2
095300     GO TO   CALL-TEST-8-1.                                       IC2224.2
095400 CALL-DELETE-8-1.                                                 IC2224.2
095500     PERFORM DE-LETE.                                             IC2224.2
095600     PERFORM PRINT-DETAIL.                                        IC2224.2
095700     ADD     1 TO REC-CT.                                         IC2224.2
095800*                                                                 IC2224.2
095900*    IF THIS TEST IS DELETED THEN ITS SUBORDINATE TEST IS      *  IC2224.2
096000*    AUTOMATICALLY DELETED.                                    *  IC2224.2
096100*                                                                 IC2224.2
096200     GO TO   CALL-DELETE-8-2.                                     IC2224.2
096300 CALL-TEST-8-1.                                                   IC2224.2
096400*    CALL "NON-EXISTING-PROGRAM"                                  IC2224.2
096500     CALL   "XXXXXXXX" USING DN1, DN2, DN3, DN4                   IC2224.2
096600             ON OVERFLOW                                          IC2224.2
096700                  MOVE "P" TO EXCEPTION-PATH-FLAG                 IC2224.2
096800     END-CALL                                                     IC2224.2
096900     MOVE    1 TO CALL-FLAG.                                      IC2224.2
097000     IF EXCEPTION-PATH-FLAG NOT = "P"                             IC2224.2
097100             MOVE "EXCEPTION SHOULD HAVE OCCURRED" TO RE-MARK     IC2224.2
097200             MOVE "P" TO CORRECT-A                                IC2224.2
097300             MOVE EXCEPTION-PATH-FLAG TO COMPUTED-A               IC2224.2
097400             PERFORM FAIL                                         IC2224.2
097500     ELSE                                                         IC2224.2
097600             PERFORM PASS.                                        IC2224.2
097700 CALL-WRITE-8-1.                                                  IC2224.2
097800     PERFORM PRINT-DETAIL.                                        IC2224.2
097900     ADD 1 TO REC-CT.                                             IC2224.2
098000*                                                                 IC2224.2
098100 CALL-INIT-8-2.                                                   IC2224.2
098200     GO TO    CALL-TEST-8-2.                                      IC2224.2
098300 CALL-DELETE-8-2.                                                 IC2224.2
098400     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
098500     PERFORM DE-LETE.                                             IC2224.2
098600     PERFORM PRINT-DETAIL.                                        IC2224.2
098700     ADD     1 TO REC-CT.                                         IC2224.2
098800     GO TO   CALL-EXIT-8.                                         IC2224.2
098900*                                                                 IC2224.2
099000 CALL-TEST-8-2.                                                   IC2224.2
099100****************************************************************  IC2224.2
099200*                                                              *  IC2224.2
099300*    CHECKS THAT THE STATEMENT FOLLOWING THE SCOPE TERMINATOR  *  IC2224.2
099400*    WAS EXECUTED.  IF THE PREVIOUS TEST FAILED, A PASS HERE   *  IC2224.2
099500*    INDICATES THAT THE SCOPE TERMINATOR HAS BEEN INTERPRETED  *  IC2224.2
099600*    CORRECTLY.  IF THE PREVIOUS TEST PASSED, A PASS HERE      *  IC2224.2
099700*    INDICATES THAT THE SCOPE TERMINATOR WAS NOT INTERPRETED   *  IC2224.2
099800*    AS "NOT ON EXCEPTION" OR "GO TO NEXT-SENTENCE".           *  IC2224.2
099900*                                                              *  IC2224.2
100000****************************************************************  IC2224.2
100100*                                                                 IC2224.2
100200     MOVE   "END-CALL OBSERVANCE" TO FEATURE.                     IC2224.2
100300     IF      CALL-FLAG = 1                                        IC2224.2
100400             PERFORM PASS                                         IC2224.2
100500     ELSE                                                         IC2224.2
100600             MOVE   "INCORRECT CONTROL FLOW" TO RE-MARK           IC2224.2
100700             MOVE    1 TO CORRECT-N                               IC2224.2
100800             MOVE    CALL-FLAG TO COMPUTED-N                      IC2224.2
100900             PERFORM FAIL.                                        IC2224.2
101000     PERFORM PRINT-DETAIL.                                        IC2224.2
101100*                                                                 IC2224.2
101200 CALL-EXIT-8.                                                     IC2224.2
101300*                                                                 IC2224.2
101400*                                                                 IC2224.2
101500 CCVS-EXIT SECTION.                                               IC2224.2
101600 CCVS-999999.                                                     IC2224.2
101700     GO TO CLOSE-FILES.                                           IC2224.2
101800 END PROGRAM IC222A.                                              IC2224.2
101900 IDENTIFICATION DIVISION.                                         IC2224.2
102000 PROGRAM-ID.                                                      IC2224.2
102100     IC222A-1.                                                    IC2224.2
102200****************************************************************  IC2224.2
102300*                                                              *  IC2224.2
102400*    THIS PROGRAM FORMS PART OF THE COBOL COMPILER VALIDATION  *  IC2224.2
102500*    SYSTEM (CCVS) USED TO TEST COBOL COMPILERS FOR            *  IC2224.2
102600*    CONFORMANCE WITH THE AMERICAN NATIONAL STANDARD           *  IC2224.2
102700*    (ANSI DOCUMENT REFERENCE: X3.23-1985) AND THE STANDARD OF *  IC2224.2
102800*    THE INTERNATIONAL ORGANIZATION FOR STANDARDISATION        *  IC2224.2
102900*    (ISO DOCUMENT REFERENCE: ISO-1989-1985).                  *  IC2224.2
103000*                                                              *  IC2224.2
103100*    THIS CCVS INCORPORATES ENHANCEMENTS TO THE CCVS FOR THE   *  IC2224.2
103200*    1974 STANDARD (ANSI DOCUMENT REFERENCE: X3.23-1974; ISO   *  IC2224.2
103300*    DOCUMENT REFERENCE: ISO-1989-1978).                       *  IC2224.2
103400*                                                              *  IC2224.2
103500*    THESE ENHANCEMENTS WERE SPECIFIED BY A PROJECT TEAM WHICH *  IC2224.2
103600*    WAS FUNDED BY THE COMMISSION FOR EUROPEAN COMMUNITIES AND *  IC2224.2
103700*    WHICH WAS RESPONSIBLE FOR TECHNICAL ISSUES TO:            *  IC2224.2
103800*                                                              *  IC2224.2
103900*          THE FEDERAL SOFTWARE TESTING CENTER                 *  IC2224.2
104000*          OFFICE OF SOFTWARE DEVELOPMENT                      *  IC2224.2
104100*                & INFORMATION TECHNOLOGY                      *  IC2224.2
104200*          TWO SKYLINE PLACE                                   *  IC2224.2
104300*          SUITE 1100                                          *  IC2224.2
104400*          5203 LEESBURG PIKE                                  *  IC2224.2
104500*          FALLS CHURCH                                        *  IC2224.2
104600*          VA 22041                                            *  IC2224.2
104700*          U.S.A.                                              *  IC2224.2
104800*                                                              *  IC2224.2
104900*    THE PROJECT TEAM MEMBERS WERE:                            *  IC2224.2
105000*                                                              *  IC2224.2
105100*          BIADI (BUREAU INTER ADMINISTRATION                  *  IC2224.2
105200*                 DE DOCUMENTATION INFORMATIQUE)               *  IC2224.2
105300*          21 RUE BARA                                         *  IC2224.2
105400*          F-92132 ISSY                                        *  IC2224.2
105500*          FRANCE                                              *  IC2224.2
105600*                                                              *  IC2224.2
105700*                                                              *  IC2224.2
105800*          GMD (GESELLSCHAFT FUR MATHEMATIK                    *  IC2224.2
105900*               UND DATENVERARBEITUNG MBH)                     *  IC2224.2
106000*          SCHLOSS BIRLINGHOVEN                                *  IC2224.2
106100*          POSTFACH 12 40                                      *  IC2224.2
106200*          D-5205 ST. AUGUSTIN 1                               *  IC2224.2
106300*          GERMANY FR                                          *  IC2224.2
106400*                                                              *  IC2224.2
106500*                                                              *  IC2224.2
106600*          NCC (THE NATIONAL COMPUTING CENTRE LTD)             *  IC2224.2
106700*          OXFORD ROAD                                         *  IC2224.2
106800*          MANCHESTER                                          *  IC2224.2
106900*          M1 7ED                                              *  IC2224.2
107000*          UNITED KINGDOM                                      *  IC2224.2
107100*                                                              *  IC2224.2
107200*                                                              *  IC2224.2
107300*    THIS TEST SUITE WAS PRODUCED BY THE NATIONAL COMPUTING    *  IC2224.2
107400*    CENTRE IN ENGLAND AND IS THE OFFICIAL CCVS TEST SUITE     *  IC2224.2
107500*    USED THROUGHOUT EUROPE AND THE UNITED STATES OF AMERICA.  *  IC2224.2
107600*                                                              *  IC2224.2
107700*    REVISED 1986 AUGUST                                       *  IC2224.2
107800*                                                              *  IC2224.2
107900****************************************************************  IC2224.2
108000*                                                              *  IC2224.2
108100*    VALIDATION FOR:-                                          *  IC2224.2
108200*                                                              *  IC2224.2
108300*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2224.2
108400*                                                              *  IC2224.2
108500*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2224.2
108600*                                                              *  IC2224.2
108700****************************************************************  IC2224.2
108800*                                                              *  IC2224.2
108900*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2224.2
109000*                                                              *  IC2224.2
109100*            X-82   SOURCE-COMPUTER                            *  IC2224.2
109200*            X-83   OBJECT-COMPUTER.                           *  IC2224.2
109300*                                                              *  IC2224.2
109400****************************************************************  IC2224.2
109500*                                                              *  IC2224.2
109600*    THE SOURCE FILE CONTAINS TWO PROGRAMS, IC222A AND         *  IC2224.2
109700*    IC222A-1, WHICH TEST LANGUAGE ELEMENTS FROM  LEVEL 2 OF   *  IC2224.2
109800*    THE INTER-PROGRAM COMMUNICATION MODULE.  THE LANGUAGE     *  IC2224.2
109900*    ELEMENTS TESTED ARE:                                      *  IC2224.2
110000*          "ON EXCEPTION"     PHRASE                           *  IC2224.2
110100*          "NOT ON EXCEPTION" PHRASE                           *  IC2224.2
110200*          "END-CALL"         PHRASE                           *  IC2224.2
110300*          "ON OVERFLOW"      PHRASE                           *  IC2224.2
110400*                                                                 IC2224.2
110500*    THE TWO PROGRAMS SHOULD BE COMPILED IN THE SAME           *  IC2224.2
110600*    INVOCATION OF THE COMPILER TO TEST THE BATCH COMPILATION  *  IC2224.2
110700*    FEATURE AND RECOGNITION OF THE END PROGRAM HEADER.  THE   *  IC2224.2
110800*    ARRANGEMENT OF THE PROGRAMS IN THE SOURCE FILE IS:           IC2224.2
110900*                                                                 IC2224.2
111000*    IDENTIFICATION DIVISION.                                     IC2224.2
111100*    PROGRAM-ID. IC222A.                                          IC2224.2
111200*          .                                                      IC2224.2
111300*          .                                                      IC2224.2
111400*          .                                                      IC2224.2
111500*    END PROGRAM IC222A.                                          IC2224.2
111600*    IDENTIFICATION DIVISION.                                     IC2224.2
111700*    PROGRAM-ID. IC222A-1.                                        IC2224.2
111800*          .                                                      IC2224.2
111900*          .                                                      IC2224.2
112000*          .                                                      IC2224.2
112100*                                                                 IC2224.2
112200*    A FULL DESCRIPTION OF THE TWO PROGRAMS IS INCLUDED AS     *  IC2224.2
112300*    COMMENTS IN PROGRAM IC222A.                               *  IC2224.2
112400*                                                              *  IC2224.2
112500****************************************************************  IC2224.2
112600*                                                                 IC2224.2
112700 ENVIRONMENT DIVISION.                                            IC2224.2
112800 CONFIGURATION SECTION.                                           IC2224.2
112900 SOURCE-COMPUTER.                                                 IC2224.2
113000     XXXXX082.                                                    IC2224.2
113100 OBJECT-COMPUTER.                                                 IC2224.2
113200     XXXXX083.                                                    IC2224.2
113300 INPUT-OUTPUT SECTION.                                            IC2224.2
113400 FILE-CONTROL.                                                    IC2224.2
113500     SELECT PRINT-FILE ASSIGN TO                                  IC2224.2
113600     XXXXX055.                                                    IC2224.2
113700*                                                                 IC2224.2
113800 DATA DIVISION.                                                   IC2224.2
113900 FILE SECTION.                                                    IC2224.2
114000 FD  PRINT-FILE.                                                  IC2224.2
114100 01  PRINT-REC PICTURE X(120).                                    IC2224.2
114200 01  DUMMY-RECORD PICTURE X(120).                                 IC2224.2
114300 WORKING-STORAGE SECTION.                                         IC2224.2
114400 77  WS1 PICTURE S999.                                            IC2224.2
114500 77  WS2 PICTURE S999                                             IC2224.2
114600         USAGE COMPUTATIONAL, VALUE ZERO.                         IC2224.2
114700 LINKAGE SECTION.                                                 IC2224.2
114800 77  DN1 PICTURE S99.                                             IC2224.2
114900 77  DN2 PICTURE S99 USAGE COMPUTATIONAL.                         IC2224.2
115000 77  DN3 PICTURE S99.                                             IC2224.2
115100 77  DN4 PICTURE S99 USAGE COMPUTATIONAL.                         IC2224.2
115200*                                                                 IC2224.2
115300 PROCEDURE DIVISION USING DN1, DN2, DN3, DN4.                     IC2224.2
115400 SECT-IC222A-1-001 SECTION.                                       IC2224.2
115500 CALL-TEST-001.                                                   IC2224.2
115600     MOVE DN1 TO WS1.                                             IC2224.2
115700     ADD 1 TO WS1.                                                IC2224.2
115800     ADD 1 TO WS2.                                                IC2224.2
115900     MOVE WS1 TO DN3.                                             IC2224.2
116000     MOVE WS2 TO DN4.                                             IC2224.2
116100 CALL-EXIT-001.                                                   IC2224.2
116200     EXIT PROGRAM.                                                IC2224.2
      *END-OF,IC222A                                                                  
