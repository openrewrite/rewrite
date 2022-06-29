      *HEADER,COBOL,CM202M                                                            
000100 IDENTIFICATION DIVISION.                                         CM2024.2
000200 PROGRAM-ID.                                                      CM2024.2
000300     CM202M.                                                      CM2024.2
000400 AUTHOR.                                                          CM2024.2
000500     FEDERAL COMPILER TESTING CENTER.                             CM2024.2
000600 INSTALLATION.                                                    CM2024.2
000700     GENERAL SERVICES ADMINISTRATION                              CM2024.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                CM2024.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 CM2024.2
001000     5203 LEESBURG PIKE  SUITE 1100                               CM2024.2
001100     FALLS CHURCH VIRGINIA 22041.                                 CM2024.2
001200                                                                  CM2024.2
001300     PHONE   (703) 756-6153                                       CM2024.2
001400                                                                  CM2024.2
001500     " HIGH       ".                                              CM2024.2
001600 DATE-WRITTEN.                                                    CM2024.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           CM2024.2
001800     CREATION DATE     /    VALIDATION DATE                       CM2024.2
001900     "4.2 ".                                                      CM2024.2
002000 SECURITY.                                                        CM2024.2
002100     NONE.                                                        CM2024.2
002200 ENVIRONMENT DIVISION.                                            CM2024.2
002300 CONFIGURATION SECTION.                                           CM2024.2
002400 SOURCE-COMPUTER.                                                 CM2024.2
002500     XXXXX082.                                                    CM2024.2
002600 OBJECT-COMPUTER.                                                 CM2024.2
002700     XXXXX083.                                                    CM2024.2
002800 INPUT-OUTPUT SECTION.                                            CM2024.2
002900 FILE-CONTROL.                                                    CM2024.2
003000     SELECT PRINT-FILE ASSIGN TO                                  CM2024.2
003100     XXXXX055.                                                    CM2024.2
003200 DATA DIVISION.                                                   CM2024.2
003300 FILE SECTION.                                                    CM2024.2
003400 FD  PRINT-FILE                                                   CM2024.2
003500     LABEL RECORDS                                                CM2024.2
003600     XXXXX084                                                     CM2024.2
003700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       CM2024.2
003800 01  PRINT-REC PICTURE X(120).                                    CM2024.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 CM2024.2
004000 WORKING-STORAGE SECTION.                                         CM2024.2
004100 77  END-FLAG PIC 9.                                              CM2024.2
004200 77  FAIL-SAFE PIC 999 COMP.                                      CM2024.2
004300 77  ELAPSED-TIME PIC 999.                                        CM2024.2
004400 77  ENABLE-ALL-MSG PIC X(57) VALUE                               CM2024.2
004500     "ATTEMPT TO ENTER MESSAGES.  BOTH SOURCES ARE NOW ENABLED.". CM2024.2
004600 01  INIT-TIME.                                                   CM2024.2
004700     02  I-HRS PIC 99.                                            CM2024.2
004800     02  I-MINS PIC 99.                                           CM2024.2
004900     02  I-SECS PIC 99V99.                                        CM2024.2
005000 01  TEST-TIME.                                                   CM2024.2
005100     02  T-HRS PIC 99.                                            CM2024.2
005200     02  T-MINS PIC 99.                                           CM2024.2
005300     02  T-SECS PIC 99V99.                                        CM2024.2
005400 01  SKIP-MSG-1 PIC X(71) VALUE "SEGMENTED-MSG-TEST-05: THERE SHOUCM2024.2
005500-    "LD BE NO BLANK LINES BETWEEN THIS LINE".                    CM2024.2
005600 01  SKIP-MSG-2 PIC X(17) VALUE "AND THIS ONE.".                  CM2024.2
005700 01  INCOMP-MSG.                                                  CM2024.2
005800     02  FILLER PIC X(20) VALUE "INCOMPLETE-MSG-TEST-".           CM2024.2
005900     02  INC-MSG-NO PIC 99.                                       CM2024.2
006000     02  FILLER PIC X(33) VALUE " FAILURE IF THIS SEGMENT APPEARS.CM2024.2
006100-    "".                                                          CM2024.2
006200 01  ENABLE-MSG.                                                  CM2024.2
006300     02  FILLER PIC X(34) VALUE "ATTEMPT TO ENTER MESSAGES.  ONLY CM2024.2
006400-    """".                                                        CM2024.2
006500     02  SELECTED-SOURCE PIC X(12).                               CM2024.2
006600     02  FILLER PIC X(13) VALUE """ IS ENABLED.".                 CM2024.2
006700 01  DISABLE-MSG.                                                 CM2024.2
006800     02  FILLER PIC X(36) VALUE                                   CM2024.2
006900     "TERMINALS DEACTIVATED FOR INPUT:    ".                      CM2024.2
007000     02  TERMINAL-1 PIC X(14) VALUE                               CM2024.2
007100     XXXXX042.                                                    CM2024.2
007200     02  TERMINAL-2 PIC X(12) VALUE                               CM2024.2
007300     XXXXX043.                                                    CM2024.2
007400 01  SEND-MSG.                                                    CM2024.2
007500     02  FILLER PIC X(17) VALUE "SEND-STATUS-TEST-".              CM2024.2
007600     02  TEST-NUMB PIC 99 VALUE 1.                                CM2024.2
007700     02  FILLER PIC X(42) VALUE                                   CM2024.2
007800     " FAILURE.  THIS MESSAGE SHOULD NOT APPEAR.".                CM2024.2
007900 01  ONE-TERMINAL-MSG.                                            CM2024.2
008000     02  FILLER PIC X(15) VALUE "ONLY TERMINAL """.               CM2024.2
008100     02  SELECTED-DEST PIC X(12) VALUE                            CM2024.2
008200     XXXXX032.                                                    CM2024.2
008300     02  FILLER PIC X(30) VALUE                                   CM2024.2
008400     """ SHOULD RECEIVE THIS MESSAGE.".                           CM2024.2
008500 01  SEG-INIT.                                                    CM2024.2
008600     02  FILLER PIC X(19) VALUE "SEGMENTED-MSG-TEST-".            CM2024.2
008700     02  SEG-TEST-NO PIC 99.                                      CM2024.2
008800     02  FILLER PIC X(19) VALUE " SEGMENT INITIATED ".            CM2024.2
008900     02  FILLER PIC X(16) VALUE "-LENGTH FAILURE ".               CM2024.2
009000 01  SEG-CONT PIC X(10) VALUE "-CONTINUED".                       CM2024.2
009100 01  MSG-COMP PIC X(14) VALUE "-MSG COMPLETE.".                   CM2024.2
009200 01  GROUP-COMP PIC X(16) VALUE "-GROUP COMPLETE.".               CM2024.2
009300 01  TEST-RESULTS.                                                CM2024.2
009400     02 FILLER                    PICTURE X VALUE SPACE.          CM2024.2
009500     02 FEATURE                   PICTURE X(18).                  CM2024.2
009600     02 FILLER                    PICTURE X VALUE SPACE.          CM2024.2
009700     02 P-OR-F                    PICTURE X(5).                   CM2024.2
009800     02 FILLER                    PICTURE X  VALUE SPACE.         CM2024.2
009900     02  PAR-NAME PIC X(20).                                      CM2024.2
010000     02 FILLER                    PICTURE X VALUE SPACE.          CM2024.2
010100     02  COMPUTED-A.                                              CM2024.2
010200         03  FILLER PIC X(9).                                     CM2024.2
010300         03  COMPUTED-STATUS PIC XX.                              CM2024.2
010400         03  FILLER PIC X(9).                                     CM2024.2
010500     02 FILLER                    PICTURE X VALUE SPACE.          CM2024.2
010600     02  CORRECT-A.                                               CM2024.2
010700         03  FILLER PIC X(9).                                     CM2024.2
010800         03  CORRECT-STATUS PIC XX.                               CM2024.2
010900         03  FILLER PIC X(9).                                     CM2024.2
011000     02 FILLER                    PICTURE X VALUE SPACE.          CM2024.2
011100     02  RE-MARK.                                                 CM2024.2
011200         03  MSG-1 PIC X.                                         CM2024.2
011300         03  FILLER PIC X(29).                                    CM2024.2
011400 01  COLUMNS-LINE-1.                                              CM2024.2
011500     02  FILLER PIC X(3) VALUE SPACES.                            CM2024.2
011600     02  FILLER PIC X(17) VALUE "FEATURE TESTED".                 CM2024.2
011700     02  FILLER PIC X(9) VALUE "RESLT".                           CM2024.2
011800     02  FILLER PIC X(21) VALUE "PARAGRAPH NAME".                 CM2024.2
011900     02  FILLER PIC X(22) VALUE "COMPUTED DATA".                  CM2024.2
012000     02  FILLER PIC X(29) VALUE "CORRECT DATA".                   CM2024.2
012100     02  FILLER PIC X(7) VALUE "REMARKS".                         CM2024.2
012200 01  COLUMNS-LINE-2.                                              CM2024.2
012300     02  FILLER PIC X VALUE SPACE.                                CM2024.2
012400     02  FILLER PIC X(18) VALUE ALL "-".                          CM2024.2
012500     02  FILLER PIC X VALUE SPACE.                                CM2024.2
012600     02  FILLER PIC X(5) VALUE ALL "-".                           CM2024.2
012700     02  FILLER PIC X VALUE SPACE.                                CM2024.2
012800     02  FILLER PIC X(20) VALUE ALL "-".                          CM2024.2
012900     02  FILLER PIC X VALUE SPACE.                                CM2024.2
013000     02  FILLER PIC X(20) VALUE ALL "-".                          CM2024.2
013100     02  FILLER PIC X VALUE SPACE.                                CM2024.2
013200     02  FILLER PIC X(20) VALUE ALL "-".                          CM2024.2
013300     02  FILLER PIC X VALUE SPACE.                                CM2024.2
013400     02  FILLER PIC X(31) VALUE ALL "-".                          CM2024.2
013500 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         CM2024.2
013600 01  REC-CT PICTURE 99 VALUE ZERO.                                CM2024.2
013700 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        CM2024.2
013800 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  CM2024.2
013900 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          CM2024.2
014000 01  PASS-COUNTER PIC 999 VALUE ZERO.                             CM2024.2
014100 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              CM2024.2
014200 01  ERROR-HOLD PIC 999 VALUE ZERO.                               CM2024.2
014300 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           CM2024.2
014400 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            CM2024.2
014500 01  CCVS-H-1.                                                    CM2024.2
014600     02  FILLER   PICTURE X(27)  VALUE SPACE.                     CM2024.2
014700     02 FILLER PICTURE X(67) VALUE                                CM2024.2
014800     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  CM2024.2
014900-    " SYSTEM".                                                   CM2024.2
015000     02  FILLER     PICTURE X(26)  VALUE SPACE.                   CM2024.2
015100 01  CCVS-H-2.                                                    CM2024.2
015200     02 FILLER PICTURE X(52) VALUE IS                             CM2024.2
015300     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   CM2024.2
015400     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   CM2024.2
015500     02 TEST-ID PICTURE IS X(9).                                  CM2024.2
015600     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   CM2024.2
015700 01  CCVS-H-3.                                                    CM2024.2
015800     02  FILLER PICTURE X(34) VALUE                               CM2024.2
015900     " FOR OFFICIAL USE ONLY    ".                                CM2024.2
016000     02  FILLER PICTURE X(58) VALUE                               CM2024.2
016100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".CM2024.2
016200     02  FILLER PICTURE X(28) VALUE                               CM2024.2
016300     "  COPYRIGHT   1974 ".                                       CM2024.2
016400 01  CCVS-E-1.                                                    CM2024.2
016500     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   CM2024.2
016600     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        CM2024.2
016700     02 ID-AGAIN PICTURE IS X(9).                                 CM2024.2
016800     02 FILLER PICTURE X(45) VALUE IS                             CM2024.2
016900     " NTIS DISTRIBUTION COBOL 74".                               CM2024.2
017000 01  CCVS-E-2.                                                    CM2024.2
017100     02  FILLER                   PICTURE X(31)  VALUE            CM2024.2
017200     SPACE.                                                       CM2024.2
017300     02  FILLER                   PICTURE X(21)  VALUE SPACE.     CM2024.2
017400     02 CCVS-E-2-2.                                               CM2024.2
017500         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            CM2024.2
017600         03 FILLER PICTURE IS X VALUE IS SPACE.                   CM2024.2
017700         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      CM2024.2
017800 01  CCVS-E-3.                                                    CM2024.2
017900     02  FILLER PICTURE X(22) VALUE                               CM2024.2
018000     " FOR OFFICIAL USE ONLY".                                    CM2024.2
018100     02  FILLER PICTURE X(12) VALUE SPACE.                        CM2024.2
018200     02  FILLER PICTURE X(58) VALUE                               CM2024.2
018300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".CM2024.2
018400     02  FILLER PICTURE X(13) VALUE SPACE.                        CM2024.2
018500     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 CM2024.2
018600 01  CCVS-E-4.                                                    CM2024.2
018700     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           CM2024.2
018800     02 FILLER PIC XXXX VALUE " OF ".                             CM2024.2
018900     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           CM2024.2
019000     02 FILLER PIC X(40) VALUE                                    CM2024.2
019100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       CM2024.2
019200 01  XXINFO.                                                      CM2024.2
019300     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    CM2024.2
019400     02 INFO-TEXT.                                                CM2024.2
019500     04 FILLER PIC X(20) VALUE SPACE.                             CM2024.2
019600     04 XXCOMPUTED PIC X(20).                                     CM2024.2
019700     04 FILLER PIC X(5) VALUE SPACE.                              CM2024.2
019800     04 XXCORRECT PIC X(20).                                      CM2024.2
019900 01  HYPHEN-LINE.                                                 CM2024.2
020000     02 FILLER PICTURE IS X VALUE IS SPACE.                       CM2024.2
020100     02 FILLER PICTURE IS X(65) VALUE IS "************************CM2024.2
020200-    "*****************************************".                 CM2024.2
020300     02 FILLER PICTURE IS X(54) VALUE IS "************************CM2024.2
020400-    "******************************".                            CM2024.2
020500 01  CCVS-PGM-ID PIC X(6) VALUE                                   CM2024.2
020600     "CM202M".                                                    CM2024.2
020700 COMMUNICATION SECTION.                                           CM2024.2
020800 CD  CM-INQUE-1 INPUT.                                            CM2024.2
020900 01  INQUE-1-RECORD.                                              CM2024.2
021000     02  QUEUE-SET PIC X(12) VALUE                                CM2024.2
021100     XXXXX030.                                                    CM2024.2
021200     02  FILLER PIC X(36) VALUE SPACES.                           CM2024.2
021300     02  FILLER PIC X(14).                                        CM2024.2
021400     02  SYM-SOURCE PIC X(12).                                    CM2024.2
021500     02  IN-LENGTH PIC 9999.                                      CM2024.2
021600     02  END-KEY PIC X.                                           CM2024.2
021700     02  IN-STATUS PIC XX.                                        CM2024.2
021800     02  MSG-COUNT PIC 9(6).                                      CM2024.2
021900 CD  CM-OUTQUE-1 OUTPUT                                           CM2024.2
022000     DESTINATION COUNT DEST-COUNT                                 CM2024.2
022100     TEXT LENGTH OUT-LENGTH                                       CM2024.2
022200     STATUS KEY OUT-STATUS                                        CM2024.2
022300     DESTINATION TABLE OCCURS 2 TIMES INDEXED BY I1               CM2024.2
022400     ERROR KEY ERR-KEY                                            CM2024.2
022500     DESTINATION SYM-DEST.                                        CM2024.2
022600 PROCEDURE    DIVISION.                                           CM2024.2
022700 SECT-CM202M-0001 SECTION.                                        CM2024.2
022800 CM202M-INIT.                                                     CM2024.2
022900     OPEN     OUTPUT PRINT-FILE.                                  CM2024.2
023000     MOVE "CM202M     " TO TEST-ID.                               CM2024.2
023100     MOVE     TEST-ID TO ID-AGAIN.                                CM2024.2
023200     MOVE    SPACE TO TEST-RESULTS.                               CM2024.2
023300     PERFORM HEAD-ROUTINE.                                        CM2024.2
023400     MOVE 2 TO DEST-COUNT                                         CM2024.2
023500     MOVE                                                         CM2024.2
023600     XXXXX032                                                     CM2024.2
023700         TO SYM-DEST (1).                                         CM2024.2
023800     MOVE                                                         CM2024.2
023900     XXXXX035                                                     CM2024.2
024000         TO SYM-DEST (2).                                         CM2024.2
024100     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM2024.2
024200     XXXXX033.                                                    CM2024.2
024300     MOVE                                                         CM2024.2
024400     XXXXX042                                                     CM2024.2
024500         TO SELECTED-SOURCE  SYM-SOURCE.                          CM2024.2
024600     MOVE 59 TO OUT-LENGTH.                                       CM2024.2
024700     SEND CM-OUTQUE-1 FROM ENABLE-MSG WITH EMI.                   CM2024.2
024800     ENABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                    CM2024.2
024900     XXXXX031.                                                    CM2024.2
025000     PERFORM DELAY-FOR-30.                                        CM2024.2
025100     DISABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                   CM2024.2
025200     XXXXX031.                                                    CM2024.2
025300     MOVE                                                         CM2024.2
025400     XXXXX043                                                     CM2024.2
025500         TO SELECTED-SOURCE  SYM-SOURCE.                          CM2024.2
025600     SEND CM-OUTQUE-1 FROM ENABLE-MSG WITH EMI.                   CM2024.2
025700     ENABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                    CM2024.2
025800     XXXXX031.                                                    CM2024.2
025900     PERFORM DELAY-FOR-30.                                        CM2024.2
026000     MOVE 57 TO OUT-LENGTH.                                       CM2024.2
026100     SEND CM-OUTQUE-1 FROM ENABLE-ALL-MSG WITH EMI.               CM2024.2
026200     MOVE                                                         CM2024.2
026300     XXXXX042                                                     CM2024.2
026400         TO SYM-SOURCE.                                           CM2024.2
026500     ENABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                    CM2024.2
026600     XXXXX031.                                                    CM2024.2
026700     PERFORM DELAY-FOR-30.                                        CM2024.2
026800     DISABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                   CM2024.2
026900     XXXXX031.                                                    CM2024.2
027000     MOVE                                                         CM2024.2
027100     XXXXX043                                                     CM2024.2
027200         TO SYM-SOURCE.                                           CM2024.2
027300     DISABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                   CM2024.2
027400     XXXXX031.                                                    CM2024.2
027500     MOVE 62 TO OUT-LENGTH.                                       CM2024.2
027600     SEND CM-OUTQUE-1 FROM DISABLE-MSG WITH EMI.                  CM2024.2
027700     PERFORM DELAY-FOR-30.                                        CM2024.2
027800 BEGIN-CM202M-STATUS-TESTS.                                       CM2024.2
027900     MOVE 200 TO FAIL-SAFE.                                       CM2024.2
028000     MOVE "RCV MSG/SHORT AREA" TO FEATURE.                        CM2024.2
028100     MOVE "RECEIVE-TEST-01" TO PAR-NAME.                          CM2024.2
028200     MOVE " SEE REMARKS COLUMN " TO COMPUTED-A.                   CM2024.2
028300     MOVE "  INSPECT RESULTS" TO CORRECT-A.                       CM2024.2
028400 RECEIVE-TEST-01.                                                 CM2024.2
028500     RECEIVE CM-INQUE-1 MESSAGE INTO MSG-1  NO DATA               CM2024.2
028600         MOVE "QUEUE TESTED EMPTY" TO COMPUTED-A                  CM2024.2
028700         MOVE SPACES TO CORRECT-A RE-MARK                         CM2024.2
028800         PERFORM FAIL  PERFORM PRINT-DETAIL                       CM2024.2
028900         GO TO RECEIVE-TEST-02-INIT.                              CM2024.2
029000     IF END-KEY IS EQUAL TO "1"                                   CM2024.2
029100         MOVE SPACE TO CORRECT-A                                  CM2024.2
029200         MOVE "   ESI WAS SENSED   " TO COMPUTED-A                CM2024.2
029300         PERFORM FAIL                                             CM2024.2
029400         PERFORM PRINT-DETAIL                                     CM2024.2
029500         GO TO RECEIVE-TEST-02-INIT.                              CM2024.2
029600     PERFORM PRINT-DETAIL.                                        CM2024.2
029700     IF END-KEY IS NOT EQUAL TO "0" GO TO RECEIVE-TEST-02-INIT.   CM2024.2
029800     MOVE "RECEIVE-TEST-01-CONT" TO PAR-NAME.                     CM2024.2
029900     SUBTRACT 1 FROM FAIL-SAFE.                                   CM2024.2
030000     IF FAIL-SAFE IS EQUAL TO 0                                   CM2024.2
030100         MOVE "FAIL-SAFE ACTIVATED" TO CORRECT-A                  CM2024.2
030200         MOVE "****** WARNING *****" TO COMPUTED-A                CM2024.2
030300         PERFORM PRINT-DETAIL                                     CM2024.2
030400         GO TO RECEIVE-TEST-02-INIT.                              CM2024.2
030500     PERFORM PRINT-DETAIL.                                        CM2024.2
030600     GO TO RECEIVE-TEST-01.                                       CM2024.2
030700 RECEIVE-DELETE-01.                                               CM2024.2
030800     PERFORM DE-LETE.                                             CM2024.2
030900     PERFORM PRINT-DETAIL.                                        CM2024.2
031000 RECEIVE-TEST-02-INIT.                                            CM2024.2
031100     MOVE 200 TO FAIL-SAFE.                                       CM2024.2
031200     MOVE "RCV SEG/SHORT AREA" TO FEATURE.                        CM2024.2
031300     MOVE "RECEIVE-TEST-02" TO PAR-NAME.                          CM2024.2
031400     MOVE " SEE REMARKS COLUMN " TO COMPUTED-A.                   CM2024.2
031500     MOVE "  INSPECT RESULTS" TO CORRECT-A.                       CM2024.2
031600 RECEIVE-TEST-02.                                                 CM2024.2
031700     RECEIVE CM-INQUE-1 SEGMENT INTO MSG-1  NO DATA               CM2024.2
031800         MOVE "QUEUE TESTED EMPTY" TO COMPUTED-A                  CM2024.2
031900         MOVE SPACES TO CORRECT-A  RE-MARK                        CM2024.2
032000         PERFORM FAIL  PERFORM PRINT-DETAIL                       CM2024.2
032100         GO TO RECEIVE-TEST-03-INIT.                              CM2024.2
032200     PERFORM PRINT-DETAIL.                                        CM2024.2
032300     IF END-KEY IS NOT EQUAL TO "0" GO TO RECEIVE-TEST-03-INIT.   CM2024.2
032400     MOVE "RECEIVE-TEST-02-CONT" TO PAR-NAME.                     CM2024.2
032500     SUBTRACT 1 FROM FAIL-SAFE.                                   CM2024.2
032600     IF FAIL-SAFE IS EQUAL TO 0                                   CM2024.2
032700         MOVE "FAIL-SAFE ACTIVATED" TO CORRECT-A                  CM2024.2
032800         MOVE "****** WARNING *****" TO COMPUTED-A                CM2024.2
032900         PERFORM PRINT-DETAIL                                     CM2024.2
033000         GO TO RECEIVE-TEST-03-INIT.                              CM2024.2
033100     PERFORM PRINT-DETAIL.                                        CM2024.2
033200     GO TO RECEIVE-TEST-02.                                       CM2024.2
033300 RECEIVE-DELETE-02.                                               CM2024.2
033400     PERFORM DE-LETE.                                             CM2024.2
033500     PERFORM PRINT-DETAIL.                                        CM2024.2
033600 RECEIVE-TEST-03-INIT.                                            CM2024.2
033700     MOVE "RCV SEG REPEATEDLY" TO FEATURE.                        CM2024.2
033800     MOVE "RECEIVE-TEST-03" TO PAR-NAME.                          CM2024.2
033900 RECEIVE-TEST-03.                                                 CM2024.2
034000     MOVE "COMPTD SHOWS END KEY" TO CORRECT-A.                    CM2024.2
034100     MOVE ALL "*" TO RE-MARK.                                     CM2024.2
034200     RECEIVE CM-INQUE-1 SEGMENT INTO RE-MARK                      CM2024.2
034300         NO DATA GO TO ENABL-STATUS-TEST-01.                      CM2024.2
034400     MOVE END-KEY TO COMPUTED-STATUS.                             CM2024.2
034500     PERFORM PRINT-DETAIL.                                        CM2024.2
034600     MOVE "RECEIVE-TEST-03-CONT" TO PAR-NAME.                     CM2024.2
034700     GO TO RECEIVE-TEST-03.                                       CM2024.2
034800 RECEIVE-DELETE-03.                                               CM2024.2
034900     PERFORM DE-LETE.                                             CM2024.2
035000     PERFORM PRINT-DETAIL.                                        CM2024.2
035100 ENABL-STATUS-TEST-01.                                            CM2024.2
035200     MOVE "ENABLE TERMINAL" TO FEATURE.                           CM2024.2
035300     MOVE "BAD PASSWORD SUPPLIED" TO RE-MARK.                     CM2024.2
035400     ENABLE INPUT TERMINAL CM-INQUE-1 WITH KEY "GARBAGE".         CM2024.2
035500     IF IN-STATUS IS EQUAL TO "40"                                CM2024.2
035600         PERFORM PASS GO TO ENABL-STATUS-WRITE-01.                CM2024.2
035700     MOVE IN-STATUS TO COMPUTED-STATUS.                           CM2024.2
035800     MOVE "40" TO CORRECT-STATUS.                                 CM2024.2
035900     PERFORM FAIL.                                                CM2024.2
036000     GO TO ENABL-STATUS-WRITE-01.                                 CM2024.2
036100 ENABL-STATUS-DELETE-01.                                          CM2024.2
036200     PERFORM DE-LETE.                                             CM2024.2
036300 ENABL-STATUS-WRITE-01.                                           CM2024.2
036400     MOVE "ENABL-STATUS-TEST-01" TO PAR-NAME.                     CM2024.2
036500     PERFORM PRINT-DETAIL.                                        CM2024.2
036600 ENABL-STATUS-TEST-02.                                            CM2024.2
036700     MOVE "BAD SOURCE NAME USED" TO RE-MARK.                      CM2024.2
036800     MOVE "GARBAGE" TO SYM-SOURCE.                                CM2024.2
036900     ENABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                    CM2024.2
037000     XXXXX031.                                                    CM2024.2
037100     IF IN-STATUS IS EQUAL TO "21"                                CM2024.2
037200         PERFORM PASS  GO TO ENABL-STATUS-WRITE-02.               CM2024.2
037300     MOVE IN-STATUS TO COMPUTED-STATUS.                           CM2024.2
037400     MOVE "21" TO CORRECT-STATUS.                                 CM2024.2
037500     PERFORM FAIL.                                                CM2024.2
037600     GO TO ENABL-STATUS-WRITE-02.                                 CM2024.2
037700 ENABL-STATUS-DELETE-02.                                          CM2024.2
037800     PERFORM DE-LETE.                                             CM2024.2
037900 ENABL-STATUS-WRITE-02.                                           CM2024.2
038000     MOVE "ENABL-STATUS-TEST-02" TO PAR-NAME.                     CM2024.2
038100     PERFORM PRINT-DETAIL.                                        CM2024.2
038200 DISAB-STATUS-TEST-01.                                            CM2024.2
038300     MOVE "DISABLE TERMINAL" TO FEATURE.                          CM2024.2
038400     MOVE "BAD PASSWORD SUPPLIED" TO RE-MARK.                     CM2024.2
038500     MOVE                                                         CM2024.2
038600     XXXXX042                                                     CM2024.2
038700         TO SYM-SOURCE.                                           CM2024.2
038800     DISABLE INPUT TERMINAL CM-INQUE-1 WITH KEY "GARBAGE".        CM2024.2
038900     IF IN-STATUS IS EQUAL TO "40"                                CM2024.2
039000         PERFORM PASS  GO TO DISAB-STATUS-WRITE-01.               CM2024.2
039100     MOVE IN-STATUS TO COMPUTED-STATUS.                           CM2024.2
039200     MOVE "40" TO CORRECT-STATUS.                                 CM2024.2
039300     PERFORM FAIL.                                                CM2024.2
039400     GO TO DISAB-STATUS-WRITE-01.                                 CM2024.2
039500 DISAB-STATUS-DELETE-01.                                          CM2024.2
039600     PERFORM DE-LETE.                                             CM2024.2
039700 DISAB-STATUS-WRITE-01.                                           CM2024.2
039800     MOVE "DISAB-STATUS-TEST-01" TO PAR-NAME.                     CM2024.2
039900     PERFORM PRINT-DETAIL.                                        CM2024.2
040000 DISAB-STATUS-TEST-02.                                            CM2024.2
040100     MOVE "BAD SOURCE NAME USED" TO RE-MARK.                      CM2024.2
040200     MOVE "GARBAGE" TO SYM-SOURCE.                                CM2024.2
040300     DISABLE INPUT TERMINAL CM-INQUE-1 WITH KEY                   CM2024.2
040400     XXXXX031.                                                    CM2024.2
040500     IF IN-STATUS IS EQUAL TO "21"                                CM2024.2
040600         PERFORM PASS  GO TO DISAB-STATUS-WRITE-02.               CM2024.2
040700     MOVE IN-STATUS TO COMPUTED-STATUS.                           CM2024.2
040800     MOVE "21" TO CORRECT-STATUS.                                 CM2024.2
040900     PERFORM FAIL.                                                CM2024.2
041000     GO TO DISAB-STATUS-WRITE-02.                                 CM2024.2
041100 DISAB-STATUS-DELETE-02.                                          CM2024.2
041200     PERFORM DE-LETE.                                             CM2024.2
041300 DISAB-STATUS-WRITE-02.                                           CM2024.2
041400     MOVE "DISAB-STATUS-TEST-02" TO PAR-NAME.                     CM2024.2
041500     PERFORM PRINT-DETAIL.                                        CM2024.2
041600 SEND-STATUS-TEST-01.                                             CM2024.2
041700     MOVE "COUNT EXCEEDS LIMIT" TO RE-MARK.                       CM2024.2
041800     MOVE 3 TO DEST-COUNT.                                        CM2024.2
041900     MOVE 61 TO OUT-LENGTH.                                       CM2024.2
042000     SEND CM-OUTQUE-1 FROM SEND-MSG WITH EMI.                     CM2024.2
042100     IF OUT-STATUS IS EQUAL TO "30"                               CM2024.2
042200         PERFORM PASS  GO TO SEND-STATUS-WRITE-01.                CM2024.2
042300     MOVE OUT-STATUS TO COMPUTED-STATUS.                          CM2024.2
042400     MOVE "30" TO CORRECT-STATUS.                                 CM2024.2
042500     PERFORM FAIL.                                                CM2024.2
042600     GO TO SEND-STATUS-WRITE-01.                                  CM2024.2
042700 SEND-STATUS-DELETE-01.                                           CM2024.2
042800     PERFORM DE-LETE.                                             CM2024.2
042900 SEND-STATUS-WRITE-01.                                            CM2024.2
043000     MOVE "DESTINATION COUNT" TO FEATURE.                         CM2024.2
043100     MOVE "SEND-STATUS-TEST-01" TO PAR-NAME.                      CM2024.2
043200     PERFORM PRINT-DETAIL.                                        CM2024.2
043300     MOVE 2 TO DEST-COUNT.                                        CM2024.2
043400 SEND-STATUS-TEST-02.                                             CM2024.2
043500     MOVE 2 TO TEST-NUMB.                                         CM2024.2
043600     MOVE 0 TO OUT-LENGTH.                                        CM2024.2
043700     SEND CM-OUTQUE-1 FROM SEND-MSG.                              CM2024.2
043800     IF OUT-STATUS IS EQUAL TO "60"                               CM2024.2
043900         PERFORM PASS  GO TO SEND-STATUS-WRITE-02.                CM2024.2
044000     MOVE OUT-STATUS TO COMPUTED-STATUS.                          CM2024.2
044100     MOVE "60" TO CORRECT-STATUS.                                 CM2024.2
044200     PERFORM FAIL.                                                CM2024.2
044300     GO TO SEND-STATUS-WRITE-02.                                  CM2024.2
044400 SEND-STATUS-DELETE-02.                                           CM2024.2
044500     PERFORM DE-LETE.                                             CM2024.2
044600 SEND-STATUS-WRITE-02.                                            CM2024.2
044700     MOVE "PARTIAL SEGMENT" TO FEATURE.                           CM2024.2
044800     MOVE "ZERO CHARACTER COUNT USED" TO RE-MARK.                 CM2024.2
044900     MOVE "SEND-STATUS-TEST-02" TO PAR-NAME.                      CM2024.2
045000     PERFORM PRINT-DETAIL.                                        CM2024.2
045100 SEND-STATUS-TEST-03.                                             CM2024.2
045200     MOVE 0 TO END-FLAG.                                          CM2024.2
045300     MOVE 3 TO TEST-NUMB.                                         CM2024.2
045400     MOVE 61 TO OUT-LENGTH.                                       CM2024.2
045500     SEND CM-OUTQUE-1 WITH END-FLAG.                              CM2024.2
045600     IF OUT-STATUS IS EQUAL TO "60"                               CM2024.2
045700         PERFORM PASS  GO TO SEND-STATUS-WRITE-03.                CM2024.2
045800     MOVE OUT-STATUS TO COMPUTED-STATUS.                          CM2024.2
045900     MOVE "60" TO CORRECT-STATUS.                                 CM2024.2
046000     PERFORM FAIL.                                                CM2024.2
046100     GO TO SEND-STATUS-WRITE-03.                                  CM2024.2
046200 SEND-STATUS-DELETE-03.                                           CM2024.2
046300     PERFORM DE-LETE.                                             CM2024.2
046400 SEND-STATUS-WRITE-03.                                            CM2024.2
046500     MOVE "NO SENDING AREA SPECIFIED" TO RE-MARK.                 CM2024.2
046600     MOVE "SEND-STATUS-TEST-03" TO PAR-NAME.                      CM2024.2
046700     PERFORM PRINT-DETAIL.                                        CM2024.2
046800 SEND-STATUS-TEST-04.                                             CM2024.2
046900     MOVE 57 TO OUT-LENGTH.                                       CM2024.2
047000     MOVE "GARBAGE" TO SYM-DEST (2).                              CM2024.2
047100     SEND CM-OUTQUE-1 FROM ONE-TERMINAL-MSG WITH EMI.             CM2024.2
047200     IF OUT-STATUS IS NOT EQUAL TO "20"                           CM2024.2
047300         MOVE OUT-STATUS TO COMPUTED-STATUS                       CM2024.2
047400         MOVE "STATUS SHOULD BE 20" TO CORRECT-A                  CM2024.2
047500         PERFORM FAIL                                             CM2024.2
047600     ELSE IF ERR-KEY (2) IS NOT EQUAL TO "1"                      CM2024.2
047700         MOVE ERR-KEY (2) TO COMPUTED-STATUS                      CM2024.2
047800         MOVE " ERROR KEY (2) = 1" TO CORRECT-A                   CM2024.2
047900         PERFORM FAIL                                             CM2024.2
048000     ELSE IF ERR-KEY (1) IS NOT EQUAL TO "0"                      CM2024.2
048100         MOVE ERR-KEY (1) TO COMPUTED-STATUS                      CM2024.2
048200         MOVE " ERROR KEY (1) = 0" TO CORRECT-A                   CM2024.2
048300         PERFORM FAIL                                             CM2024.2
048400     ELSE PERFORM PASS.                                           CM2024.2
048500     GO TO SEND-STATUS-WRITE-04.                                  CM2024.2
048600 SEND-STATUS-DELETE-04.                                           CM2024.2
048700     PERFORM DE-LETE.                                             CM2024.2
048800 SEND-STATUS-WRITE-04.                                            CM2024.2
048900     MOVE "SYMBOLIC DESTINAT""N (2) IS BAD" TO RE-MARK.           CM2024.2
049000     MOVE "SEND-STATUS-TEST-04" TO PAR-NAME.                      CM2024.2
049100     PERFORM PRINT-DETAIL.                                        CM2024.2
049200     MOVE                                                         CM2024.2
049300     XXXXX035                                                     CM2024.2
049400         TO SYM-DEST (2).                                         CM2024.2
049500 SEGMENTED-MSG-TEST-01.                                           CM2024.2
049600     MOVE 1 TO SEG-TEST-NO.                                       CM2024.2
049700     MOVE 39 TO OUT-LENGTH.                                       CM2024.2
049800     SEND CM-OUTQUE-1 FROM SEG-INIT WITH ESI.                     CM2024.2
049900     MOVE 14 TO OUT-LENGTH.                                       CM2024.2
050000     SEND CM-OUTQUE-1 FROM MSG-COMP WITH EMI.                     CM2024.2
050100 SEGMENTED-MSG-TEST-02.                                           CM2024.2
050200     MOVE 2 TO SEG-TEST-NO.                                       CM2024.2
050300     MOVE 39 TO OUT-LENGTH.                                       CM2024.2
050400     SEND CM-OUTQUE-1 FROM SEG-INIT WITH ESI.                     CM2024.2
050500     MOVE 16 TO OUT-LENGTH.                                       CM2024.2
050600     SEND CM-OUTQUE-1 FROM GROUP-COMP WITH EGI.                   CM2024.2
050700 SEGMENTED-MSG-TEST-03.                                           CM2024.2
050800     MOVE 3 TO SEG-TEST-NO.                                       CM2024.2
050900     MOVE 39 TO OUT-LENGTH.                                       CM2024.2
051000     SEND CM-OUTQUE-1 FROM SEG-INIT.                              CM2024.2
051100     MOVE 10 TO OUT-LENGTH.                                       CM2024.2
051200     SEND CM-OUTQUE-1 FROM SEG-CONT WITH ESI.                     CM2024.2
051300     MOVE 14 TO OUT-LENGTH.                                       CM2024.2
051400     SEND CM-OUTQUE-1 FROM MSG-COMP WITH EMI.                     CM2024.2
051500 SEGMENTED-MSG-TEST-04.                                           CM2024.2
051600     MOVE 4 TO SEG-TEST-NO.                                       CM2024.2
051700     MOVE 0 TO END-FLAG.                                          CM2024.2
051800     MOVE 39 TO OUT-LENGTH.                                       CM2024.2
051900     SEND CM-OUTQUE-1 FROM SEG-INIT WITH END-FLAG.                CM2024.2
052000     MOVE 1 TO END-FLAG.                                          CM2024.2
052100     MOVE 10 TO OUT-LENGTH.                                       CM2024.2
052200     SEND CM-OUTQUE-1 FROM SEG-CONT WITH END-FLAG.                CM2024.2
052300     MOVE 2 TO END-FLAG.                                          CM2024.2
052400     MOVE 14 TO OUT-LENGTH.                                       CM2024.2
052500     SEND CM-OUTQUE-1 FROM MSG-COMP WITH END-FLAG.                CM2024.2
052600     MOVE 3 TO END-FLAG.                                          CM2024.2
052700     MOVE 16 TO OUT-LENGTH.                                       CM2024.2
052800     SEND CM-OUTQUE-1 FROM GROUP-COMP WITH END-FLAG.              CM2024.2
052900 SEGMENTED-MSG-TEST-05.                                           CM2024.2
053000     MOVE 71 TO OUT-LENGTH.                                       CM2024.2
053100     MOVE 0 TO END-FLAG.                                          CM2024.2
053200     SEND CM-OUTQUE-1 FROM SKIP-MSG-1 WITH END-FLAG               CM2024.2
053300         BEFORE ADVANCING 4 LINES.                                CM2024.2
053400     MOVE 17 TO OUT-LENGTH.                                       CM2024.2
053500     MOVE 3 TO END-FLAG.                                          CM2024.2
053600     SEND CM-OUTQUE-1 FROM SKIP-MSG-2 WITH END-FLAG.              CM2024.2
053700 SINGLE-TERMINAL-TEST-01.                                         CM2024.2
053800     MOVE 1 TO DEST-COUNT.                                        CM2024.2
053900     MOVE 57 TO OUT-LENGTH.                                       CM2024.2
054000     SEND CM-OUTQUE-1 FROM ONE-TERMINAL-MSG WITH EGI.             CM2024.2
054100     MOVE 2 TO DEST-COUNT.                                        CM2024.2
054200 INCOMPLETE-MSG-TEST-01.                                          CM2024.2
054300     MOVE 55 TO OUT-LENGTH.                                       CM2024.2
054400     MOVE 1 TO INC-MSG-NO.                                        CM2024.2
054500     SEND CM-OUTQUE-1 FROM INCOMP-MSG.                            CM2024.2
054600 INCOMPLETE-MSG-TEST-02.                                          CM2024.2
054700     MOVE 55 TO OUT-LENGTH.                                       CM2024.2
054800     MOVE 2 TO INC-MSG-NO.                                        CM2024.2
054900     SEND CM-OUTQUE-1 FROM INCOMP-MSG WITH ESI.                   CM2024.2
055000 INCOMPLETE-MSG-TEST-03.                                          CM2024.2
055100     MOVE 0 TO END-FLAG.                                          CM2024.2
055200     MOVE 55 TO OUT-LENGTH.                                       CM2024.2
055300     MOVE 3 TO INC-MSG-NO.                                        CM2024.2
055400     SEND CM-OUTQUE-1 FROM INCOMP-MSG WITH END-FLAG.              CM2024.2
055500 INCOMPLETE-MSG-TEST-04.                                          CM2024.2
055600     MOVE 1 TO END-FLAG.                                          CM2024.2
055700     MOVE 55 TO OUT-LENGTH.                                       CM2024.2
055800     MOVE 4 TO INC-MSG-NO.                                        CM2024.2
055900     SEND CM-OUTQUE-1 FROM INCOMP-MSG WITH END-FLAG.              CM2024.2
056000 STOP-WITHOUT-COMPLETING-MSG.                                     CM2024.2
056100     PERFORM END-ROUTINE THRU END-ROUTINE-4.                      CM2024.2
056200     CLOSE PRINT-FILE.                                            CM2024.2
056300     STOP RUN.                                                    CM2024.2
056400 DELAY-FOR-30 SECTION.                                            CM2024.2
056500 TAKE-INIT-TIME.                                                  CM2024.2
056600     ACCEPT INIT-TIME FROM TIME.                                  CM2024.2
056700 TEST-ELAPSED-TIME.                                               CM2024.2
056800     ACCEPT TEST-TIME FROM TIME.                                  CM2024.2
056900     COMPUTE ELAPSED-TIME =                                       CM2024.2
057000         (T-HRS * 3600 + T-MINS * 60 + T-SECS) -                  CM2024.2
057100         (I-HRS * 3600 + I-MINS * 60 + I-SECS).                   CM2024.2
057200     IF ELAPSED-TIME IS LESS THAN 30 GO TO TEST-ELAPSED-TIME.     CM2024.2
057300 COMMON-SUBROUTINES SECTION.                                      CM2024.2
057400 PASS.                                                            CM2024.2
057500     MOVE "PASS" TO P-OR-F.                                       CM2024.2
057600 FAIL.                                                            CM2024.2
057700     ADD      1 TO ERROR-COUNTER.                                 CM2024.2
057800     MOVE "FAIL*" TO P-OR-F.                                      CM2024.2
057900 DE-LETE.                                                         CM2024.2
058000     MOVE     SPACE TO P-OR-F.                                    CM2024.2
058100     MOVE     "    ************    " TO COMPUTED-A.               CM2024.2
058200     MOVE     "    ************    " TO CORRECT-A.                CM2024.2
058300     MOVE "****TEST DELETED****" TO RE-MARK.                      CM2024.2
058400     ADD 1 TO DELETE-CNT.                                         CM2024.2
058500 PRINT-DETAIL.                                                    CM2024.2
058600     MOVE     TEST-RESULTS TO PRINT-REC.                          CM2024.2
058700     PERFORM WRITE-LINE.                                          CM2024.2
058800     MOVE     SPACE TO P-OR-F.                                    CM2024.2
058900     MOVE     SPACE TO COMPUTED-A.                                CM2024.2
059000     MOVE SPACE TO CORRECT-A.                                     CM2024.2
059100     MOVE SPACE TO RE-MARK.                                       CM2024.2
059200     MOVE SPACE TO FEATURE.                                       CM2024.2
059300 COLUMN-NAMES-ROUTINE.                                            CM2024.2
059400     MOVE     COLUMNS-LINE-1 TO DUMMY-RECORD.                     CM2024.2
059500     PERFORM WRITE-LINE.                                          CM2024.2
059600     MOVE     COLUMNS-LINE-2 TO DUMMY-RECORD.                     CM2024.2
059700     PERFORM WRITE-LINE.                                          CM2024.2
059800     PERFORM  BLANK-LINE-PRINT.                                   CM2024.2
059900 END-ROUTINE.                                                     CM2024.2
060000     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        CM2024.2
060100     PERFORM WRITE-LINE.                                          CM2024.2
060200 PARA-Z.                                                          CM2024.2
060300     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           CM2024.2
060400     MOVE     CCVS-E-1 TO DUMMY-RECORD.                           CM2024.2
060500     PERFORM WRITE-LINE.                                          CM2024.2
060600 END-ROUTINE-1.                                                   CM2024.2
060700     PERFORM  BLANK-LINE-PRINT.                                   CM2024.2
060800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      CM2024.2
060900              GO TO END-ROUTINE-2.                                CM2024.2
061000     MOVE     ERROR-COUNTER TO ERROR-TOTAL.                       CM2024.2
061100     GO TO    END-ROUTINE-3.                                      CM2024.2
061200 END-ROUTINE-2.                                                   CM2024.2
061300     MOVE " NO" TO ERROR-TOTAL.                                   CM2024.2
061400 END-ROUTINE-3.                                                   CM2024.2
061500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           CM2024.2
061600     PERFORM WRITE-LINE.                                          CM2024.2
061700     IF DELETE-CNT IS EQUAL TO ZERO                               CM2024.2
061800         MOVE " NO" TO ERROR-TOTAL  ELSE                          CM2024.2
061900     MOVE DELETE-CNT TO ERROR-TOTAL.                              CM2024.2
062000     MOVE "TESTS DELETED     " TO ENDER-DESC.                     CM2024.2
062100     MOVE CCVS-E-2 TO DUMMY-RECORD.                               CM2024.2
062200     PERFORM WRITE-LINE.                                          CM2024.2
062300 END-ROUTINE-4.                                                   CM2024.2
062400     MOVE CCVS-E-3 TO DUMMY-RECORD.                               CM2024.2
062500     PERFORM WRITE-LINE.                                          CM2024.2
062600 BLANK-LINE-PRINT.                                                CM2024.2
062700     MOVE     SPACE TO DUMMY-RECORD.                              CM2024.2
062800     PERFORM WRITE-LINE.                                          CM2024.2
062900 WRITE-LINE.                                                      CM2024.2
063000     WRITE DUMMY-RECORD AFTER ADVANCING 1 LINE.                   CM2024.2
063100 HEAD-ROUTINE.                                                    CM2024.2
063200     MOVE CCVS-H-1 TO PRINT-REC                                   CM2024.2
063300     WRITE PRINT-REC                                              CM2024.2
063400         AFTER ADVANCING PAGE.                                    CM2024.2
063500     MOVE CCVS-H-2 TO PRINT-REC.                                  CM2024.2
063600     WRITE PRINT-REC                                              CM2024.2
063700         AFTER 2 LINES.                                           CM2024.2
063800     MOVE CCVS-H-3 TO PRINT-REC.                                  CM2024.2
063900     WRITE PRINT-REC                                              CM2024.2
064000         AFTER 5 LINES.                                           CM2024.2
064100     MOVE HYPHEN-LINE TO PRINT-REC.                               CM2024.2
064200     PERFORM WRITE-LINE.                                          CM2024.2
      *END-OF,CM202M                                                                  
