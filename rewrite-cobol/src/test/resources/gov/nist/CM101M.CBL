      *HEADER,COBOL,CM101M                                                            
000100 IDENTIFICATION DIVISION.                                         CM1014.2
000200 PROGRAM-ID.                                                      CM1014.2
000300     CM101M.                                                      CM1014.2
000400 AUTHOR.                                                          CM1014.2
000500     FEDERAL COMPILER TESTING CENTER.                             CM1014.2
000600* INSTALLATION.                                                    CM1014.2
000700*    GENERAL SERVICES ADMINISTRATION                              CM1014.2
000800**     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                CM1014.2
000900*     SOFTWARE DEVELOPMENT OFFICE.                                 CM1014.2
001000*     5203 LEESBURG PIKE  SUITE 1100                               CM1014.2
001100*     FALLS CHURCH VIRGINIA 22041.                                 CM1014.2
001200*                                                                  CM1014.2
001300*     PHONE   (703) 756-6153                                       CM1014.2
001400*                                                                  CM1014.2
001500*     " HIGH       ".                                              CM1014.2
001600 DATE-WRITTEN.                                                    CM1014.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           CM1014.2
001800     CREATION DATE     /    VALIDATION DATE                       CM1014.2
001900     "4.2 ".                                                      CM1014.2
002000 SECURITY.                                                        CM1014.2
002100     NONE.                                                        CM1014.2
002200 ENVIRONMENT DIVISION.                                            CM1014.2
002300 CONFIGURATION SECTION.                                           CM1014.2
002400 SOURCE-COMPUTER.                                                 CM1014.2
002500     XXXXX082.                                                    CM1014.2
002600 OBJECT-COMPUTER.                                                 CM1014.2
002700     XXXXX083.                                                    CM1014.2
002800 INPUT-OUTPUT SECTION.                                            CM1014.2
002900 FILE-CONTROL.                                                    CM1014.2
003000     SELECT PRINT-FILE ASSIGN TO                                  CM1014.2
003100     XXXXX055.                                                    CM1014.2
003200 DATA DIVISION.                                                   CM1014.2
003300 FILE SECTION.                                                    CM1014.2
003400 FD  PRINT-FILE                                                   CM1014.2
003500     LABEL RECORDS                                                CM1014.2
003600     XXXXX084                                                     CM1014.2
003700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       CM1014.2
003800 01  PRINT-REC PICTURE X(120).                                    CM1014.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 CM1014.2
004000 WORKING-STORAGE SECTION.                                         CM1014.2
004100 77  PASSWORD1 PIC X(10) VALUE                                    CM1014.2
004200     XXXXX031.                                                    CM1014.2
004300 77  DISABLE-STATUS PIC XX VALUE "99".                            CM1014.2
004400 77  POLL-COUNT PIC 9(8).                                         CM1014.2
004500 77  INIT-TIME PIC 9(8).                                          CM1014.2
004600 77  COMP-TIME PIC 9(8).                                          CM1014.2
004700 01  SYSTEM-TIME.                                                 CM1014.2
004800     02  SYS-HRS PIC 99.                                          CM1014.2
004900     02  SYS-MINS PIC 99.                                         CM1014.2
005000     02  SYS-SECS PIC 99V99.                                      CM1014.2
005100 01  MSG-TIME.                                                    CM1014.2
005200     02  HOURS PIC 99.                                            CM1014.2
005300     02  MINUTES PIC 99.                                          CM1014.2
005400     02  SECONDS PIC 99V99.                                       CM1014.2
005500 01  SPEC-LINE-1.                                                 CM1014.2
005600     02  FILLER PIC X(40) VALUE                                   CM1014.2
005700     " INITIAL ENABLE RETURNED STATUS CODE OF ".                  CM1014.2
005800     02  INIT-ENABLE-STATUS PIC XX.                               CM1014.2
005900 01  INCOMING-MSG.                                                CM1014.2
006000     02  KILL-FIELD PIC X(4).                                     CM1014.2
006100     02  FILLER PIC X(68).                                        CM1014.2
006200 01  LOG-HDR-1.                                                   CM1014.2
006300     02  FILLER PIC X(48) VALUE SPACES.                           CM1014.2
006400     02  FILLER PIC X(24) VALUE "LOG OF INCOMING MESSAGES".       CM1014.2
006500 01  LOG-HDR-2.                                                   CM1014.2
006600     02  FILLER PIC X VALUE SPACE.                                CM1014.2
006700     02  FILLER PIC X(19) VALUE "SYMBOLIC SOURCE IS ".            CM1014.2
006800     02  SYM-SOURCE PIC X(25).                                    CM1014.2
006900     02  FILLER PIC X(16) VALUE "MESSAGE DATE IS ".               CM1014.2
007000     02  MSG-DATE PIC 9(6) VALUE ZERO.                            CM1014.2
007100 01  LOG-HDR-3.                                                   CM1014.2
007200     02  FILLER PIC X VALUE SPACE.                                CM1014.2
007300     02  FILLER PIC X(12) VALUE "TIME  RECVD".                    CM1014.2
007400     02  FILLER PIC X(9) VALUE "LOG LAG".                         CM1014.2
007500     02  FILLER PIC X(7) VALUE "LENGTH".                          CM1014.2
007600     02  FILLER PIC X(4) VALUE "END".                             CM1014.2
007700     02  FILLER PIC X(3) VALUE "QD".                              CM1014.2
007800     02  FILLER PIC X(40) VALUE "POLL COUNT".                     CM1014.2
007900     02  FILLER PIC X(16) VALUE "MESSAGE CONTENTS".               CM1014.2
008000 01  LOG-HDR-4.                                                   CM1014.2
008100     02  FILLER PIC X VALUE SPACE.                                CM1014.2
008200     02  FILLER PIC X(11) VALUE ALL "-".                          CM1014.2
008300     02  FILLER PIC X VALUE SPACES.                               CM1014.2
008400     02  FILLER PIC X(7) VALUE ALL "-".                           CM1014.2
008500     02  FILLER PIC X(2) VALUE SPACES.                            CM1014.2
008600     02  FILLER PIC X(6) VALUE ALL "-".                           CM1014.2
008700     02  FILLER PIC X VALUE SPACE.                                CM1014.2
008800     02  FILLER PIC XXX VALUE "---".                              CM1014.2
008900     02  FILLER PIC X VALUE SPACES.                               CM1014.2
009000     02  FILLER PIC XX VALUE "--".                                CM1014.2
009100     02  FILLER PIC X VALUE SPACES.                               CM1014.2
009200     02  FILLER PIC X(10) VALUE ALL "-".                          CM1014.2
009300     02  FILLER PIC X(2) VALUE SPACES.                            CM1014.2
009400     02  FILLER PIC X(72) VALUE ALL "-".                          CM1014.2
009500 01  LOG-LINE.                                                    CM1014.2
009600     02  FILLER PIC X VALUE SPACE.                                CM1014.2
009700     02  RECEIPT-TIME.                                            CM1014.2
009800         03  HOURS PIC 99.                                        CM1014.2
009900         03  FILLER PIC X VALUE ":".                              CM1014.2
010000         03  MINUTES PIC 99.                                      CM1014.2
010100         03  FILLER PIC X VALUE ":".                              CM1014.2
010200         03  SECONDS PIC 99.99.                                   CM1014.2
010300     02  LAG-TIME PIC -(4)9.99.                                   CM1014.2
010400     02  FILLER PIC XX VALUE SPACES.                              CM1014.2
010500     02  MESSAGE-LENGTH PIC ZZZ9.                                 CM1014.2
010600     02  FILLER PIC XXX VALUE SPACES.                             CM1014.2
010700     02  SENTINEL PIC XXX.                                        CM1014.2
010800     02  FILLER PIC X VALUE SPACES.                               CM1014.2
010900     02  QUEUE-DEPTH PIC 99.                                      CM1014.2
011000     02  FILLER PIC X VALUE SPACES.                               CM1014.2
011100     02  IDLE-COUNT PIC ZZ,ZZZ,ZZ9.                               CM1014.2
011200     02  IDLE-OVERFLOW REDEFINES IDLE-COUNT PIC X(10).            CM1014.2
011300     02  FILLER PIC XX VALUE SPACES.                              CM1014.2
011400     02  MSG PIC X(72).                                           CM1014.2
011500     66  LONG-NARRATIVE RENAMES LAG-TIME THRU MSG.                CM1014.2
011600     66  SHORT-NARRATIVE RENAMES IDLE-COUNT THRU MSG.             CM1014.2
011700 01  TEST-RESULTS.                                                CM1014.2
011800     02 FILLER                    PICTURE X VALUE SPACE.          CM1014.2
011900     02 FEATURE                   PICTURE X(18).                  CM1014.2
012000     02 FILLER                    PICTURE X VALUE SPACE.          CM1014.2
012100     02 P-OR-F                    PICTURE X(5).                   CM1014.2
012200     02 FILLER                    PICTURE X  VALUE SPACE.         CM1014.2
012300     02  PAR-NAME PIC X(20).                                      CM1014.2
012400     02 FILLER                    PICTURE X VALUE SPACE.          CM1014.2
012500     02  COMPUTED-A.                                              CM1014.2
012600         03  FILLER PIC X(9) VALUE SPACES.                        CM1014.2
012700         03  COMPUTED-STATUS PIC XX.                              CM1014.2
012800         03  FILLER PIC X(9) VALUE SPACES.                        CM1014.2
012900     02 FILLER                    PICTURE X VALUE SPACE.          CM1014.2
013000     02  CORRECT-A.                                               CM1014.2
013100         03  FILLER PIC X(9) VALUE SPACES.                        CM1014.2
013200         03  CORRECT-STATUS PIC XX.                               CM1014.2
013300         03  FILLER PIC X(9) VALUE SPACES.                        CM1014.2
013400     02 FILLER                    PICTURE X VALUE SPACE.          CM1014.2
013500     02 RE-MARK                   PICTURE X(30).                  CM1014.2
013600 01  COLUMNS-LINE-1.                                              CM1014.2
013700     02  FILLER PIC X(3) VALUE SPACES.                            CM1014.2
013800     02  FILLER PIC X(17) VALUE "FEATURE TESTED".                 CM1014.2
013900     02  FILLER PIC X(9) VALUE "RESLT".                           CM1014.2
014000     02  FILLER PIC X(21) VALUE "PARAGRAPH NAME".                 CM1014.2
014100     02  FILLER PIC X(22) VALUE "COMPUTED DATA".                  CM1014.2
014200     02  FILLER PIC X(29) VALUE "CORRECT DATA".                   CM1014.2
014300     02  FILLER PIC X(7) VALUE "REMARKS".                         CM1014.2
014400 01  COLUMNS-LINE-2.                                              CM1014.2
014500     02  FILLER PIC X VALUE SPACE.                                CM1014.2
014600     02  FILLER PIC X(18) VALUE ALL "-".                          CM1014.2
014700     02  FILLER PIC X VALUE SPACE.                                CM1014.2
014800     02  FILLER PIC X(5) VALUE ALL "-".                           CM1014.2
014900     02  FILLER PIC X VALUE SPACE.                                CM1014.2
015000     02  FILLER PIC X(20) VALUE ALL "-".                          CM1014.2
015100     02  FILLER PIC X VALUE SPACE.                                CM1014.2
015200     02  FILLER PIC X(20) VALUE ALL "-".                          CM1014.2
015300     02  FILLER PIC X VALUE SPACE.                                CM1014.2
015400     02  FILLER PIC X(20) VALUE ALL "-".                          CM1014.2
015500     02  FILLER PIC X VALUE SPACE.                                CM1014.2
015600     02  FILLER PIC X(31) VALUE ALL "-".                          CM1014.2
015700 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         CM1014.2
015800 01  REC-CT PICTURE 99 VALUE ZERO.                                CM1014.2
015900 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        CM1014.2
016000 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  CM1014.2
016100 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          CM1014.2
016200 01  PASS-COUNTER PIC 999 VALUE ZERO.                             CM1014.2
016300 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              CM1014.2
016400 01  ERROR-HOLD PIC 999 VALUE ZERO.                               CM1014.2
016500 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           CM1014.2
016600 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            CM1014.2
016700 01  CCVS-H-1.                                                    CM1014.2
016800     02  FILLER   PICTURE X(27)  VALUE SPACE.                     CM1014.2
016900     02 FILLER PICTURE X(67) VALUE                                CM1014.2
017000     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  CM1014.2
017100-    " SYSTEM".                                                   CM1014.2
017200     02  FILLER     PICTURE X(26)  VALUE SPACE.                   CM1014.2
017300 01  CCVS-H-2.                                                    CM1014.2
017400     02 FILLER PICTURE X(52) VALUE IS                             CM1014.2
017500     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   CM1014.2
017600     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   CM1014.2
017700     02 TEST-ID PICTURE IS X(9).                                  CM1014.2
017800     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   CM1014.2
017900 01  CCVS-H-3.                                                    CM1014.2
018000     02  FILLER PICTURE X(34) VALUE                               CM1014.2
018100     " FOR OFFICIAL USE ONLY    ".                                CM1014.2
018200     02  FILLER PICTURE X(58) VALUE                               CM1014.2
018300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".CM1014.2
018400     02  FILLER PICTURE X(28) VALUE                               CM1014.2
018500     "  COPYRIGHT   1974 ".                                       CM1014.2
018600 01  CCVS-E-1.                                                    CM1014.2
018700     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   CM1014.2
018800     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        CM1014.2
018900     02 ID-AGAIN PICTURE IS X(9).                                 CM1014.2
019000     02 FILLER PICTURE X(45) VALUE IS                             CM1014.2
019100     " NTIS DISTRIBUTION COBOL 74".                               CM1014.2
019200 01  CCVS-E-2.                                                    CM1014.2
019300     02  FILLER                   PICTURE X(31)  VALUE            CM1014.2
019400     SPACE.                                                       CM1014.2
019500     02  FILLER                   PICTURE X(21)  VALUE SPACE.     CM1014.2
019600     02 CCVS-E-2-2.                                               CM1014.2
019700         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            CM1014.2
019800         03 FILLER PICTURE IS X VALUE IS SPACE.                   CM1014.2
019900         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      CM1014.2
020000 01  CCVS-E-3.                                                    CM1014.2
020100     02  FILLER PICTURE X(22) VALUE                               CM1014.2
020200     " FOR OFFICIAL USE ONLY".                                    CM1014.2
020300     02  FILLER PICTURE X(12) VALUE SPACE.                        CM1014.2
020400     02  FILLER PICTURE X(58) VALUE                               CM1014.2
020500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".CM1014.2
020600     02  FILLER PICTURE X(13) VALUE SPACE.                        CM1014.2
020700     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 CM1014.2
020800 01  CCVS-E-4.                                                    CM1014.2
020900     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           CM1014.2
021000     02 FILLER PIC XXXX VALUE " OF ".                             CM1014.2
021100     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           CM1014.2
021200     02 FILLER PIC X(40) VALUE                                    CM1014.2
021300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       CM1014.2
021400 01  XXINFO.                                                      CM1014.2
021500     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    CM1014.2
021600     02 INFO-TEXT.                                                CM1014.2
021700     04 FILLER PIC X(20) VALUE SPACE.                             CM1014.2
021800     04 XXCOMPUTED PIC X(20).                                     CM1014.2
021900     04 FILLER PIC X(5) VALUE SPACE.                              CM1014.2
022000     04 XXCORRECT PIC X(20).                                      CM1014.2
022100 01  HYPHEN-LINE.                                                 CM1014.2
022200     02 FILLER PICTURE IS X VALUE IS SPACE.                       CM1014.2
022300     02 FILLER PICTURE IS X(65) VALUE IS "************************CM1014.2
022400-    "*****************************************".                 CM1014.2
022500     02 FILLER PICTURE IS X(54) VALUE IS "************************CM1014.2
022600-    "******************************".                            CM1014.2
022700 01  CCVS-PGM-ID PIC X(6) VALUE                                   CM1014.2
022800     "CM101M".                                                    CM1014.2
022900 COMMUNICATION SECTION.                                           CM1014.2
023000 CD  CM-INQUE-1 FOR INPUT                                         CM1014.2
023100     SYMBOLIC QUEUE IS MAIN-QUEUE                                 CM1014.2
023200     SYMBOLIC SUB-QUEUE-1 IS NO-SPEC-1                            CM1014.2
023300     SYMBOLIC SUB-QUEUE-2 IS NO-SPEC-2                            CM1014.2
023400     SYMBOLIC SUB-QUEUE-3 IS NO-SPEC-3                            CM1014.2
023500     MESSAGE DATE IS DATE-RECEIVED                                CM1014.2
023600     MESSAGE TIME IS TIME-RECEIVED                                CM1014.2
023700     SYMBOLIC SOURCE IS WHERE-FROM                                CM1014.2
023800     TEXT LENGTH IS MSG-LENGTH                                    CM1014.2
023900     END KEY IS END-KEY                                           CM1014.2
024000     STATUS KEY IS STATUS-KEY                                     CM1014.2
024100     MESSAGE COUNT IS MSG-COUNT.                                  CM1014.2
024200 PROCEDURE    DIVISION.                                           CM1014.2
024300 SECT-CM101M-0001 SECTION.                                        CM1014.2
024400 CM101M-INIT.                                                     CM1014.2
024500     OPEN     OUTPUT PRINT-FILE.                                  CM1014.2
024600     MOVE "CM101M     " TO TEST-ID.                               CM1014.2
024700     MOVE     TEST-ID TO ID-AGAIN.                                CM1014.2
024800     MOVE    SPACE TO TEST-RESULTS.                               CM1014.2
024900     PERFORM HEAD-ROUTINE.                                        CM1014.2
025000     MOVE                                                         CM1014.2
025100     XXXXX030                                                     CM1014.2
025200     TO MAIN-QUEUE.                                               CM1014.2
025300     MOVE SPACES TO NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.                CM1014.2
025400     ENABLE INPUT  CM-INQUE-1 WITH KEY                            CM1014.2
025500     XXXXX031.                                                    CM1014.2
025600     MOVE STATUS-KEY TO INIT-ENABLE-STATUS.                       CM1014.2
025700     MOVE SPEC-LINE-1 TO PRINT-REC.                               CM1014.2
025800     WRITE PRINT-REC                                              CM1014.2
025900         AFTER 2 LINES.                                           CM1014.2
026000     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1014.2
026100     WRITE PRINT-REC                                              CM1014.2
026200         AFTER 2 LINES.                                           CM1014.2
026300 LOG-INIT.                                                        CM1014.2
026400     MOVE ZERO TO POLL-COUNT.                                     CM1014.2
026500     MOVE ALL "*" TO MSG.                                         CM1014.2
026600 LOG-MSG.                                                         CM1014.2
026700     MOVE   SPACES TO INCOMING-MSG.                               CM1014.2
026800     RECEIVE CM-INQUE-1 MESSAGE INTO INCOMING-MSG                 CM1014.2
026900         NO DATA PERFORM INCREMENT-POLL-COUNT GO TO LOG-MSG.      CM1014.2
027000     ACCEPT SYSTEM-TIME FROM TIME.                                CM1014.2
027100     ACCEPT CM-INQUE-1 MESSAGE COUNT.                             CM1014.2
027200     IF STATUS-KEY IS NOT EQUAL TO ZERO                           CM1014.2
027300         DISPLAY "RUN ABORTED - STATUS KEY WAS " STATUS-KEY       CM1014.2
027400         STOP RUN.                                                CM1014.2
027500     IF MSG-DATE IS EQUAL TO ZERO PERFORM LOG-HEADER.             CM1014.2
027600     IF KILL-FIELD IS EQUAL TO "KILL"                             CM1014.2
027700         ACCEPT INIT-TIME FROM TIME                               CM1014.2
027800         DISABLE INPUT CM-INQUE-1 WITH KEY                        CM1014.2
027900     XXXXX031                                                     CM1014.2
028000         ACCEPT COMP-TIME FROM TIME                               CM1014.2
028100         MOVE STATUS-KEY TO DISABLE-STATUS.                       CM1014.2
028200     MOVE TIME-RECEIVED TO MSG-TIME.                              CM1014.2
028300     MOVE CORR MSG-TIME TO RECEIPT-TIME.                          CM1014.2
028400     COMPUTE LAG-TIME =                                           CM1014.2
028500         ((SYS-HRS * 3600) + (SYS-MINS * 60) + SYS-SECS) -        CM1014.2
028600         ((HOURS OF MSG-TIME * 3600) + (MINUTES OF MSG-TIME * 60) CM1014.2
028700         + SECONDS OF MSG-TIME).                                  CM1014.2
028800     IF END-KEY IS EQUAL TO "3"                                   CM1014.2
028900         MOVE "EGI" TO SENTINEL                                   CM1014.2
029000         ELSE IF END-KEY IS EQUAL TO "2"                          CM1014.2
029100         MOVE "EMI" TO SENTINEL                                   CM1014.2
029200             ELSE MOVE END-KEY TO SENTINEL.                       CM1014.2
029300     MOVE MSG-COUNT TO QUEUE-DEPTH.                               CM1014.2
029400     MOVE MSG-LENGTH TO MESSAGE-LENGTH.                           CM1014.2
029500     IF POLL-COUNT IS EQUAL TO 99999999                           CM1014.2
029600         MOVE " OVERFLOW " TO IDLE-OVERFLOW                       CM1014.2
029700         ELSE MOVE POLL-COUNT TO IDLE-COUNT.                      CM1014.2
029800     MOVE INCOMING-MSG TO MSG.                                    CM1014.2
029900     MOVE LOG-LINE TO PRINT-REC.                                  CM1014.2
030000     PERFORM WRITE-LINE.                                          CM1014.2
030100 LOG-MSG-01.                                                      CM1014.2
030200     IF KILL-FIELD IS EQUAL TO "WAIT"                             CM1014.2
030300         PERFORM GET-INITIAL-TIME                                 CM1014.2
030400         PERFORM DELAY-FOR-30-SECS                                CM1014.2
030500         GO TO LOG-INIT.                                          CM1014.2
030600     IF KILL-FIELD IS NOT EQUAL TO "KILL" GO TO LOG-INIT.         CM1014.2
030700 DISABLE-CM-INQUE-1.                                              CM1014.2
030800     MOVE INIT-TIME TO MSG-TIME.                                  CM1014.2
030900     MOVE CORR MSG-TIME TO RECEIPT-TIME.                          CM1014.2
031000     MOVE "-DISABLE COMMAND INITIATED FROM PROGRAM"               CM1014.2
031100         TO LONG-NARRATIVE.                                       CM1014.2
031200     MOVE LOG-LINE TO PRINT-REC.                                  CM1014.2
031300     WRITE PRINT-REC                                              CM1014.2
031400         AFTER 2 LINES.                                           CM1014.2
031500     MOVE COMP-TIME TO MSG-TIME.                                  CM1014.2
031600     MOVE CORR MSG-TIME TO RECEIPT-TIME.                          CM1014.2
031700     MOVE "- STATUS CODE OF" TO LONG-NARRATIVE.                   CM1014.2
031800     MOVE DISABLE-STATUS TO QUEUE-DEPTH.                          CM1014.2
031900     MOVE "AND EXECUTION CONTROL RETURNED FROM MCS"               CM1014.2
032000         TO SHORT-NARRATIVE.                                      CM1014.2
032100     MOVE LOG-LINE TO PRINT-REC.                                  CM1014.2
032200     PERFORM WRITE-LINE.                                          CM1014.2
032300     MOVE SPACES TO PRINT-REC.                                    CM1014.2
032400     PERFORM WRITE-LINE.                                          CM1014.2
032500 GET-INITIAL-TIME.                                                CM1014.2
032600     ACCEPT SYSTEM-TIME FROM TIME.                                CM1014.2
032700     COMPUTE INIT-TIME =                                          CM1014.2
032800         SYS-HRS * 3600 + SYS-MINS * 60 + SYS-SECS.               CM1014.2
032900 LOOK-FOR-LATE-TRANSMISSIONS.                                     CM1014.2
033000     ACCEPT CM-INQUE-1 MESSAGE COUNT.                             CM1014.2
033100     IF MSG-COUNT IS NOT EQUAL TO ZERO                            CM1014.2
033200         PERFORM LOG-INIT THRU LOG-MSG                            CM1014.2
033300         GO TO LOOK-FOR-LATE-TRANSMISSIONS.                       CM1014.2
033400 GET-TIME-DIFFERENCE.                                             CM1014.2
033500     ACCEPT SYSTEM-TIME FROM TIME.                                CM1014.2
033600     COMPUTE COMP-TIME =                                          CM1014.2
033700         SYS-HRS * 3600 + SYS-MINS * 60 + SYS-SECS - INIT-TIME.   CM1014.2
033800 CHECK-FOR-15.                                                    CM1014.2
033900     IF COMP-TIME IS LESS THAN 15                                 CM1014.2
034000         GO TO LOOK-FOR-LATE-TRANSMISSIONS.                       CM1014.2
034100 15-SECONDS-HAVE-ELAPSED.                                         CM1014.2
034200     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1014.2
034300     WRITE PRINT-REC                                              CM1014.2
034400         AFTER 2 LINES.                                           CM1014.2
034500     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1014.2
034600     PERFORM WRITE-LINE.                                          CM1014.2
034700 STATUS-TESTS-INIT.                                               CM1014.2
034800     MOVE " BEGIN INPUT STATUS TESTS" TO PRINT-REC.               CM1014.2
034900     WRITE PRINT-REC                                              CM1014.2
035000         AFTER 2 LINES.                                           CM1014.2
035100     MOVE COLUMNS-LINE-1 TO PRINT-REC.                            CM1014.2
035200     WRITE PRINT-REC                                              CM1014.2
035300         AFTER 2 LINES.                                           CM1014.2
035400     MOVE COLUMNS-LINE-2 TO PRINT-REC.                            CM1014.2
035500     PERFORM WRITE-LINE.                                          CM1014.2
035600     MOVE SPACES TO PRINT-REC.                                    CM1014.2
035700     PERFORM WRITE-LINE.                                          CM1014.2
035800     MOVE "MCS STATUS WORD" TO FEATURE.                           CM1014.2
035900 REC-STATUS-TEST-01.                                              CM1014.2
036000     MOVE "QUEUE NAME NOT SPECIFIED" TO RE-MARK.                  CM1014.2
036100     MOVE "99" TO STATUS-KEY.                                     CM1014.2
036200     MOVE SPACES TO MAIN-QUEUE NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.     CM1014.2
036300     RECEIVE CM-INQUE-1 MESSAGE INTO INCOMING-MSG                 CM1014.2
036400         NO DATA ADD 0 TO POLL-COUNT.                             CM1014.2
036500     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
036600         PERFORM PASS GO TO REC-STATUS-WRITE-01.                  CM1014.2
036700     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
036800     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
036900     PERFORM FAIL.                                                CM1014.2
037000     GO TO REC-STATUS-WRITE-01.                                   CM1014.2
037100 REC-STATUS-DELETE-01.                                            CM1014.2
037200     PERFORM DE-LETE.                                             CM1014.2
037300 REC-STATUS-WRITE-01.                                             CM1014.2
037400     MOVE "REC-STATUS-TEST-01" TO PAR-NAME.                       CM1014.2
037500     PERFORM PRINT-DETAIL.                                        CM1014.2
037600 REC-STATUS-TEST-02.                                              CM1014.2
037700     MOVE "UNKNOWN SUB-QUEUE-1 SPECIFIED" TO RE-MARK.             CM1014.2
037800     MOVE "99" TO STATUS-KEY.                                     CM1014.2
037900     MOVE                                                         CM1014.2
038000     XXXXX030                                                     CM1014.2
038100         TO MAIN-QUEUE.                                           CM1014.2
038200     MOVE "DUMMYNAME" TO NO-SPEC-1.                               CM1014.2
038300     MOVE SPACES TO NO-SPEC-2 NO-SPEC-3.                          CM1014.2
038400     RECEIVE CM-INQUE-1 MESSAGE INTO INCOMING-MSG                 CM1014.2
038500         NO DATA ADD 0 TO POLL-COUNT.                             CM1014.2
038600     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
038700         PERFORM PASS GO TO REC-STATUS-WRITE-02.                  CM1014.2
038800     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
038900     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
039000     PERFORM FAIL.                                                CM1014.2
039100     GO TO REC-STATUS-WRITE-02.                                   CM1014.2
039200 REC-STATUS-DELETE-02.                                            CM1014.2
039300     PERFORM DE-LETE.                                             CM1014.2
039400 REC-STATUS-WRITE-02.                                             CM1014.2
039500     MOVE "REC-STATUS-TEST-02" TO PAR-NAME.                       CM1014.2
039600     PERFORM PRINT-DETAIL.                                        CM1014.2
039700 ACCPT-STATUS-TEST-01.                                            CM1014.2
039800     MOVE "QUEUE NAME NOT SPECIFIED" TO RE-MARK.                  CM1014.2
039900     MOVE "99" TO STATUS-KEY.                                     CM1014.2
040000     MOVE SPACES TO MAIN-QUEUE NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.     CM1014.2
040100     ACCEPT CM-INQUE-1 MESSAGE COUNT.                             CM1014.2
040200     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
040300         PERFORM PASS GO TO ACCPT-STATUS-WRITE-01.                CM1014.2
040400     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
040500     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
040600     PERFORM FAIL.                                                CM1014.2
040700     GO TO ACCPT-STATUS-WRITE-01.                                 CM1014.2
040800 ACCPT-STATUS-DELETE-01.                                          CM1014.2
040900     PERFORM DE-LETE.                                             CM1014.2
041000 ACCPT-STATUS-WRITE-01.                                           CM1014.2
041100     MOVE "ACCPT-STATUS-TEST-01" TO PAR-NAME.                     CM1014.2
041200     PERFORM PRINT-DETAIL.                                        CM1014.2
041300 ACCPT-STATUS-TEST-02.                                            CM1014.2
041400     MOVE "UNKNOWN SUB-QUEUE-1 SPECIFIED" TO RE-MARK.             CM1014.2
041500     MOVE "99" TO STATUS-KEY.                                     CM1014.2
041600     MOVE                                                         CM1014.2
041700     XXXXX030                                                     CM1014.2
041800         TO MAIN-QUEUE.                                           CM1014.2
041900     MOVE "DUMMYNAME" TO NO-SPEC-1.                               CM1014.2
042000     MOVE SPACES TO NO-SPEC-2 NO-SPEC-3.                          CM1014.2
042100     ACCEPT CM-INQUE-1 COUNT.                                     CM1014.2
042200     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
042300     PERFORM PASS GO TO ACCPT-STATUS-WRITE-02.                    CM1014.2
042400     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
042500     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
042600     PERFORM FAIL.                                                CM1014.2
042700      GO TO ACCPT-STATUS-WRITE-02.                                CM1014.2
042800 ACCPT-STATUS-DELETE-02.                                          CM1014.2
042900     PERFORM DE-LETE.                                             CM1014.2
043000 ACCPT-STATUS-WRITE-02.                                           CM1014.2
043100     MOVE "ACCPT-STATUS-TEST-02" TO PAR-NAME.                     CM1014.2
043200     PERFORM PRINT-DETAIL.                                        CM1014.2
043300 ENABL-STATUS-TEST-01.                                            CM1014.2
043400     MOVE "QUEUE NAME NOT SPECIFIED" TO RE-MARK.                  CM1014.2
043500     MOVE "99" TO STATUS-KEY.                                     CM1014.2
043600     MOVE SPACES TO MAIN-QUEUE NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.     CM1014.2
043700     ENABLE INPUT CM-INQUE-1 WITH KEY                             CM1014.2
043800     XXXXX031                                                     CM1014.2
043900     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
044000         PERFORM PASS GO TO ENABL-STATUS-WRITE-01.                CM1014.2
044100     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
044200     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
044300     PERFORM FAIL.                                                CM1014.2
044400     GO TO ENABL-STATUS-WRITE-01.                                 CM1014.2
044500 ENABL-STATUS-DELETE-01.                                          CM1014.2
044600     PERFORM DE-LETE.                                             CM1014.2
044700 ENABL-STATUS-WRITE-01.                                           CM1014.2
044800     MOVE "ENABL-STATUS-TEST-01" TO PAR-NAME.                     CM1014.2
044900     PERFORM PRINT-DETAIL.                                        CM1014.2
045000 ENABL-STATUS-TEST-02.                                            CM1014.2
045100     MOVE "UNKNOWN SUB-QUEUE-1 SPECIFIED" TO RE-MARK.             CM1014.2
045200     MOVE "99" TO STATUS-KEY.                                     CM1014.2
045300     MOVE                                                         CM1014.2
045400     XXXXX030                                                     CM1014.2
045500         TO MAIN-QUEUE.                                           CM1014.2
045600     MOVE "DUMMYNAME" TO NO-SPEC-1.                               CM1014.2
045700     MOVE SPACES TO NO-SPEC-2 NO-SPEC-3.                          CM1014.2
045800     ENABLE INPUT CM-INQUE-1 KEY                                  CM1014.2
045900         PASSWORD1.                                               CM1014.2
046000     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
046100         PERFORM PASS GO TO ENABL-STATUS-WRITE-02.                CM1014.2
046200     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
046300     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
046400     PERFORM FAIL.                                                CM1014.2
046500     GO TO ENABL-STATUS-WRITE-02.                                 CM1014.2
046600 ENABL-STATUS-DELETE-02.                                          CM1014.2
046700     PERFORM DE-LETE.                                             CM1014.2
046800 ENABL-STATUS-WRITE-02.                                           CM1014.2
046900     MOVE "ENABL-STATUS-TEST-02" TO PAR-NAME.                     CM1014.2
047000     PERFORM PRINT-DETAIL.                                        CM1014.2
047100 ENABL-STATUS-TEST-03.                                            CM1014.2
047200     MOVE "INVALID PASSWORD USED" TO RE-MARK.                     CM1014.2
047300     MOVE "99" TO STATUS-KEY.                                     CM1014.2
047400     MOVE                                                         CM1014.2
047500     XXXXX030                                                     CM1014.2
047600         TO MAIN-QUEUE.                                           CM1014.2
047700     MOVE SPACES TO NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.                CM1014.2
047800     ENABLE INPUT CM-INQUE-1 WITH KEY "LETMEIN".                  CM1014.2
047900     IF STATUS-KEY IS EQUAL TO "40"                               CM1014.2
048000         PERFORM PASS GO TO ENABL-STATUS-WRITE-03.                CM1014.2
048100     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
048200     MOVE "40" TO CORRECT-STATUS.                                 CM1014.2
048300     PERFORM FAIL.                                                CM1014.2
048400     GO TO ENABL-STATUS-WRITE-03.                                 CM1014.2
048500 ENABL-STATUS-DELETE-03.                                          CM1014.2
048600     PERFORM DE-LETE.                                             CM1014.2
048700 ENABL-STATUS-WRITE-03.                                           CM1014.2
048800     MOVE "ENABL-STATUS-TEST-03" TO PAR-NAME.                     CM1014.2
048900     PERFORM PRINT-DETAIL.                                        CM1014.2
049000 ENABL-STATUS-TEST-04.                                            CM1014.2
049100     MOVE "NO QUEUE NAME / WRONG PASSWORD" TO RE-MARK.            CM1014.2
049200     MOVE  "99" TO STATUS-KEY.                                    CM1014.2
049300     MOVE SPACES TO MAIN-QUEUE NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.     CM1014.2
049400     ENABLE INPUT CM-INQUE-1 WITH KEY                             CM1014.2
049500         "LETMEIN".                                               CM1014.2
049600     MOVE "INFO" TO P-OR-F.                                       CM1014.2
049700     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
049800     MOVE "  INFO TEST FOR BOTH" TO CORRECT-A.                    CM1014.2
049900     GO TO ENABL-STATUS-WRITE-04.                                 CM1014.2
050000 ENABL-STATUS-DELETE-04.                                          CM1014.2
050100     PERFORM DE-LETE.                                             CM1014.2
050200 ENABL-STATUS-WRITE-04.                                           CM1014.2
050300     MOVE "ENABL-STATUS-TEST-04" TO PAR-NAME.                     CM1014.2
050400     PERFORM PRINT-DETAIL.                                        CM1014.2
050500 DISAB-STATUS-TEST-01.                                            CM1014.2
050600     MOVE "QUEUE NAME NOT SPECIFIED" TO RE-MARK.                  CM1014.2
050700     MOVE "99" TO STATUS-KEY.                                     CM1014.2
050800     MOVE SPACES TO MAIN-QUEUE NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.     CM1014.2
050900     DISABLE INPUT CM-INQUE-1 WITH KEY                            CM1014.2
051000     XXXXX031                                                     CM1014.2
051100     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
051200         PERFORM PASS GO TO DISAB-STATUS-WRITE-01.                CM1014.2
051300     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
051400     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
051500     PERFORM FAIL.                                                CM1014.2
051600     GO TO DISAB-STATUS-WRITE-01.                                 CM1014.2
051700 DISAB-STATUS-DELETE-01.                                          CM1014.2
051800     PERFORM DE-LETE.                                             CM1014.2
051900 DISAB-STATUS-WRITE-01.                                           CM1014.2
052000     MOVE "DISAB-STATUS-TEST-01" TO PAR-NAME.                     CM1014.2
052100     PERFORM PRINT-DETAIL.                                        CM1014.2
052200 DISAB-STATUS-TEST-02.                                            CM1014.2
052300     MOVE "UNKNOWN SUB-QUEUE-1 SPECIFIED" TO RE-MARK.             CM1014.2
052400     MOVE "99" TO STATUS-KEY.                                     CM1014.2
052500     MOVE                                                         CM1014.2
052600     XXXXX030                                                     CM1014.2
052700         TO MAIN-QUEUE.                                           CM1014.2
052800     MOVE "DUMMYNAME" TO NO-SPEC-1.                               CM1014.2
052900     MOVE SPACES TO NO-SPEC-2 NO-SPEC-3.                          CM1014.2
053000     DISABLE INPUT CM-INQUE-1 WITH KEY                            CM1014.2
053100     PASSWORD1.                                                   CM1014.2
053200     IF STATUS-KEY IS EQUAL TO "20"                               CM1014.2
053300         PERFORM PASS GO TO DISAB-STATUS-WRITE-02.                CM1014.2
053400     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
053500     MOVE "20" TO CORRECT-STATUS.                                 CM1014.2
053600     PERFORM FAIL.                                                CM1014.2
053700      GO TO DISAB-STATUS-WRITE-02.                                CM1014.2
053800 DISAB-STATUS-DELETE-02.                                          CM1014.2
053900     PERFORM DE-LETE.                                             CM1014.2
054000 DISAB-STATUS-WRITE-02.                                           CM1014.2
054100     MOVE "DISAB-STATUS-TEST-02" TO PAR-NAME.                     CM1014.2
054200     PERFORM PRINT-DETAIL.                                        CM1014.2
054300 DISAB-STATUS-TEST-03.                                            CM1014.2
054400     MOVE "INVALID PASSWORD USED" TO RE-MARK.                     CM1014.2
054500     MOVE "99" TO STATUS-KEY.                                     CM1014.2
054600     MOVE                                                         CM1014.2
054700     XXXXX030                                                     CM1014.2
054800         TO MAIN-QUEUE.                                           CM1014.2
054900     MOVE SPACES TO NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.                CM1014.2
055000     DISABLE INPUT CM-INQUE-1 WITH KEY                            CM1014.2
055100     "KILLITNOW".                                                 CM1014.2
055200     IF STATUS-KEY IS EQUAL TO "40"                               CM1014.2
055300         PERFORM PASS GO TO DISAB-STATUS-WRITE-03.                CM1014.2
055400     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1014.2
055500     MOVE "40" TO CORRECT-STATUS.                                 CM1014.2
055600     PERFORM FAIL.                                                CM1014.2
055700     GO TO DISAB-STATUS-WRITE-03.                                 CM1014.2
055800 DISAB-STATUS-DELETE-03.                                          CM1014.2
055900     PERFORM DE-LETE.                                             CM1014.2
056000 DISAB-STATUS-WRITE-03.                                           CM1014.2
056100     MOVE "DISAB-STATUS-TEST-03" TO PAR-NAME.                     CM1014.2
056200     PERFORM PRINT-DETAIL.                                        CM1014.2
056300 RENAB-STATUS-TEST-01.                                            CM1014.2
056400     MOVE "RE-ENABLE PREVIOUSLY DISABLED" TO RE-MARK.             CM1014.2
056500     MOVE "99" TO STATUS-KEY.                                     CM1014.2
056600     MOVE                                                         CM1014.2
056700     XXXXX030                                                     CM1014.2
056800         TO MAIN-QUEUE.                                           CM1014.2
056900     MOVE SPACES TO NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.                CM1014.2
057000     ENABLE INPUT CM-INQUE-1 KEY                                  CM1014.2
057100     PASSWORD1.                                                   CM1014.2
057200     IF STATUS-KEY IS EQUAL TO ZERO                               CM1014.2
057300         PERFORM PASS GO TO RENAB-STATUS-WRITE-01.                CM1014.2
057400     MOVE STATUS-KEY TO COMPUTED-STATUS                           CM1014.2
057500     MOVE ZERO TO CORRECT-STATUS.                                 CM1014.2
057600     PERFORM FAIL.                                                CM1014.2
057700     GO TO RENAB-STATUS-WRITE-01.                                 CM1014.2
057800 RENAB-STATUS-DELETE-01.                                          CM1014.2
057900     PERFORM DE-LETE.                                             CM1014.2
058000 RENAB-STATUS-WRITE-01.                                           CM1014.2
058100     MOVE "RENAB-STATUS-TEST-01" TO PAR-NAME.                     CM1014.2
058200     PERFORM PRINT-DETAIL.                                        CM1014.2
058300 CLOSE-FILES.                                                     CM1014.2
058400     PERFORM END-ROUTINE THRU END-ROUTINE-3.                      CM1014.2
058500     CLOSE    PRINT-FILE.                                         CM1014.2
058600     STOP     RUN.                                                CM1014.2
058700 PASS.                                                            CM1014.2
058800     MOVE "PASS" TO P-OR-F.                                       CM1014.2
058900 FAIL.                                                            CM1014.2
059000     ADD      1 TO ERROR-COUNTER.                                 CM1014.2
059100     MOVE "FAIL*" TO P-OR-F.                                      CM1014.2
059200 DE-LETE.                                                         CM1014.2
059300     MOVE     SPACE TO P-OR-F.                                    CM1014.2
059400     MOVE     "    ************    " TO COMPUTED-A.               CM1014.2
059500     MOVE     "    ************    " TO CORRECT-A.                CM1014.2
059600     MOVE "****TEST DELETED****" TO RE-MARK.                      CM1014.2
059700     ADD 1 TO DELETE-CNT.                                         CM1014.2
059800 PRINT-DETAIL.                                                    CM1014.2
059900     MOVE     TEST-RESULTS TO PRINT-REC.                          CM1014.2
060000     PERFORM WRITE-LINE.                                          CM1014.2
060100     MOVE     SPACE TO P-OR-F.                                    CM1014.2
060200     MOVE     SPACE TO COMPUTED-A.                                CM1014.2
060300     MOVE SPACE TO CORRECT-A.                                     CM1014.2
060400 END-ROUTINE.                                                     CM1014.2
060500     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        CM1014.2
060600     PERFORM WRITE-LINE.                                          CM1014.2
060700 PARA-Z.                                                          CM1014.2
060800     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           CM1014.2
060900     MOVE     CCVS-E-1 TO DUMMY-RECORD.                           CM1014.2
061000     PERFORM WRITE-LINE.                                          CM1014.2
061100 END-ROUTINE-1.                                                   CM1014.2
061200     PERFORM  BLANK-LINE-PRINT.                                   CM1014.2
061300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      CM1014.2
061400              GO TO END-ROUTINE-2.                                CM1014.2
061500     MOVE     ERROR-COUNTER TO ERROR-TOTAL.                       CM1014.2
061600     GO TO    END-ROUTINE-3.                                      CM1014.2
061700 END-ROUTINE-2.                                                   CM1014.2
061800     MOVE " NO" TO ERROR-TOTAL.                                   CM1014.2
061900 END-ROUTINE-3.                                                   CM1014.2
062000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           CM1014.2
062100     PERFORM WRITE-LINE.                                          CM1014.2
062200     IF DELETE-CNT IS EQUAL TO ZERO                               CM1014.2
062300         MOVE " NO" TO ERROR-TOTAL  ELSE                          CM1014.2
062400     MOVE DELETE-CNT TO ERROR-TOTAL.                              CM1014.2
062500     MOVE "TESTS DELETED     " TO ENDER-DESC.                     CM1014.2
062600     MOVE CCVS-E-2 TO DUMMY-RECORD.                               CM1014.2
062700     PERFORM WRITE-LINE.                                          CM1014.2
062800 END-ROUTINE-4.                                                   CM1014.2
062900     MOVE CCVS-E-3 TO DUMMY-RECORD.                               CM1014.2
063000     PERFORM WRITE-LINE.                                          CM1014.2
063100 BLANK-LINE-PRINT.                                                CM1014.2
063200     MOVE     SPACE TO DUMMY-RECORD.                              CM1014.2
063300     PERFORM WRITE-LINE.                                          CM1014.2
063400 WRITE-LINE.                                                      CM1014.2
063500     WRITE DUMMY-RECORD AFTER ADVANCING 1 LINE.                   CM1014.2
063600 INCREMENT-POLL-COUNT.                                            CM1014.2
063700     ADD 1 TO POLL-COUNT ON SIZE ERROR ADD 0 TO POLL-COUNT.       CM1014.2
063800 DELAY-FOR-30-SECS.                                               CM1014.2
063900     PERFORM GET-TIME-DIFFERENCE.                                 CM1014.2
064000     IF COMP-TIME IS LESS THAN 30                                 CM1014.2
064100         GO TO DELAY-FOR-30-SECS.                                 CM1014.2
064200 LOG-HEADER.                                                      CM1014.2
064300     MOVE LOG-HDR-1 TO PRINT-REC                                  CM1014.2
064400     WRITE PRINT-REC                                              CM1014.2
064500         AFTER 3 LINES.                                           CM1014.2
064600     MOVE DATE-RECEIVED TO MSG-DATE.                              CM1014.2
064700     MOVE WHERE-FROM TO SYM-SOURCE.                               CM1014.2
064800     MOVE LOG-HDR-2 TO PRINT-REC.                                 CM1014.2
064900     WRITE PRINT-REC                                              CM1014.2
065000         AFTER 3 LINES.                                           CM1014.2
065100     MOVE LOG-HDR-3 TO PRINT-REC.                                 CM1014.2
065200     WRITE PRINT-REC                                              CM1014.2
065300         AFTER 2 LINES.                                           CM1014.2
065400     MOVE LOG-HDR-4 TO PRINT-REC.                                 CM1014.2
065500     PERFORM WRITE-LINE.                                          CM1014.2
065600     MOVE SPACES TO PRINT-REC.                                    CM1014.2
065700     PERFORM WRITE-LINE.                                          CM1014.2
065800 HEAD-ROUTINE.                                                    CM1014.2
065900     MOVE CCVS-H-1 TO PRINT-REC                                   CM1014.2
066000     WRITE PRINT-REC                                              CM1014.2
066100         AFTER ADVANCING PAGE.                                    CM1014.2
066200     MOVE CCVS-H-2 TO PRINT-REC.                                  CM1014.2
066300     WRITE PRINT-REC                                              CM1014.2
066400         AFTER 2 LINES.                                           CM1014.2
066500     MOVE CCVS-H-3 TO PRINT-REC.                                  CM1014.2
066600     WRITE PRINT-REC                                              CM1014.2
066700         AFTER 5 LINES.                                           CM1014.2
066800     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1014.2
066900     PERFORM WRITE-LINE.                                          CM1014.2
      *END-OF,CM101M                                                                  
