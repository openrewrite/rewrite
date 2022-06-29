      *HEADER,COBOL,CM103M                                                            
000100 IDENTIFICATION DIVISION.                                         CM1034.2
000200 PROGRAM-ID.                                                      CM1034.2
000300     CM103M.                                                      CM1034.2
000400 AUTHOR.                                                          CM1034.2
000500     FEDERAL COMPILER TESTING CENTER.                             CM1034.2
000600* INSTALLATION.                                                    CM1034.2
000700*     GENERAL SERVICES ADMINISTRATION                              CM1034.2
000800**     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                CM1034.2
000900*     SOFTWARE DEVELOPMENT OFFICE.                                 CM1034.2
001000*     5203 LEESBURG PIKE  SUITE 1100                               CM1034.2
001100*     FALLS CHURCH VIRGINIA 22041.                                 CM1034.2
001200*                                                                  CM1034.2
001300*     PHONE   (703) 756-6153                                       CM1034.2
001400*                                                                  CM1034.2
001500*     " HIGH       ".                                              CM1034.2
001600 DATE-WRITTEN.                                                    CM1034.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           CM1034.2
001800     CREATION DATE     /    VALIDATION DATE                       CM1034.2
001900     "4.2 ".                                                      CM1034.2
002000 SECURITY.                                                        CM1034.2
002100     NONE.                                                        CM1034.2
002200 ENVIRONMENT DIVISION.                                            CM1034.2
002300 CONFIGURATION SECTION.                                           CM1034.2
002400 SOURCE-COMPUTER.                                                 CM1034.2
002500     XXXXX082.                                                    CM1034.2
002600 OBJECT-COMPUTER.                                                 CM1034.2
002700     XXXXX083.                                                    CM1034.2
002800 INPUT-OUTPUT SECTION.                                            CM1034.2
002900 FILE-CONTROL.                                                    CM1034.2
003000     SELECT PRINT-FILE ASSIGN TO                                  CM1034.2
003100     XXXXX055.                                                    CM1034.2
003200 DATA DIVISION.                                                   CM1034.2
003300 FILE SECTION.                                                    CM1034.2
003400 FD  PRINT-FILE                                                   CM1034.2
003500     LABEL RECORDS                                                CM1034.2
003600     XXXXX084                                                     CM1034.2
003700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       CM1034.2
003800 01  PRINT-REC PICTURE X(120).                                    CM1034.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 CM1034.2
004000 WORKING-STORAGE SECTION.                                         CM1034.2
004100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         CM1034.2
004200 01  REC-CT PICTURE 99 VALUE ZERO.                                CM1034.2
004300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        CM1034.2
004400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  CM1034.2
004500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          CM1034.2
004600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             CM1034.2
004700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              CM1034.2
004800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               CM1034.2
004900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           CM1034.2
005000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            CM1034.2
005100 01  CCVS-H-1.                                                    CM1034.2
005200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     CM1034.2
005300     02 FILLER PICTURE X(67) VALUE                                CM1034.2
005400     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  CM1034.2
005500-    " SYSTEM".                                                   CM1034.2
005600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   CM1034.2
005700 01  CCVS-H-2.                                                    CM1034.2
005800     02 FILLER PICTURE X(52) VALUE IS                             CM1034.2
005900     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   CM1034.2
006000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   CM1034.2
006100     02 TEST-ID PICTURE IS X(9).                                  CM1034.2
006200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   CM1034.2
006300 01  CCVS-H-3.                                                    CM1034.2
006400     02  FILLER PICTURE X(34) VALUE                               CM1034.2
006500     " FOR OFFICIAL USE ONLY    ".                                CM1034.2
006600     02  FILLER PICTURE X(58) VALUE                               CM1034.2
006700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".CM1034.2
006800     02  FILLER PICTURE X(28) VALUE                               CM1034.2
006900     "  COPYRIGHT   1974 ".                                       CM1034.2
007000 01  CCVS-E-1.                                                    CM1034.2
007100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   CM1034.2
007200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        CM1034.2
007300     02 ID-AGAIN PICTURE IS X(9).                                 CM1034.2
007400     02 FILLER PICTURE X(45) VALUE IS                             CM1034.2
007500     " NTIS DISTRIBUTION COBOL 74".                               CM1034.2
007600 01  CCVS-E-2.                                                    CM1034.2
007700     02  FILLER                   PICTURE X(31)  VALUE            CM1034.2
007800     SPACE.                                                       CM1034.2
007900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     CM1034.2
008000     02 CCVS-E-2-2.                                               CM1034.2
008100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            CM1034.2
008200         03 FILLER PICTURE IS X VALUE IS SPACE.                   CM1034.2
008300         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      CM1034.2
008400 01  CCVS-E-3.                                                    CM1034.2
008500     02  FILLER PICTURE X(22) VALUE                               CM1034.2
008600     " FOR OFFICIAL USE ONLY".                                    CM1034.2
008700     02  FILLER PICTURE X(12) VALUE SPACE.                        CM1034.2
008800     02  FILLER PICTURE X(58) VALUE                               CM1034.2
008900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".CM1034.2
009000     02  FILLER PICTURE X(13) VALUE SPACE.                        CM1034.2
009100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 CM1034.2
009200 01  CCVS-E-4.                                                    CM1034.2
009300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           CM1034.2
009400     02 FILLER PIC XXXX VALUE " OF ".                             CM1034.2
009500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           CM1034.2
009600     02 FILLER PIC X(40) VALUE                                    CM1034.2
009700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       CM1034.2
009800 01  XXINFO.                                                      CM1034.2
009900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    CM1034.2
010000     02 INFO-TEXT.                                                CM1034.2
010100     04 FILLER PIC X(20) VALUE SPACE.                             CM1034.2
010200     04 XXCOMPUTED PIC X(20).                                     CM1034.2
010300     04 FILLER PIC X(5) VALUE SPACE.                              CM1034.2
010400     04 XXCORRECT PIC X(20).                                      CM1034.2
010500 01  HYPHEN-LINE.                                                 CM1034.2
010600     02 FILLER PICTURE IS X VALUE IS SPACE.                       CM1034.2
010700     02 FILLER PICTURE IS X(65) VALUE IS "************************CM1034.2
010800-    "*****************************************".                 CM1034.2
010900     02 FILLER PICTURE IS X(54) VALUE IS "************************CM1034.2
011000-    "******************************".                            CM1034.2
011100 01  CCVS-PGM-ID PIC X(6) VALUE                                   CM1034.2
011200     "CM103M".                                                    CM1034.2
011300 01  MCS-TIME.                                                    CM1034.2
011400     02  HRS PIC 99.                                              CM1034.2
011500     02  MINS PIC 99.                                             CM1034.2
011600     02  SECS PIC 99V99.                                          CM1034.2
011700 01  IN-TIME.                                                     CM1034.2
011800     02  IN-HRS PIC 99.                                           CM1034.2
011900     02  IN-MINS PIC 99.                                          CM1034.2
012000     02  IN-SECS PIC 99V99.                                       CM1034.2
012100 01  OUT-TIME.                                                    CM1034.2
012200     02  OUT-HRS PIC 99.                                          CM1034.2
012300     02  OUT-MINS PIC 99.                                         CM1034.2
012400     02  OUT-SECS PIC 99V99.                                      CM1034.2
012500 01  LOG-HDR-1.                                                   CM1034.2
012600     02  FILLER PIC X(54) VALUE SPACES.                           CM1034.2
012700     02  FILLER PIC X(11) VALUE "MESSAGE LOG".                    CM1034.2
012800 01  LOG-HDR-2.                                                   CM1034.2
012900     02  FILLER PIC X VALUE SPACE.                                CM1034.2
013000     02  FILLER PIC X(12) VALUE "MCS RECEIPT".                    CM1034.2
013100     02  FILLER PIC X(8) VALUE "PROGRAM".                         CM1034.2
013200     02  FILLER PIC X(9) VALUE "MCS REC".                         CM1034.2
013300     02  FILLER PIC X(12) VALUE "RECV SEND".                      CM1034.2
013400     02  FILLER PIC X(38) VALUE "MSG".                            CM1034.2
013500     02  FILLER PIC X(7) VALUE "MESSAGE".                         CM1034.2
013600 01  LOG-HDR-3.                                                   CM1034.2
013700     02  FILLER PIC XXX VALUE SPACE.                              CM1034.2
013800     02  FILLER PIC X(10) VALUE "INBOUND".                        CM1034.2
013900     02  FILLER PIC X(8) VALUE "RECEIPT".                         CM1034.2
014000     02  FILLER PIC X(9) VALUE "OUTB""ND".                        CM1034.2
014100     02  FILLER PIC X(11) VALUE "STAT STAT".                      CM1034.2
014200     02  FILLER PIC X(39) VALUE "LENGTH".                         CM1034.2
014300     02  FILLER PIC X(7) VALUE "CONTENT".                         CM1034.2
014400 01  LOG-HDR-4.                                                   CM1034.2
014500     02  FILLER PIC X VALUE SPACE.                                CM1034.2
014600     02  FILLER PIC X(11) VALUE ALL "-".                          CM1034.2
014700     02  FILLER PIC X VALUE SPACE.                                CM1034.2
014800     02  FILLER PIC X(7) VALUE ALL "-".                           CM1034.2
014900     02  FILLER PIC X VALUE SPACE.                                CM1034.2
015000     02  FILLER PIC X(7) VALUE ALL "-".                           CM1034.2
015100     02  FILLER PIC XX VALUE SPACES.                              CM1034.2
015200     02  FILLER PIC X(11) VALUE "---- ----".                      CM1034.2
015300     02  FILLER PIC X(5) VALUE ALL "-".                           CM1034.2
015400     02  FILLER PIC XX VALUE SPACES.                              CM1034.2
015500     02  FILLER PIC X(72) VALUE ALL "-".                          CM1034.2
015600 01  LOG-LINE.                                                    CM1034.2
015700     02  FILLER PIC X VALUE SPACE.                                CM1034.2
015800     02  TIME-REC.                                                CM1034.2
015900         03  HRS PIC 99.                                          CM1034.2
016000         03  FILLER PIC X VALUE ":".                              CM1034.2
016100         03  MINS PIC 99.                                         CM1034.2
016200         03  FILLER PIC X VALUE ":".                              CM1034.2
016300         03  SECS PIC 99.99.                                      CM1034.2
016400     02  FILLER PIC X VALUE SPACE.                                CM1034.2
016500     02  PROG-TIME PIC ---.99.                                    CM1034.2
016600     02  FILLER PIC XX VALUE SPACES.                              CM1034.2
016700     02  TIME-SENT PIC ---.99.                                    CM1034.2
016800     02  FILLER PIC XXXX VALUE SPACES.                            CM1034.2
016900     02  RECV-STATUS PIC XX.                                      CM1034.2
017000     02  FILLER PIC XX VALUE SPACES.                              CM1034.2
017100     02  SEND-STATUS PIC XX.                                      CM1034.2
017200     02  FILLER PIC X VALUE "/".                                  CM1034.2
017300     02  SEND-ERR PIC X.                                          CM1034.2
017400     02  FILLER PIC XXX VALUE SPACES.                             CM1034.2
017500     02  MSG-LNGTH PIC ZZ9.                                       CM1034.2
017600     02  FILLER PIC XXX VALUE SPACES.                             CM1034.2
017700     02  MSG.                                                     CM1034.2
017800         03  KILL-FIELD PIC X(4).                                 CM1034.2
017900         03  FILLER PIC X(68).                                    CM1034.2
018000 COMMUNICATION SECTION.                                           CM1034.2
018100 CD  CM-INQUE-1 FOR INPUT                                         CM1034.2
018200     MAIN-QUEUE NO-SPEC-1 NO-SPEC-2 NO-SPEC-3 FILLER TIME-RECEIVEDCM1034.2
018300     FILLER IN-LENGTH END-KEY IN-STATUS FILLER.                   CM1034.2
018400 CD  CM-OUTQUE-1 FOR OUTPUT.                                      CM1034.2
018500 01  OUTQUE-SPECIFICATIONS.                                       CM1034.2
018600     02  ONE PIC 9999 VALUE IS 1.                                 CM1034.2
018700     02  OUT-LENGTH PIC 9999.                                     CM1034.2
018800     02  OUT-STATUS PIC XX.                                       CM1034.2
018900     02  ERR-KEY PIC X.                                           CM1034.2
019000     02  SYM-DEST PIC X(12) VALUE IS                              CM1034.2
019100     XXXXX032.                                                    CM1034.2
019200 PROCEDURE    DIVISION.                                           CM1034.2
019300 SECT-CM103M-0001 SECTION.                                        CM1034.2
019400 CM103M-INIT.                                                     CM1034.2
019500     OPEN     OUTPUT PRINT-FILE.                                  CM1034.2
019600     MOVE "CM103M     " TO TEST-ID.                               CM1034.2
019700     MOVE     TEST-ID TO ID-AGAIN.                                CM1034.2
019800     MOVE SPACES TO NO-SPEC-1 NO-SPEC-2 NO-SPEC-3.                CM1034.2
019900     MOVE                                                         CM1034.2
020000     XXXXX030                                                     CM1034.2
020100         TO MAIN-QUEUE.                                           CM1034.2
020200     ENABLE INPUT CM-INQUE-1 WITH KEY                             CM1034.2
020300     XXXXX031.                                                    CM1034.2
020400     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM1034.2
020500     XXXXX033.                                                    CM1034.2
020600     PERFORM HEAD-ROUTINE.                                        CM1034.2
020700     PERFORM LOG-HEADER.                                          CM1034.2
020800 RECEIVE-ECHO-AND-LOG.                                            CM1034.2
020900     MOVE SPACES TO MSG.                                          CM1034.2
021000     RECEIVE CM-INQUE-1 MESSAGE INTO MSG.                         CM1034.2
021100     ACCEPT IN-TIME FROM TIME.                                    CM1034.2
021200     IF IN-LENGTH IS GREATER THAN 72                              CM1034.2
021300         MOVE 72 TO OUT-LENGTH                                    CM1034.2
021400         ELSE MOVE IN-LENGTH TO OUT-LENGTH.                       CM1034.2
021500     SEND CM-OUTQUE-1 FROM MSG WITH EMI.                          CM1034.2
021600     ACCEPT OUT-TIME FROM TIME.                                   CM1034.2
021700     MOVE TIME-RECEIVED TO MCS-TIME.                              CM1034.2
021800     MOVE CORR MCS-TIME TO TIME-REC.                              CM1034.2
021900     COMPUTE PROG-TIME =                                          CM1034.2
022000         (IN-HRS * 3600 + IN-MINS * 60 + IN-SECS)  -              CM1034.2
022100         (HRS OF MCS-TIME * 3600 + MINS OF MCS-TIME * 60 +        CM1034.2
022200          SECS OF MCS-TIME).                                      CM1034.2
022300     COMPUTE TIME-SENT =                                          CM1034.2
022400         (OUT-HRS * 3600 + OUT-MINS * 60 + OUT-SECS)  -           CM1034.2
022500         (HRS OF MCS-TIME * 3600 + MINS OF MCS-TIME * 60 +        CM1034.2
022600          SECS OF MCS-TIME).                                      CM1034.2
022700     MOVE IN-STATUS TO RECV-STATUS.                               CM1034.2
022800     MOVE OUT-STATUS TO SEND-STATUS.                              CM1034.2
022900     MOVE ERR-KEY TO SEND-ERR.                                    CM1034.2
023000     MOVE IN-LENGTH TO MSG-LNGTH.                                 CM1034.2
023100     MOVE LOG-LINE TO PRINT-REC.                                  CM1034.2
023200     WRITE PRINT-REC.                                             CM1034.2
023300     IF KILL-FIELD IS NOT EQUAL TO "KILL"                         CM1034.2
023400         GO TO RECEIVE-ECHO-AND-LOG.                              CM1034.2
023500     PERFORM END-ROUTINE THRU END-ROUTINE-3.                      CM1034.2
023600     CLOSE    PRINT-FILE.                                         CM1034.2
023700     STOP     RUN.                                                CM1034.2
023800 END-ROUTINE.                                                     CM1034.2
023900     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        CM1034.2
024000     PERFORM WRITE-LINE.                                          CM1034.2
024100 PARA-Z.                                                          CM1034.2
024200     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           CM1034.2
024300     MOVE     CCVS-E-1 TO DUMMY-RECORD.                           CM1034.2
024400     PERFORM WRITE-LINE.                                          CM1034.2
024500 END-ROUTINE-3.                                                   CM1034.2
024600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           CM1034.2
024700     PERFORM WRITE-LINE.                                          CM1034.2
024800     MOVE CCVS-E-3 TO DUMMY-RECORD.                               CM1034.2
024900     PERFORM WRITE-LINE.                                          CM1034.2
025000 BLANK-LINE-PRINT.                                                CM1034.2
025100     MOVE     SPACE TO DUMMY-RECORD.                              CM1034.2
025200     PERFORM WRITE-LINE.                                          CM1034.2
025300 WRITE-LINE.                                                      CM1034.2
025400     WRITE DUMMY-RECORD AFTER ADVANCING 1 LINE.                   CM1034.2
025500 LOG-HEADER.                                                      CM1034.2
025600     MOVE LOG-HDR-1 TO PRINT-REC                                  CM1034.2
025700     WRITE PRINT-REC                                              CM1034.2
025800         AFTER 3 LINES.                                           CM1034.2
025900     MOVE LOG-HDR-2 TO PRINT-REC.                                 CM1034.2
026000     WRITE PRINT-REC                                              CM1034.2
026100         AFTER 3 LINES.                                           CM1034.2
026200     MOVE LOG-HDR-3 TO PRINT-REC.                                 CM1034.2
026300     WRITE PRINT-REC                                              CM1034.2
026400     MOVE LOG-HDR-4 TO PRINT-REC.                                 CM1034.2
026500     PERFORM WRITE-LINE.                                          CM1034.2
026600     MOVE SPACES TO PRINT-REC.                                    CM1034.2
026700     PERFORM WRITE-LINE.                                          CM1034.2
026800 HEAD-ROUTINE.                                                    CM1034.2
026900     MOVE CCVS-H-1 TO PRINT-REC                                   CM1034.2
027000     WRITE PRINT-REC                                              CM1034.2
027100         AFTER ADVANCING PAGE.                                    CM1034.2
027200     MOVE CCVS-H-2 TO PRINT-REC.                                  CM1034.2
027300     WRITE PRINT-REC                                              CM1034.2
027400         AFTER 2 LINES.                                           CM1034.2
027500     MOVE CCVS-H-3 TO PRINT-REC.                                  CM1034.2
027600     WRITE PRINT-REC                                              CM1034.2
027700         AFTER 5 LINES.                                           CM1034.2
027800     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1034.2
027900     PERFORM WRITE-LINE.                                          CM1034.2
      *END-OF,CM103M                                                                  
