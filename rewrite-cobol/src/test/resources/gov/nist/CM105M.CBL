      *HEADER,COBOL,CM105M                                                            
000100 IDENTIFICATION DIVISION.                                         CM1054.2
000200 PROGRAM-ID.                                                      CM1054.2
000300     CM105M.                                                      CM1054.2
000400 AUTHOR.                                                          CM1054.2
000500     FEDERAL COMPILER TESTING CENTER.                             CM1054.2
000600* INSTALLATION.                                                    CM1054.2
000700*     GENERAL SERVICES ADMINISTRATION                              CM1054.2
000800**     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                CM1054.2
000900*    SOFTWARE DEVELOPMENT OFFICE.                                 CM1054.2
001000*     5203 LEESBURG PIKE  SUITE 1100                               CM1054.2
001100*     FALLS CHURCH VIRGINIA 22041.                                 CM1054.2
001200*                                                                  CM1054.2
001300*     PHONE   (703) 756-6153                                       CM1054.2
001400*                                                                  CM1054.2
001500*     " HIGH       ".                                              CM1054.2
001600 DATE-WRITTEN.                                                    CM1054.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           CM1054.2
001800     CREATION DATE     /    VALIDATION DATE                       CM1054.2
001900     "4.2 ".                                                      CM1054.2
002000 SECURITY.                                                        CM1054.2
002100     NONE.                                                        CM1054.2
002200 ENVIRONMENT DIVISION.                                            CM1054.2
002300 CONFIGURATION SECTION.                                           CM1054.2
002400 SOURCE-COMPUTER.                                                 CM1054.2
002500     XXXXX082.                                                    CM1054.2
002600 OBJECT-COMPUTER.                                                 CM1054.2
002700     XXXXX083.                                                    CM1054.2
002800 INPUT-OUTPUT SECTION.                                            CM1054.2
002900 FILE-CONTROL.                                                    CM1054.2
003000     SELECT PRINT-FILE ASSIGN TO                                  CM1054.2
003100     XXXXX055.                                                    CM1054.2
003200 DATA DIVISION.                                                   CM1054.2
003300 FILE SECTION.                                                    CM1054.2
003400 FD  PRINT-FILE                                                   CM1054.2
003500     LABEL RECORDS                                                CM1054.2
003600     XXXXX084                                                     CM1054.2
003700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       CM1054.2
003800 01  PRINT-REC PICTURE X(120).                                    CM1054.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 CM1054.2
004000 WORKING-STORAGE SECTION.                                         CM1054.2
004100 77  P    PIC X(12).                                              CM1054.2
004200 77  PP   PIC X(24).                                              CM1054.2
004300 77  PPP  PIC X(36).                                              CM1054.2
004400 77  PS   PIC X(24).                                              CM1054.2
004500 77  PSP  PIC X(36).                                              CM1054.2
004600 77  PPS  PIC X(36).                                              CM1054.2
004700 01  QUEUE-NAMES.                                                 CM1054.2
004800     02  PPPP PIC X(48) VALUE                                     CM1054.2
004900     XXXXX038.                                                    CM1054.2
005000     02  PPPS PIC X(48) VALUE                                     CM1054.2
005100     XXXXX039.                                                    CM1054.2
005200     02  PPSP PIC X(48) VALUE                                     CM1054.2
005300     XXXXX040.                                                    CM1054.2
005400     02  PSPP PIC X(48) VALUE                                     CM1054.2
005500     XXXXX041.                                                    CM1054.2
005600 01  QUEUE-NAMES-TABLE REDEFINES QUEUE-NAMES.                     CM1054.2
005700     02  NAME-SET PIC X(48) OCCURS 4 TIMES INDEXED BY I1.         CM1054.2
005800 01  TEST-RESULTS.                                                CM1054.2
005900     02 FILLER                    PICTURE X VALUE SPACE.          CM1054.2
006000     02 FEATURE                   PICTURE X(18).                  CM1054.2
006100     02 FILLER                    PICTURE X VALUE SPACE.          CM1054.2
006200     02 P-OR-F                    PICTURE X(5).                   CM1054.2
006300     02 FILLER                    PICTURE X  VALUE SPACE.         CM1054.2
006400     02  PAR-NAME PIC X(20).                                      CM1054.2
006500     02 FILLER                    PICTURE X VALUE SPACE.          CM1054.2
006600     02  COMPUTED-A.                                              CM1054.2
006700         03  FILLER PIC X(9) VALUE SPACE.                         CM1054.2
006800         03  STAT PIC XX.                                         CM1054.2
006900         03  FILLER PIC X(9) VALUE SPACE.                         CM1054.2
007000     02 FILLER                    PICTURE X VALUE SPACE.          CM1054.2
007100     02  CORRECT-A.                                               CM1054.2
007200         03  FILLER PIC X(8).                                     CM1054.2
007300         03  CORRECT-QUEUE PIC X(4).                              CM1054.2
007400         03  FILLER PIC X(8).                                     CM1054.2
007500     02 FILLER                    PICTURE X VALUE SPACE.          CM1054.2
007600     02  RE-MARK.                                                 CM1054.2
007700         03  QUEUE-KEY PIC X(4).                                  CM1054.2
007800         03  FILLER PIC X(26).                                    CM1054.2
007900 01  COLUMNS-LINE-1.                                              CM1054.2
008000     02  FILLER PIC X(3) VALUE SPACES.                            CM1054.2
008100     02  FILLER PIC X(17) VALUE "FEATURE TESTED".                 CM1054.2
008200     02  FILLER PIC X(9) VALUE "RESLT".                           CM1054.2
008300     02  FILLER PIC X(21) VALUE "PARAGRAPH NAME".                 CM1054.2
008400     02  FILLER PIC X(22) VALUE "COMPUTED DATA".                  CM1054.2
008500     02  FILLER PIC X(29) VALUE "CORRECT DATA".                   CM1054.2
008600     02  FILLER PIC X(7) VALUE "REMARKS".                         CM1054.2
008700 01  COLUMNS-LINE-2.                                              CM1054.2
008800     02  FILLER PIC X VALUE SPACE.                                CM1054.2
008900     02  FILLER PIC X(18) VALUE ALL "-".                          CM1054.2
009000     02  FILLER PIC X VALUE SPACE.                                CM1054.2
009100     02  FILLER PIC X(5) VALUE ALL "-".                           CM1054.2
009200     02  FILLER PIC X VALUE SPACE.                                CM1054.2
009300     02  FILLER PIC X(20) VALUE ALL "-".                          CM1054.2
009400     02  FILLER PIC X VALUE SPACE.                                CM1054.2
009500     02  FILLER PIC X(20) VALUE ALL "-".                          CM1054.2
009600     02  FILLER PIC X VALUE SPACE.                                CM1054.2
009700     02  FILLER PIC X(20) VALUE ALL "-".                          CM1054.2
009800     02  FILLER PIC X VALUE SPACE.                                CM1054.2
009900     02  FILLER PIC X(31) VALUE ALL "-".                          CM1054.2
010000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         CM1054.2
010100 01  REC-CT PICTURE 99 VALUE ZERO.                                CM1054.2
010200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        CM1054.2
010300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  CM1054.2
010400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          CM1054.2
010500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             CM1054.2
010600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              CM1054.2
010700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               CM1054.2
010800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           CM1054.2
010900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            CM1054.2
011000 01  CCVS-H-1.                                                    CM1054.2
011100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     CM1054.2
011200     02 FILLER PICTURE X(67) VALUE                                CM1054.2
011300     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  CM1054.2
011400-    " SYSTEM".                                                   CM1054.2
011500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   CM1054.2
011600 01  CCVS-H-2.                                                    CM1054.2
011700     02 FILLER PICTURE X(52) VALUE IS                             CM1054.2
011800     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   CM1054.2
011900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   CM1054.2
012000     02 TEST-ID PICTURE IS X(9).                                  CM1054.2
012100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   CM1054.2
012200 01  CCVS-H-3.                                                    CM1054.2
012300     02  FILLER PICTURE X(34) VALUE                               CM1054.2
012400     " FOR OFFICIAL USE ONLY    ".                                CM1054.2
012500     02  FILLER PICTURE X(58) VALUE                               CM1054.2
012600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".CM1054.2
012700     02  FILLER PICTURE X(28) VALUE                               CM1054.2
012800     "  COPYRIGHT   1974 ".                                       CM1054.2
012900 01  CCVS-E-1.                                                    CM1054.2
013000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   CM1054.2
013100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        CM1054.2
013200     02 ID-AGAIN PICTURE IS X(9).                                 CM1054.2
013300     02 FILLER PICTURE X(45) VALUE IS                             CM1054.2
013400     " NTIS DISTRIBUTION COBOL 74".                               CM1054.2
013500 01  CCVS-E-2.                                                    CM1054.2
013600     02  FILLER                   PICTURE X(31)  VALUE            CM1054.2
013700     SPACE.                                                       CM1054.2
013800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     CM1054.2
013900     02 CCVS-E-2-2.                                               CM1054.2
014000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            CM1054.2
014100         03 FILLER PICTURE IS X VALUE IS SPACE.                   CM1054.2
014200         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      CM1054.2
014300 01  CCVS-E-3.                                                    CM1054.2
014400     02  FILLER PICTURE X(22) VALUE                               CM1054.2
014500     " FOR OFFICIAL USE ONLY".                                    CM1054.2
014600     02  FILLER PICTURE X(12) VALUE SPACE.                        CM1054.2
014700     02  FILLER PICTURE X(58) VALUE                               CM1054.2
014800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".CM1054.2
014900     02  FILLER PICTURE X(13) VALUE SPACE.                        CM1054.2
015000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 CM1054.2
015100 01  CCVS-E-4.                                                    CM1054.2
015200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           CM1054.2
015300     02 FILLER PIC XXXX VALUE " OF ".                             CM1054.2
015400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           CM1054.2
015500     02 FILLER PIC X(40) VALUE                                    CM1054.2
015600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       CM1054.2
015700 01  XXINFO.                                                      CM1054.2
015800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    CM1054.2
015900     02 INFO-TEXT.                                                CM1054.2
016000     04 FILLER PIC X(20) VALUE SPACE.                             CM1054.2
016100     04 XXCOMPUTED PIC X(20).                                     CM1054.2
016200     04 FILLER PIC X(5) VALUE SPACE.                              CM1054.2
016300     04 XXCORRECT PIC X(20).                                      CM1054.2
016400 01  HYPHEN-LINE.                                                 CM1054.2
016500     02 FILLER PICTURE IS X VALUE IS SPACE.                       CM1054.2
016600     02 FILLER PICTURE IS X(65) VALUE IS "************************CM1054.2
016700-    "*****************************************".                 CM1054.2
016800     02 FILLER PICTURE IS X(54) VALUE IS "************************CM1054.2
016900-    "******************************".                            CM1054.2
017000 01  CCVS-PGM-ID PIC X(6) VALUE                                   CM1054.2
017100     "CM105M".                                                    CM1054.2
017200 01  MAIN-QUEUE-NAME.                                             CM1054.2
017300     02 MAIN-QUEUE   PIC X(12).                                   CM1054.2
017400     02 FILLER       PIC X(36) VALUE SPACE.                       CM1054.2
017500 COMMUNICATION SECTION.                                           CM1054.2
017600 CD  CM-INQUE-1 INPUT STATUS KEY IS IN-STAT SUB-QUEUE-3           CM1054.2
017700     IS-OF-NO-INTEREST COUNT NAMED-BELOW SOURCE NOT-USED.         CM1054.2
017800 01  INQUE-RECORD.                                                CM1054.2
017900     02  QUEUE-SET PIC X(48).                                     CM1054.2
018000     02  FILLER PIC X(33).                                        CM1054.2
018100     02  MSG-COUNT-N PIC 9(6).                                    CM1054.2
018200     02  MSG-CNT REDEFINES MSG-COUNT-N.                           CM1054.2
018300         03  FILLER PIC X(4).                                     CM1054.2
018400         03  MSG-COUNT PIC XX.                                    CM1054.2
018500 PROCEDURE    DIVISION.                                           CM1054.2
018600 SECT-CM105M-0001 SECTION.                                        CM1054.2
018700 CM105M-INIT.                                                     CM1054.2
018800     OPEN     OUTPUT PRINT-FILE.                                  CM1054.2
018900     MOVE "CM105M     " TO TEST-ID.                               CM1054.2
019000     MOVE     TEST-ID TO ID-AGAIN.                                CM1054.2
019100     MOVE    SPACE TO TEST-RESULTS.                               CM1054.2
019200     PERFORM HEAD-ROUTINE.                                        CM1054.2
019300     PERFORM COLUMN-NAMES-ROUTINE.                                CM1054.2
019400     MOVE                                                         CM1054.2
019500     XXXXX030                                                     CM1054.2
019600     TO MAIN-QUEUE.                                               CM1054.2
019700     MOVE MAIN-QUEUE-NAME TO QUEUE-SET.                           CM1054.2
019800     ENABLE INPUT CM-INQUE-1 KEY                                  CM1054.2
019900     XXXXX031.                                                    CM1054.2
020000     PERFORM BUILD-UP-QUEUES VARYING I1 FROM 1 BY 1               CM1054.2
020100         UNTIL I1 IS GREATER THAN 4.                              CM1054.2
020200     GO TO BEGIN-TESTS.                                           CM1054.2
020300 BUILD-UP-QUEUES.                                                 CM1054.2
020400     MOVE NAME-SET (I1) TO QUEUE-SET.                             CM1054.2
020500     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
020600     IF MSG-COUNT IS LESS THAN 10 GO TO BUILD-UP-QUEUES.          CM1054.2
020700 BEGIN-TESTS.                                                     CM1054.2
020800     DISABLE INPUT CM-INQUE-1 KEY                                 CM1054.2
020900     XXXXX031.                                                    CM1054.2
021000     MOVE PPPP TO  P  PP  PPP.                                    CM1054.2
021100     MOVE PPSP TO  PPS.                                           CM1054.2
021200     MOVE PSPP TO  PS  PSP.                                       CM1054.2
021300 QUEUE-TEST-01.                                                   CM1054.2
021400     MOVE "QUEUE SERIES PPPP" TO FEATURE.                         CM1054.2
021500     MOVE PPPP TO QUEUE-SET.                                      CM1054.2
021600     PERFORM RECEIVE-A-MSG.                                       CM1054.2
021700     IF QUEUE-KEY IS EQUAL TO "PPPP"                              CM1054.2
021800         PERFORM PASS GO TO QUEUE-TEST-WRITE-01.                  CM1054.2
021900     MOVE "PPPP" TO CORRECT-QUEUE.                                CM1054.2
022000     PERFORM FAIL.                                                CM1054.2
022100 QUEUE-TEST-WRITE-01.                                             CM1054.2
022200     MOVE "QUEUE-TEST-01" TO PAR-NAME.                            CM1054.2
022300     PERFORM PRINT-DETAIL.                                        CM1054.2
022400 QUEUE-TEST-02.                                                   CM1054.2
022500     MOVE "QUEUE SERIES PPPS" TO FEATURE.                         CM1054.2
022600     MOVE PPPS TO QUEUE-SET.                                      CM1054.2
022700     PERFORM RECEIVE-A-MSG.                                       CM1054.2
022800     IF QUEUE-KEY IS EQUAL TO "PPPS"                              CM1054.2
022900         PERFORM PASS   GO TO QUEUE-TEST-WRITE-02.                CM1054.2
023000     MOVE "PPPS" TO CORRECT-QUEUE.                                CM1054.2
023100     PERFORM FAIL.                                                CM1054.2
023200 QUEUE-TEST-WRITE-02.                                             CM1054.2
023300     MOVE "QUEUE-TEST-02" TO PAR-NAME.                            CM1054.2
023400     PERFORM PRINT-DETAIL.                                        CM1054.2
023500 QUEUE-TEST-03.                                                   CM1054.2
023600     MOVE "QUEUE SERIES PPSP" TO FEATURE.                         CM1054.2
023700     MOVE PPSP TO QUEUE-SET.                                      CM1054.2
023800     PERFORM RECEIVE-A-MSG.                                       CM1054.2
023900     IF QUEUE-KEY IS EQUAL TO "PPSP"                              CM1054.2
024000         PERFORM PASS   GO TO QUEUE-TEST-WRITE-03.                CM1054.2
024100     MOVE "PPSP" TO CORRECT-QUEUE.                                CM1054.2
024200     PERFORM FAIL.                                                CM1054.2
024300 QUEUE-TEST-WRITE-03.                                             CM1054.2
024400     MOVE "QUEUE-TEST-03" TO PAR-NAME.                            CM1054.2
024500     PERFORM PRINT-DETAIL.                                        CM1054.2
024600 QUEUE-TEST-04.                                                   CM1054.2
024700     MOVE "QUEUE SERIES PSPP" TO FEATURE.                         CM1054.2
024800     MOVE PSPP TO QUEUE-SET.                                      CM1054.2
024900     PERFORM RECEIVE-A-MSG.                                       CM1054.2
025000     IF QUEUE-KEY IS EQUAL TO "PSPP"                              CM1054.2
025100         PERFORM PASS   GO TO QUEUE-TEST-WRITE-04.                CM1054.2
025200     MOVE "PSPP" TO CORRECT-QUEUE.                                CM1054.2
025300     PERFORM FAIL.                                                CM1054.2
025400 QUEUE-TEST-WRITE-04.                                             CM1054.2
025500     MOVE "QUEUE-TEST-04" TO PAR-NAME.                            CM1054.2
025600     PERFORM PRINT-DETAIL.                                        CM1054.2
025700 QUEUE-TEST-05.                                                   CM1054.2
025800     MOVE "QUEUE SERIES P" TO FEATURE.                            CM1054.2
025900     MOVE P TO QUEUE-SET.                                         CM1054.2
026000     PERFORM RECEIVE-A-MSG.                                       CM1054.2
026100     IF QUEUE-KEY IS EQUAL TO "PPPP"                              CM1054.2
026200         PERFORM PASS   GO TO QUEUE-TEST-WRITE-05.                CM1054.2
026300     MOVE "PPPP" TO CORRECT-QUEUE.                                CM1054.2
026400     PERFORM FAIL.                                                CM1054.2
026500 QUEUE-TEST-WRITE-05.                                             CM1054.2
026600     MOVE "QUEUE-TEST-05" TO PAR-NAME.                            CM1054.2
026700     PERFORM PRINT-DETAIL.                                        CM1054.2
026800 QUEUE-TEST-06.                                                   CM1054.2
026900     MOVE "QUEUE SERIES PP" TO FEATURE.                           CM1054.2
027000     MOVE PP TO QUEUE-SET.                                        CM1054.2
027100     PERFORM RECEIVE-A-MSG.                                       CM1054.2
027200     IF QUEUE-KEY IS EQUAL TO "PPPP"                              CM1054.2
027300         PERFORM PASS   GO TO QUEUE-TEST-WRITE-06.                CM1054.2
027400     MOVE "PPPP" TO CORRECT-QUEUE.                                CM1054.2
027500     PERFORM FAIL.                                                CM1054.2
027600 QUEUE-TEST-WRITE-06.                                             CM1054.2
027700     MOVE "QUEUE-TEST-06" TO PAR-NAME.                            CM1054.2
027800     PERFORM PRINT-DETAIL.                                        CM1054.2
027900 QUEUE-TEST-07.                                                   CM1054.2
028000     MOVE "QUEUE SERIES PPP" TO FEATURE.                          CM1054.2
028100     MOVE PPP TO QUEUE-SET.                                       CM1054.2
028200     PERFORM RECEIVE-A-MSG.                                       CM1054.2
028300     IF QUEUE-KEY IS EQUAL TO "PPPP"                              CM1054.2
028400         PERFORM PASS   GO TO QUEUE-TEST-WRITE-07.                CM1054.2
028500     MOVE "PPPP" TO CORRECT-QUEUE.                                CM1054.2
028600     PERFORM FAIL.                                                CM1054.2
028700 QUEUE-TEST-WRITE-07.                                             CM1054.2
028800     MOVE "QUEUE-TEST-07" TO PAR-NAME.                            CM1054.2
028900     PERFORM PRINT-DETAIL.                                        CM1054.2
029000 QUEUE-TEST-08.                                                   CM1054.2
029100     MOVE "QUEUE SERIES PS" TO FEATURE.                           CM1054.2
029200     MOVE PS TO QUEUE-SET.                                        CM1054.2
029300     PERFORM RECEIVE-A-MSG.                                       CM1054.2
029400     IF QUEUE-KEY IS EQUAL TO "PSPP"                              CM1054.2
029500         PERFORM PASS   GO TO QUEUE-TEST-WRITE-08.                CM1054.2
029600     MOVE "PSPP" TO CORRECT-QUEUE.                                CM1054.2
029700     PERFORM FAIL.                                                CM1054.2
029800 QUEUE-TEST-WRITE-08.                                             CM1054.2
029900     MOVE "QUEUE-TEST-08" TO PAR-NAME.                            CM1054.2
030000     PERFORM PRINT-DETAIL.                                        CM1054.2
030100 QUEUE-TEST-09.                                                   CM1054.2
030200     MOVE "QUEUE SERIES PSP" TO FEATURE.                          CM1054.2
030300     MOVE PSP TO QUEUE-SET.                                       CM1054.2
030400     PERFORM RECEIVE-A-MSG.                                       CM1054.2
030500     IF QUEUE-KEY IS EQUAL TO "PSPP"                              CM1054.2
030600         PERFORM PASS   GO TO QUEUE-TEST-WRITE-09.                CM1054.2
030700     MOVE "PSPP" TO CORRECT-QUEUE.                                CM1054.2
030800     PERFORM FAIL.                                                CM1054.2
030900 QUEUE-TEST-WRITE-09.                                             CM1054.2
031000     MOVE "QUEUE-TEST-09" TO PAR-NAME.                            CM1054.2
031100     PERFORM PRINT-DETAIL.                                        CM1054.2
031200 QUEUE-TEST-10.                                                   CM1054.2
031300     MOVE "QUEUE SERIES PPS" TO FEATURE.                          CM1054.2
031400     MOVE PPS TO QUEUE-SET.                                       CM1054.2
031500     PERFORM RECEIVE-A-MSG.                                       CM1054.2
031600     IF QUEUE-KEY IS EQUAL TO "PPSP"                              CM1054.2
031700         PERFORM PASS  GO TO QUEUE-TEST-WRITE-10.                 CM1054.2
031800     MOVE "PPSP" TO CORRECT-QUEUE.                                CM1054.2
031900     PERFORM FAIL.                                                CM1054.2
032000 QUEUE-TEST-WRITE-10.                                             CM1054.2
032100     MOVE "QUEUE-TEST-10" TO PAR-NAME.                            CM1054.2
032200     PERFORM PRINT-DETAIL.                                        CM1054.2
032300 ACCEPT-TEST-01.                                                  CM1054.2
032400     MOVE "ACCEPT GROUP QUEUE" TO FEATURE.                        CM1054.2
032500     MOVE PPPP TO QUEUE-SET.                                      CM1054.2
032600     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
032700     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
032800         MOVE IN-STAT TO STAT                                     CM1054.2
032900         MOVE "BAD STATUS FOR PPPP" TO RE-MARK                    CM1054.2
033000         ELSE                                                     CM1054.2
033100         MOVE MSG-COUNT TO STAT                                   CM1054.2
033200         MOVE "COUNT FOR PPPP" TO RE-MARK.                        CM1054.2
033300     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
033400     MOVE PPPS TO QUEUE-SET.                                      CM1054.2
033500     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
033600     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
033700         MOVE IN-STAT TO STAT                                     CM1054.2
033800         MOVE "BAD STATUS FOR PPPS" TO RE-MARK                    CM1054.2
033900         ELSE                                                     CM1054.2
034000         MOVE MSG-COUNT TO STAT                                   CM1054.2
034100         MOVE "COUNT FOR PPPS" TO RE-MARK.                        CM1054.2
034200     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
034300     MOVE PPSP TO QUEUE-SET.                                      CM1054.2
034400     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
034500     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
034600         MOVE IN-STAT TO STAT                                     CM1054.2
034700         MOVE "BAD STATUS FOR PPSP"  TO RE-MARK                   CM1054.2
034800         ELSE                                                     CM1054.2
034900         MOVE MSG-COUNT TO STAT                                   CM1054.2
035000         MOVE "COUNT FOR PPSP" TO RE-MARK.                        CM1054.2
035100     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
035200     MOVE PSPP TO QUEUE-SET.                                      CM1054.2
035300     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
035400     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
035500         MOVE IN-STAT TO STAT                                     CM1054.2
035600         MOVE "BAD STATUS FOR PSPP" TO RE-MARK                    CM1054.2
035700         ELSE                                                     CM1054.2
035800         MOVE MSG-COUNT TO STAT                                   CM1054.2
035900         MOVE "COUNT FOR PSPP" TO RE-MARK.                        CM1054.2
036000     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
036100     MOVE P TO QUEUE-SET.                                         CM1054.2
036200     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
036300     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
036400         MOVE IN-STAT TO STAT                                     CM1054.2
036500         MOVE "BAD STATUS FOR P" TO RE-MARK                       CM1054.2
036600         ELSE                                                     CM1054.2
036700         MOVE MSG-COUNT TO STAT                                   CM1054.2
036800         MOVE "COUNT FOR P" TO RE-MARK.                           CM1054.2
036900     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
037000     MOVE PP TO QUEUE-SET.                                        CM1054.2
037100     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
037200     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
037300         MOVE IN-STAT TO STAT                                     CM1054.2
037400         MOVE "BAD STATUS FOR PP" TO STAT                         CM1054.2
037500         ELSE                                                     CM1054.2
037600         MOVE MSG-COUNT TO STAT                                   CM1054.2
037700         MOVE "COUNT FOR PP" TO RE-MARK.                          CM1054.2
037800     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
037900     MOVE PPP TO QUEUE-SET.                                       CM1054.2
038000     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
038100     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
038200         MOVE IN-STAT TO STAT                                     CM1054.2
038300         MOVE "BAD STATUS FOR PPP" TO STAT                        CM1054.2
038400         ELSE                                                     CM1054.2
038500         MOVE MSG-COUNT TO STAT                                   CM1054.2
038600         MOVE "COUNT FOR PPP" TO RE-MARK.                         CM1054.2
038700     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
038800     MOVE PS TO QUEUE-SET.                                        CM1054.2
038900     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
039000     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
039100         MOVE IN-STAT TO STAT                                     CM1054.2
039200         MOVE "BAD STATUS FOR PS" TO STAT                         CM1054.2
039300         ELSE                                                     CM1054.2
039400         MOVE MSG-COUNT TO STAT                                   CM1054.2
039500         MOVE "COUNT FOR PS" TO RE-MARK.                          CM1054.2
039600     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
039700     MOVE PSP TO QUEUE-SET.                                       CM1054.2
039800     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
039900     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
040000         MOVE IN-STAT TO STAT                                     CM1054.2
040100         MOVE "BAD STATUS FOR PSP" TO STAT                        CM1054.2
040200         ELSE                                                     CM1054.2
040300         MOVE MSG-COUNT TO STAT                                   CM1054.2
040400         MOVE "COUNT FOR PSP" TO RE-MARK.                         CM1054.2
040500     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
040600     MOVE PPS TO QUEUE-SET.                                       CM1054.2
040700     ACCEPT CM-INQUE-1 COUNT.                                     CM1054.2
040800     IF IN-STAT IS NOT EQUAL TO "00"                              CM1054.2
040900         MOVE IN-STAT TO STAT                                     CM1054.2
041000         MOVE "BAD STATUS FOR PPS" TO STAT                        CM1054.2
041100         ELSE                                                     CM1054.2
041200         MOVE MSG-COUNT TO STAT                                   CM1054.2
041300         MOVE "COUNT FOR PPS" TO RE-MARK.                         CM1054.2
041400     PERFORM ACCEPT-WRITE-01.                                     CM1054.2
041500     GO TO CM105-FINI.                                            CM1054.2
041600 ACCEPT-WRITE-01.                                                 CM1054.2
041700     MOVE "ACCEPT-TEST-01" TO PAR-NAME.                           CM1054.2
041800     MOVE "INFO" TO P-OR-F.                                       CM1054.2
041900     PERFORM PRINT-DETAIL.                                        CM1054.2
042000 CM105-FINI.                                                      CM1054.2
042100     PERFORM END-ROUTINE THRU END-ROUTINE-4.                      CM1054.2
042200     CLOSE PRINT-FILE.                                            CM1054.2
042300     STOP RUN.                                                    CM1054.2
042400 RECEIVE-A-MSG.                                                   CM1054.2
042500     MOVE SPACE TO RE-MARK.                                       CM1054.2
042600     RECEIVE CM-INQUE-1 MESSAGE INTO RE-MARK                      CM1054.2
042700         NO DATA MOVE "NOTHING RECEIVED FROM MCS" TO RE-MARK.     CM1054.2
042800 COMMON-SUBROUTINES SECTION.                                      CM1054.2
042900 PASS.                                                            CM1054.2
043000     MOVE "PASS" TO P-OR-F.                                       CM1054.2
043100 FAIL.                                                            CM1054.2
043200     MOVE " SEE REMARKS COLUMN " TO COMPUTED-A.                   CM1054.2
043300     ADD      1 TO ERROR-COUNTER.                                 CM1054.2
043400     MOVE "FAIL*" TO P-OR-F.                                      CM1054.2
043500 PRINT-DETAIL.                                                    CM1054.2
043600     MOVE     TEST-RESULTS TO PRINT-REC.                          CM1054.2
043700     PERFORM WRITE-LINE.                                          CM1054.2
043800     MOVE     SPACE TO P-OR-F.                                    CM1054.2
043900     MOVE     SPACE TO COMPUTED-A.                                CM1054.2
044000     MOVE SPACE TO CORRECT-A.                                     CM1054.2
044100     MOVE SPACE TO RE-MARK.                                       CM1054.2
044200 COLUMN-NAMES-ROUTINE.                                            CM1054.2
044300     MOVE     COLUMNS-LINE-1 TO DUMMY-RECORD.                     CM1054.2
044400     PERFORM WRITE-LINE.                                          CM1054.2
044500     MOVE     COLUMNS-LINE-2 TO DUMMY-RECORD.                     CM1054.2
044600     PERFORM WRITE-LINE.                                          CM1054.2
044700     PERFORM  BLANK-LINE-PRINT.                                   CM1054.2
044800 END-ROUTINE.                                                     CM1054.2
044900     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        CM1054.2
045000     PERFORM WRITE-LINE.                                          CM1054.2
045100 PARA-Z.                                                          CM1054.2
045200     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           CM1054.2
045300     MOVE     CCVS-E-1 TO DUMMY-RECORD.                           CM1054.2
045400     PERFORM WRITE-LINE.                                          CM1054.2
045500 END-ROUTINE-1.                                                   CM1054.2
045600     PERFORM  BLANK-LINE-PRINT.                                   CM1054.2
045700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      CM1054.2
045800              GO TO END-ROUTINE-2.                                CM1054.2
045900     MOVE     ERROR-COUNTER TO ERROR-TOTAL.                       CM1054.2
046000     GO TO    END-ROUTINE-3.                                      CM1054.2
046100 END-ROUTINE-2.                                                   CM1054.2
046200     MOVE " NO" TO ERROR-TOTAL.                                   CM1054.2
046300 END-ROUTINE-3.                                                   CM1054.2
046400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           CM1054.2
046500     PERFORM WRITE-LINE.                                          CM1054.2
046600     IF DELETE-CNT IS EQUAL TO ZERO                               CM1054.2
046700         MOVE " NO" TO ERROR-TOTAL  ELSE                          CM1054.2
046800     MOVE DELETE-CNT TO ERROR-TOTAL.                              CM1054.2
046900     MOVE "TESTS DELETED     " TO ENDER-DESC.                     CM1054.2
047000     MOVE CCVS-E-2 TO DUMMY-RECORD.                               CM1054.2
047100     PERFORM WRITE-LINE.                                          CM1054.2
047200 END-ROUTINE-4.                                                   CM1054.2
047300     MOVE CCVS-E-3 TO DUMMY-RECORD.                               CM1054.2
047400     PERFORM WRITE-LINE.                                          CM1054.2
047500 BLANK-LINE-PRINT.                                                CM1054.2
047600     MOVE     SPACE TO DUMMY-RECORD.                              CM1054.2
047700     PERFORM WRITE-LINE.                                          CM1054.2
047800 WRITE-LINE.                                                      CM1054.2
047900     WRITE DUMMY-RECORD AFTER ADVANCING 1 LINE.                   CM1054.2
048000 HEAD-ROUTINE.                                                    CM1054.2
048100     MOVE CCVS-H-1 TO PRINT-REC                                   CM1054.2
048200     WRITE PRINT-REC                                              CM1054.2
048300         AFTER ADVANCING PAGE.                                    CM1054.2
048400     MOVE CCVS-H-2 TO PRINT-REC.                                  CM1054.2
048500     WRITE PRINT-REC                                              CM1054.2
048600         AFTER 2 LINES.                                           CM1054.2
048700     MOVE CCVS-H-3 TO PRINT-REC.                                  CM1054.2
048800     WRITE PRINT-REC                                              CM1054.2
048900         AFTER 5 LINES.                                           CM1054.2
049000     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1054.2
049100     PERFORM WRITE-LINE.                                          CM1054.2
      *END-OF,CM105M                                                                  
