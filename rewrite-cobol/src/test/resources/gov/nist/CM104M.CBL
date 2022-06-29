      *HEADER,COBOL,CM104M                                                            
000100 IDENTIFICATION DIVISION.                                         CM1044.2
000200 PROGRAM-ID.                                                      CM1044.2
000300     CM104M.                                                      CM1044.2
000400 AUTHOR.                                                          CM1044.2
000500     FEDERAL COMPILER TESTING CENTER.                             CM1044.2
000600* INSTALLATION.                                                    CM1044.2
000700*     GENERAL SERVICES ADMINISTRATION                              CM1044.2
000800**     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                CM1044.2
000900*     SOFTWARE DEVELOPMENT OFFICE.                                 CM1044.2
001000*     5203 LEESBURG PIKE  SUITE 1100                               CM1044.2
001100*     FALLS CHURCH VIRGINIA 22041.                                 CM1044.2
001200*                                                                  CM1044.2
001300*     PHONE   (703) 756-6153                                       CM1044.2
001400*                                                                  CM1044.2
001500*     " HIGH       ".                                              CM1044.2
001600 DATE-WRITTEN.                                                    CM1044.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           CM1044.2
001800     CREATION DATE     /    VALIDATION DATE                       CM1044.2
001900     "4.2 ".                                                      CM1044.2
002000 SECURITY.                                                        CM1044.2
002100     NONE.                                                        CM1044.2
002200 ENVIRONMENT DIVISION.                                            CM1044.2
002300 CONFIGURATION SECTION.                                           CM1044.2
002400 SOURCE-COMPUTER.                                                 CM1044.2
002500     XXXXX082.                                                    CM1044.2
002600 OBJECT-COMPUTER.                                                 CM1044.2
002700     XXXXX083.                                                    CM1044.2
002800 INPUT-OUTPUT SECTION.                                            CM1044.2
002900 FILE-CONTROL.                                                    CM1044.2
003000     SELECT PRINT-FILE ASSIGN TO                                  CM1044.2
003100     XXXXX055.                                                    CM1044.2
003200 DATA DIVISION.                                                   CM1044.2
003300 FILE SECTION.                                                    CM1044.2
003400 FD  PRINT-FILE                                                   CM1044.2
003500     LABEL RECORDS                                                CM1044.2
003600     XXXXX084                                                     CM1044.2
003700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       CM1044.2
003800 01  PRINT-REC PICTURE X(120).                                    CM1044.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 CM1044.2
004000 WORKING-STORAGE SECTION.                                         CM1044.2
004100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         CM1044.2
004200 01  REC-CT PICTURE 99 VALUE ZERO.                                CM1044.2
004300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        CM1044.2
004400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  CM1044.2
004500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          CM1044.2
004600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             CM1044.2
004700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              CM1044.2
004800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               CM1044.2
004900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           CM1044.2
005000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            CM1044.2
005100 01  CCVS-H-1.                                                    CM1044.2
005200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     CM1044.2
005300     02 FILLER PICTURE X(67) VALUE                                CM1044.2
005400     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  CM1044.2
005500-    " SYSTEM".                                                   CM1044.2
005600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   CM1044.2
005700 01  CCVS-H-2.                                                    CM1044.2
005800     02 FILLER PICTURE X(52) VALUE IS                             CM1044.2
005900     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   CM1044.2
006000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   CM1044.2
006100     02 TEST-ID PICTURE IS X(9).                                  CM1044.2
006200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   CM1044.2
006300 01  CCVS-H-3.                                                    CM1044.2
006400     02  FILLER PICTURE X(34) VALUE                               CM1044.2
006500     " FOR OFFICIAL USE ONLY    ".                                CM1044.2
006600     02  FILLER PICTURE X(58) VALUE                               CM1044.2
006700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".CM1044.2
006800     02  FILLER PICTURE X(28) VALUE                               CM1044.2
006900     "  COPYRIGHT   1974 ".                                       CM1044.2
007000 01  CCVS-E-1.                                                    CM1044.2
007100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   CM1044.2
007200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        CM1044.2
007300     02 ID-AGAIN PICTURE IS X(9).                                 CM1044.2
007400     02 FILLER PICTURE X(45) VALUE IS                             CM1044.2
007500     " NTIS DISTRIBUTION COBOL 74".                               CM1044.2
007600 01  CCVS-E-2.                                                    CM1044.2
007700     02  FILLER                   PICTURE X(31)  VALUE            CM1044.2
007800     SPACE.                                                       CM1044.2
007900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     CM1044.2
008000     02 CCVS-E-2-2.                                               CM1044.2
008100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            CM1044.2
008200         03 FILLER PICTURE IS X VALUE IS SPACE.                   CM1044.2
008300         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      CM1044.2
008400 01  CCVS-E-3.                                                    CM1044.2
008500     02  FILLER PICTURE X(22) VALUE                               CM1044.2
008600     " FOR OFFICIAL USE ONLY".                                    CM1044.2
008700     02  FILLER PICTURE X(12) VALUE SPACE.                        CM1044.2
008800     02  FILLER PICTURE X(58) VALUE                               CM1044.2
008900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".CM1044.2
009000     02  FILLER PICTURE X(13) VALUE SPACE.                        CM1044.2
009100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 CM1044.2
009200 01  CCVS-E-4.                                                    CM1044.2
009300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           CM1044.2
009400     02 FILLER PIC XXXX VALUE " OF ".                             CM1044.2
009500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           CM1044.2
009600     02 FILLER PIC X(40) VALUE                                    CM1044.2
009700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       CM1044.2
009800 01  XXINFO.                                                      CM1044.2
009900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    CM1044.2
010000     02 INFO-TEXT.                                                CM1044.2
010100     04 FILLER PIC X(20) VALUE SPACE.                             CM1044.2
010200     04 XXCOMPUTED PIC X(20).                                     CM1044.2
010300     04 FILLER PIC X(5) VALUE SPACE.                              CM1044.2
010400     04 XXCORRECT PIC X(20).                                      CM1044.2
010500 01  HYPHEN-LINE.                                                 CM1044.2
010600     02 FILLER PICTURE IS X VALUE IS SPACE.                       CM1044.2
010700     02 FILLER PICTURE IS X(65) VALUE IS "************************CM1044.2
010800-    "*****************************************".                 CM1044.2
010900     02 FILLER PICTURE IS X(54) VALUE IS "************************CM1044.2
011000-    "******************************".                            CM1044.2
011100 01  CCVS-PGM-ID PIC X(6) VALUE                                   CM1044.2
011200     "CM104M".                                                    CM1044.2
011300 01  LOG-HDR-1.                                                   CM1044.2
011400     02  FILLER PIC X(54) VALUE SPACES.                           CM1044.2
011500     02  FILLER PIC X(11) VALUE "MESSAGE LOG".                    CM1044.2
011600 01  LOG-HDR-2.                                                   CM1044.2
011700     02  FILLER PIC XXX VALUE SPACES.                             CM1044.2
011800     02  FILLER PIC X(12) VALUE "SYMBOLIC".                       CM1044.2
011900     02  FILLER PIC X(15) VALUE "TIME MCS".                       CM1044.2
012000     02  FILLER PIC X(6) VALUE "SEND".                            CM1044.2
012100     02  FILLER PIC X(4) VALUE "MSG".                             CM1044.2
012200     02  FILLER PIC XXX VALUE "IN".                               CM1044.2
012300     02  FILLER PIC X(3) VALUE "OUT".                             CM1044.2
012400 01  LOG-HDR-3.                                                   CM1044.2
012500     02  FILLER PIC X(4) VALUE SPACES.                            CM1044.2
012600     02  FILLER PIC X(11) VALUE "SOURCE".                         CM1044.2
012700     02  FILLER PIC X(11) VALUE "RECEIVED".                       CM1044.2
012800     02  FILLER PIC XXX VALUE "QD".                               CM1044.2
012900     02  FILLER PIC X(7) VALUE "COMPLT".                          CM1044.2
013000     02  FILLER PIC X(4) VALUE "LTH".                             CM1044.2
013100     02  FILLER PIC XXX VALUE "ST".                               CM1044.2
013200     02  FILLER PIC X(33) VALUE "STAT".                           CM1044.2
013300     02  FILLER PIC X(16) VALUE "MESSAGE CONTENTS".               CM1044.2
013400 01  LOG-HDR-4.                                                   CM1044.2
013500     02  FILLER PIC X VALUE SPACE.                                CM1044.2
013600     02  FILLER PIC X(12) VALUE ALL "-".                          CM1044.2
013700     02  FILLER PIC X VALUE SPACE.                                CM1044.2
013800     02  FILLER PIC X(11) VALUE ALL "-".                          CM1044.2
013900     02  FILLER PIC X VALUE SPACE.                                CM1044.2
014000     02  FILLER PIC XXX VALUE "--".                               CM1044.2
014100     02  FILLER PIC X(6) VALUE ALL "-".                           CM1044.2
014200     02  FILLER PIC X VALUE SPACE.                                CM1044.2
014300     02  FILLER PIC X(4) VALUE "---".                             CM1044.2
014400     02  FILLER PIC XXX VALUE "--".                               CM1044.2
014500     02  FILLER PIC X(5) VALUE "----".                            CM1044.2
014600     02  FILLER PIC X(72) VALUE ALL "-".                          CM1044.2
014700 01  LOG-LINE.                                                    CM1044.2
014800     02  FILLER PIC X VALUE SPACE.                                CM1044.2
014900     02  SYM-SOURCE PIC X(12).                                    CM1044.2
015000     02  FILLER PIC X VALUE SPACE.                                CM1044.2
015100     02  LOG-TIME.                                                CM1044.2
015200         03  HRS PIC 99.                                          CM1044.2
015300         03  FILLER PIC X VALUE ":".                              CM1044.2
015400         03  MINS PIC 99.                                         CM1044.2
015500         03  FILLER PIC X VALUE ":".                              CM1044.2
015600         03  SECS PIC 99.99.                                      CM1044.2
015700     02  FILLER PIC X VALUE SPACE.                                CM1044.2
015800     02  QUEUE-DEPTH PIC Z9.                                      CM1044.2
015900     02  OUT-TIME PIC -(4).99.                                    CM1044.2
016000     02  FILLER PIC X VALUE SPACE.                                CM1044.2
016100     02  MSG-LENGTH PIC ZZ9.                                      CM1044.2
016200     02  FILLER PIC X VALUE SPACE.                                CM1044.2
016300     02  IN-STATUS PIC XX.                                        CM1044.2
016400     02  FILLER PIC X VALUE SPACE.                                CM1044.2
016500     02  OUT-STATUS PIC XX.                                       CM1044.2
016600     02  FILLER PIC X VALUE "/".                                  CM1044.2
016700     02  OUT-ERR-KEY PIC X.                                       CM1044.2
016800     02  FILLER PIC X VALUE SPACE.                                CM1044.2
016900     02  MSG.                                                     CM1044.2
017000         03  KILL-FIELD PIC X(4).                                 CM1044.2
017100         03  FILLER PIC X(68).                                    CM1044.2
017200 01  SEND-TIME.                                                   CM1044.2
017300     02  S-HRS PIC 99.                                            CM1044.2
017400     02  S-MINS PIC 99.                                           CM1044.2
017500     02  S-SECS PIC 99V99.                                        CM1044.2
017600 COMMUNICATION SECTION.                                           CM1044.2
017700 CD  CM-INQUE-1 FOR INPUT.                                        CM1044.2
017800 01  INQUE-1-SPECIFICATIONS.                                      CM1044.2
017900     02  QUEUE-1 PIC X(24) VALUE                                  CM1044.2
018000     XXXXX030.                                                    CM1044.2
018100     02  FILLER PIC X(30) VALUE SPACES.                           CM1044.2
018200     02  TIME-RECEIVED-1.                                         CM1044.2
018300         03  HRS PIC 99.                                          CM1044.2
018400         03  MINS PIC 99.                                         CM1044.2
018500         03  SECS PIC 99V99.                                      CM1044.2
018600     02  SOURCE-1 PIC X(12).                                      CM1044.2
018700     02  IN-LENGTH-1 PIC 9(4).                                    CM1044.2
018800     02  END-KEY-1 PIC X.                                         CM1044.2
018900     02  IN-STATUS-1 PIC XX.                                      CM1044.2
019000     02  MSG-COUNT-1 PIC 9(6).                                    CM1044.2
019100 01  INQUE-1-DUMMY-RECORD PIC X(87).                              CM1044.2
019200 01  INQUE-1-DUMMY-TABLE.                                         CM1044.2
019300     02  DUMMY-NAME PIC 9 OCCURS 87 TIMES INDEXED BY I1.          CM1044.2
019400 CD  CM-OUTQUE-1 FOR OUTPUT.                                      CM1044.2
019500 01  OUTQUE-1-SPECIFIACTIONS.                                     CM1044.2
019600     02  DEST-COUNT-1 PIC 9(4) VALUE IS 1.                        CM1044.2
019700     02  OUT-LENGTH-1 PIC 9(4).                                   CM1044.2
019800     02  OUT-STATUS-1 PIC XX.                                     CM1044.2
019900     02  ERR-KEY-1 PIC X.                                         CM1044.2
020000     02  SYM-DEST-1 PIC X(12) VALUE                               CM1044.2
020100     XXXXX032.                                                    CM1044.2
020200 01  OUTQUE-1-DUMMY-RECORD PIC X(23).                             CM1044.2
020300 01  OUTQUE-1-DUMMY-TABLE.                                        CM1044.2
020400     02  DUMMY-NAME OCCURS 23 TIMES PIC X.                        CM1044.2
020500 CD  CM-INQUE-2 FOR INPUT                                         CM1044.2
020600     FILLER FILLER FILLER FILLER FILLER FILLER SOURCE-2           CM1044.2
020700     IN-LENGTH-2 END-KEY-2 IN-STATUS-2 MSG-COUNT-2.               CM1044.2
020800 01  INQUE-2-RECORD.                                              CM1044.2
020900     02  FILLER PIC X(54) VALUE                                   CM1044.2
021000     XXXXX034.                                                    CM1044.2
021100     02  TIME-RECEIVED-2.                                         CM1044.2
021200         03  HRS PIC 99.                                          CM1044.2
021300         03  MINS PIC 99.                                         CM1044.2
021400         03  SECS PIC 99V99.                                      CM1044.2
021500     02  FILLER PIC X(25).                                        CM1044.2
021600 CD  CM-OUTQUE-2 FOR OUTPUT                                       CM1044.2
021700     TEXT LENGTH OUT-LENGTH-2                                     CM1044.2
021800     STATUS KEY OUT-STATUS-2                                      CM1044.2
021900     ERROR KEY ERR-KEY-2.                                         CM1044.2
022000 01  OUTQUE-2-RECORD.                                             CM1044.2
022100     02  FILLER PIC 9(4) VALUE 1.                                 CM1044.2
022200     02  FILLER PIC X(7) VALUE SPACES.                            CM1044.2
022300     02  FILLER PIC X(12) VALUE                                   CM1044.2
022400     XXXXX035.                                                    CM1044.2
022500 PROCEDURE    DIVISION.                                           CM1044.2
022600 SECT-CM104M-0001 SECTION.                                        CM1044.2
022700 CM104M-INIT.                                                     CM1044.2
022800     OPEN     OUTPUT PRINT-FILE.                                  CM1044.2
022900     MOVE "CM104M     " TO TEST-ID.                               CM1044.2
023000     MOVE     TEST-ID TO ID-AGAIN.                                CM1044.2
023100     PERFORM HEAD-ROUTINE.                                        CM1044.2
023200     PERFORM LOG-HEADER.                                          CM1044.2
023300     ENABLE INPUT CM-INQUE-1 WITH KEY                             CM1044.2
023400     XXXXX031.                                                    CM1044.2
023500     ENABLE INPUT CM-INQUE-2 WITH KEY                             CM1044.2
023600     XXXXX036.                                                    CM1044.2
023700     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM1044.2
023800     XXXXX033.                                                    CM1044.2
023900     ENABLE OUTPUT CM-OUTQUE-2 WITH KEY                           CM1044.2
024000     XXXXX037.                                                    CM1044.2
024100 CM104M-POLL-1.                                                   CM1044.2
024200     MOVE SPACES TO MSG.                                          CM1044.2
024300     RECEIVE CM-INQUE-1 MESSAGE INTO MSG                          CM1044.2
024400         NO DATA GO TO CM104M-POLL-2.                             CM1044.2
024500     ACCEPT CM-INQUE-1 COUNT.                                     CM1044.2
024600     IF IN-LENGTH-1 IS GREATER THAN 72                            CM1044.2
024700         MOVE 72 TO OUT-LENGTH-2                                  CM1044.2
024800             ELSE MOVE IN-LENGTH-1 TO OUT-LENGTH-2.               CM1044.2
024900     SEND CM-OUTQUE-2 FROM MSG WITH EMI.                          CM1044.2
025000     ACCEPT SEND-TIME FROM TIME.                                  CM1044.2
025100     MOVE SOURCE-1 TO SYM-SOURCE.                                 CM1044.2
025200     MOVE CORR TIME-RECEIVED-1 TO LOG-TIME.                       CM1044.2
025300     COMPUTE OUT-TIME =                                           CM1044.2
025400         (S-HRS * 3600 + S-MINS * 60 + S-SECS) -                  CM1044.2
025500         (HRS OF TIME-RECEIVED-1 * 3600 + MINS OF TIME-RECEIVED-1 CM1044.2
025600          * 60 + SECS OF TIME-RECEIVED-1).                        CM1044.2
025700     MOVE MSG-COUNT-1 TO QUEUE-DEPTH.                             CM1044.2
025800     MOVE IN-LENGTH-1 TO MSG-LENGTH.                              CM1044.2
025900     MOVE IN-STATUS-1 TO IN-STATUS.                               CM1044.2
026000     MOVE OUT-STATUS-2 TO OUT-STATUS.                             CM1044.2
026100     MOVE ERR-KEY-2 TO OUT-ERR-KEY.                               CM1044.2
026200     MOVE LOG-LINE TO PRINT-REC.                                  CM1044.2
026300     WRITE PRINT-REC.                                             CM1044.2
026400     IF KILL-FIELD IS EQUAL TO "KILL" GO TO CM104M-FINI.          CM1044.2
026500 CM104M-POLL-2.                                                   CM1044.2
026600     MOVE SPACES TO MSG.                                          CM1044.2
026700     RECEIVE CM-INQUE-2 MESSAGE INTO MSG                          CM1044.2
026800         NO DATA GO TO CM104M-POLL-1.                             CM1044.2
026900     ACCEPT CM-INQUE-2 COUNT.                                     CM1044.2
027000     IF IN-LENGTH-2 IS GREATER THAN 72                            CM1044.2
027100         MOVE 72 TO OUT-LENGTH-1                                  CM1044.2
027200             ELSE MOVE IN-LENGTH-2 TO OUT-LENGTH-1.               CM1044.2
027300     SEND CM-OUTQUE-1 FROM MSG WITH EMI.                          CM1044.2
027400     ACCEPT SEND-TIME FROM TIME.                                  CM1044.2
027500     MOVE SOURCE-2 TO SYM-SOURCE.                                 CM1044.2
027600     MOVE CORR TIME-RECEIVED-2 TO LOG-TIME.                       CM1044.2
027700     COMPUTE OUT-TIME =                                           CM1044.2
027800         (S-HRS * 3600 + S-MINS * 60 + S-SECS) -                  CM1044.2
027900         (HRS OF TIME-RECEIVED-2 * 3600 + MINS OF TIME-RECEIVED-2 CM1044.2
028000          * 60 + SECS OF TIME-RECEIVED-2).                        CM1044.2
028100     MOVE MSG-COUNT-2 TO QUEUE-DEPTH.                             CM1044.2
028200     MOVE IN-LENGTH-2 TO MSG-LENGTH.                              CM1044.2
028300     MOVE IN-STATUS-2 TO IN-STATUS.                               CM1044.2
028400     MOVE OUT-STATUS-1 TO OUT-STATUS.                             CM1044.2
028500     MOVE ERR-KEY-1 TO OUT-ERR-KEY.                               CM1044.2
028600     MOVE LOG-LINE TO PRINT-REC.                                  CM1044.2
028700     WRITE PRINT-REC.                                             CM1044.2
028800     IF KILL-FIELD IS EQUAL TO "KILL" GO TO CM104M-FINI.          CM1044.2
028900     GO TO CM104M-POLL-1.                                         CM1044.2
029000 CM104M-FINI.                                                     CM1044.2
029100     PERFORM END-ROUTINE THRU END-ROUTINE-3.                      CM1044.2
029200     CLOSE    PRINT-FILE.                                         CM1044.2
029300     STOP     RUN.                                                CM1044.2
029400 END-ROUTINE.                                                     CM1044.2
029500     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        CM1044.2
029600     PERFORM WRITE-LINE.                                          CM1044.2
029700 PARA-Z.                                                          CM1044.2
029800     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           CM1044.2
029900     MOVE     CCVS-E-1 TO DUMMY-RECORD.                           CM1044.2
030000     PERFORM WRITE-LINE.                                          CM1044.2
030100 END-ROUTINE-3.                                                   CM1044.2
030200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           CM1044.2
030300     PERFORM WRITE-LINE.                                          CM1044.2
030400     MOVE CCVS-E-3 TO DUMMY-RECORD.                               CM1044.2
030500     PERFORM WRITE-LINE.                                          CM1044.2
030600 BLANK-LINE-PRINT.                                                CM1044.2
030700     MOVE     SPACE TO DUMMY-RECORD.                              CM1044.2
030800     PERFORM WRITE-LINE.                                          CM1044.2
030900 WRITE-LINE.                                                      CM1044.2
031000     WRITE DUMMY-RECORD AFTER ADVANCING 1 LINE.                   CM1044.2
031100 LOG-HEADER.                                                      CM1044.2
031200     MOVE LOG-HDR-1 TO PRINT-REC                                  CM1044.2
031300     WRITE PRINT-REC                                              CM1044.2
031400         AFTER 3 LINES.                                           CM1044.2
031500     MOVE LOG-HDR-2 TO PRINT-REC.                                 CM1044.2
031600     WRITE PRINT-REC                                              CM1044.2
031700         AFTER 3 LINES.                                           CM1044.2
031800     MOVE LOG-HDR-3 TO PRINT-REC.                                 CM1044.2
031900     WRITE PRINT-REC                                              CM1044.2
032000     MOVE LOG-HDR-4 TO PRINT-REC.                                 CM1044.2
032100     PERFORM WRITE-LINE.                                          CM1044.2
032200     MOVE SPACES TO PRINT-REC.                                    CM1044.2
032300     PERFORM WRITE-LINE.                                          CM1044.2
032400 HEAD-ROUTINE.                                                    CM1044.2
032500     MOVE CCVS-H-1 TO PRINT-REC                                   CM1044.2
032600     WRITE PRINT-REC                                              CM1044.2
032700         AFTER ADVANCING PAGE.                                    CM1044.2
032800     MOVE CCVS-H-2 TO PRINT-REC.                                  CM1044.2
032900     WRITE PRINT-REC                                              CM1044.2
033000         AFTER 2 LINES.                                           CM1044.2
033100     MOVE CCVS-H-3 TO PRINT-REC.                                  CM1044.2
033200     WRITE PRINT-REC                                              CM1044.2
033300         AFTER 5 LINES.                                           CM1044.2
033400     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1044.2
033500     PERFORM WRITE-LINE.                                          CM1044.2
      *END-OF,CM104M                                                                  
