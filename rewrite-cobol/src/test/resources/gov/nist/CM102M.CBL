      *HEADER,COBOL,CM102M                                                            
000100 IDENTIFICATION DIVISION.                                         CM1024.2
000200 PROGRAM-ID.                                                      CM1024.2
000300     CM102M.                                                      CM1024.2
000400 AUTHOR.                                                          CM1024.2
000500     FEDERAL COMPILER TESTING CENTER.                             CM1024.2
000600* INSTALLATION.                                                    CM1024.2
000700*     GENERAL SERVICES ADMINISTRATION                              CM1024.2
000800**     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                CM1024.2
000900*     SOFTWARE DEVELOPMENT OFFICE.                                 CM1024.2
001000*     5203 LEESBURG PIKE  SUITE 1100                               CM1024.2
001100*     FALLS CHURCH VIRGINIA 22041.                                 CM1024.2
001200*                                                                  CM1024.2
001300*     PHONE   (703) 756-6153                                       CM1024.2
001400*                                                                  CM1024.2
001500*     " HIGH       ".                                              CM1024.2
001600 DATE-WRITTEN.                                                    CM1024.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           CM1024.2
001800     CREATION DATE     /    VALIDATION DATE                       CM1024.2
001900     "4.2 ".                                                      CM1024.2
002000 SECURITY.                                                        CM1024.2
002100     NONE.                                                        CM1024.2
002200 ENVIRONMENT DIVISION.                                            CM1024.2
002300 CONFIGURATION SECTION.                                           CM1024.2
002400 SOURCE-COMPUTER.                                                 CM1024.2
002500     XXXXX082.                                                    CM1024.2
002600 OBJECT-COMPUTER.                                                 CM1024.2
002700     XXXXX083.                                                    CM1024.2
002800 INPUT-OUTPUT SECTION.                                            CM1024.2
002900 FILE-CONTROL.                                                    CM1024.2
003000     SELECT PRINT-FILE ASSIGN TO                                  CM1024.2
003100     XXXXX055.                                                    CM1024.2
003200 DATA DIVISION.                                                   CM1024.2
003300 FILE SECTION.                                                    CM1024.2
003400 FD  PRINT-FILE                                                   CM1024.2
003500     LABEL RECORDS                                                CM1024.2
003600     XXXXX084                                                     CM1024.2
003700     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       CM1024.2
003800 01  PRINT-REC PICTURE X(120).                                    CM1024.2
003900 01  DUMMY-RECORD PICTURE X(120).                                 CM1024.2
004000 WORKING-STORAGE SECTION.                                         CM1024.2
004100 77  COMP-TWO PIC 9 COMP VALUE 2.                                 CM1024.2
004200 77  TWO PIC 9 VALUE 2.                                           CM1024.2
004300 77  COMP-THREE PIC 9 VALUE 3.                                    CM1024.2
004400 77  THREE PIC 9 VALUE 3.                                         CM1024.2
004500 77  SEND-SWITCH PIC 99 COMP.                                     CM1024.2
004600 77  MSG-NUM PIC 9(4).                                            CM1024.2
004700 77  MSG-70 PIC X(70).                                            CM1024.2
004800 77  PASSWORD1 PIC X(10) VALUE                                    CM1024.2
004900     XXXXX033.                                                    CM1024.2
005000 01  ERR-MSG.                                                     CM1024.2
005100     02  FILLER PIC X(33) VALUE                                   CM1024.2
005200         "THIS MESSAGE SHOULD NOT APPEAR - ".                     CM1024.2
005300     02  TEST-IND PIC X(4).                                       CM1024.2
005400 01  LOG-HDR-1.                                                   CM1024.2
005500     02  FILLER PIC X(48) VALUE SPACES.                           CM1024.2
005600     02  FILLER PIC X(24) VALUE "LOG OF OUTGOING MESSAGES".       CM1024.2
005700 01  LOG-HDR-2.                                                   CM1024.2
005800     02  FILLER PIC X VALUE SPACE.                                CM1024.2
005900     02  FILLER PIC X(14) VALUE "START  TIME".                    CM1024.2
006000     02  FILLER PIC X(10) VALUE "ELAPSED".                        CM1024.2
006100     02  FILLER PIC X(13) VALUE "STATUS/ERR".                     CM1024.2
006200     02  FILLER PIC X(41) VALUE "LENGTH".                         CM1024.2
006300     02  FILLER PIC X(7) VALUE "MESSAGE".                         CM1024.2
006400 01  LOG-HDR-3.                                                   CM1024.2
006500     02  FILLER PIC X VALUE SPACES.                               CM1024.2
006600     02  FILLER PIC X(11) VALUE ALL "-".                          CM1024.2
006700     02  FILLER PIC XXX VALUE SPACES.                             CM1024.2
006800     02  FILLER PIC X(7) VALUE ALL "-".                           CM1024.2
006900     02  FILLER PIC XXX VALUE SPACES.                             CM1024.2
007000     02  FILLER PIC X(10) VALUE ALL "-".                          CM1024.2
007100     02  FILLER PIC XXX VALUE SPACES.                             CM1024.2
007200     02  FILLER PIC X(6) VALUE ALL "-".                           CM1024.2
007300     02  FILLER PIC XXX VALUE SPACES.                             CM1024.2
007400     02  FILLER PIC X(72) VALUE ALL "-".                          CM1024.2
007500 01  LOG-LINE.                                                    CM1024.2
007600     02  FILLER PIC X VALUE SPACE.                                CM1024.2
007700     02  START-TIME.                                              CM1024.2
007800         03  HOURS PIC 99.                                        CM1024.2
007900         03  FILLER PIC X VALUE ":".                              CM1024.2
008000         03  MINUTES PIC 99.                                      CM1024.2
008100         03  FILLER PIC X VALUE ":".                              CM1024.2
008200         03  SECONDS PIC 99.99.                                   CM1024.2
008300     02  FILLER PIC XX VALUE SPACES.                              CM1024.2
008400     02  ELAPSED PIC -(4)9.99.                                    CM1024.2
008500     02  FILLER PIC X(7) VALUE SPACES.                            CM1024.2
008600     02  STAT PIC 99.                                             CM1024.2
008700     02  FILLER PIC X VALUE "/".                                  CM1024.2
008800     02  ERR PIC 9.                                               CM1024.2
008900     02  FILLER PIC X(5) VALUE SPACES.                            CM1024.2
009000     02  LNTH PIC ZZZ9.                                           CM1024.2
009100     02  FILLER PIC X(5) VALUE SPACES.                            CM1024.2
009200     02  MSG-OUT PIC X(72).                                       CM1024.2
009300 01  LOG-LINE-1.                                                  CM1024.2
009400     02  FILLER PIC X(39) VALUE SPACES.                           CM1024.2
009500     02  FILLER PIC X(8) VALUE "CONT".                            CM1024.2
009600     02  MSG-FLD PIC X(72).                                       CM1024.2
009700 01  SUPERIMPOSITION.                                             CM1024.2
009800     02  S-ALL PIC X(4).                                          CM1024.2
009900     02  S-WORDS PIC X(6).                                        CM1024.2
010000     02  S-IN PIC X(3).                                           CM1024.2
010100     02  S-THIS PIC X(5).                                         CM1024.2
010200     02  S-MESSAGE PIC X(8).                                      CM1024.2
010300     02  S-SHOULD PIC X(7).                                       CM1024.2
010400     02  S-COME PIC X(5).                                         CM1024.2
010500     02  S-OUT PIC X(4).                                          CM1024.2
010600     02  S-ON PIC XXX.                                            CM1024.2
010700     02  S-THE PIC X(4).                                          CM1024.2
010800     02  S-SAME PIC X(5).                                         CM1024.2
010900     02  S-LINE PIC X(5).                                         CM1024.2
011000 01  MSG-A.                                                       CM1024.2
011100     02  FILLER PIC X VALUE SPACE.                                CM1024.2
011200     02  MSG-B.                                                   CM1024.2
011300         03  FILLER PIC X VALUE SPACE.                            CM1024.2
011400         03  MSG-C.                                               CM1024.2
011500             04  FILLER PIC X VALUE SPACE.                        CM1024.2
011600             04  MSG-D.                                           CM1024.2
011700                 05  FILLER PIC X VALUE SPACE.                    CM1024.2
011800                 05  MSG-E.                                       CM1024.2
011900                     06  FILLER PIC X(19) VALUE                   CM1024.2
012000                             "THIS IS MESSAGE NO.".               CM1024.2
012100                     06  MSG-NO PIC ZZZZ.                         CM1024.2
012200                     06  FILLER PIC X(35) VALUE                   CM1024.2
012300                             ".--THIS SENTENCE MUST NOT APPEAR.". CM1024.2
012400 01  SYSTEM-TIME.                                                 CM1024.2
012500     02  HOURS PIC 99.                                            CM1024.2
012600     02  MINUTES PIC 99.                                          CM1024.2
012700     02  SECONDS PIC 99V99.                                       CM1024.2
012800 01  COMP-TIME.                                                   CM1024.2
012900     02  COMP-HRS PIC 99.                                         CM1024.2
013000     02  COMP-MINS PIC 99.                                        CM1024.2
013100     02  COMP-SECS PIC 99V99.                                     CM1024.2
013200 01  MSG-F.                                                       CM1024.2
013300     02  FILLER PIC X(19) VALUE  "THIS IS MESSAGE NO.".           CM1024.2
013400     02  MSG-F-NO PIC ZZZZ.                                       CM1024.2
013500     02  FILLER PIC X(40) VALUE                                   CM1024.2
013600             " AND SHOULD APPEAR AT THE TOP OF A PAGE.".          CM1024.2
013700 01  MSG-G.                                                       CM1024.2
013800     02  FILLER PIC X(19) VALUE  "THIS IS MESSAGE NO.".           CM1024.2
013900     02  MSG-G-NO PIC ZZZZ.                                       CM1024.2
014000     02  FILLER PIC X(41) VALUE                                   CM1024.2
014100             " AND SHOULD APPEAR AFTER TWO BLANK LINES.".         CM1024.2
014200 01  MSG-H.                                                       CM1024.2
014300     02  FILLER PIC X(19) VALUE "THIS IS MESSAGE NO.".            CM1024.2
014400     02  MSG-H-NO PIC ZZZZ.                                       CM1024.2
014500     02  FILLER PIC X(41) VALUE                                   CM1024.2
014600             " AND SHOULD APPEAR BEFORE ONE BLANK LINE.".         CM1024.2
014700 01  LONG-MSG.                                                    CM1024.2
014800     02  LONG-MSG-S1 PIC X(73) VALUE "ON PAGE XIII-21, PARAGRAPH 3CM1024.2
014900-        ".5.4(1)C, THE COBOL STANDARD STATES, ""EXCESS ".        CM1024.2
015000     02  LONG-MSG-S2 PIC X(67) VALUE "CHARACTERS OF A MESSAGE OR MCM1024.2
015100-        "ESSAGE SEGMENT WILL NOT BE TRUNCATED.  ".               CM1024.2
015200     02  LONG-MSG-S3 PIC X(71) VALUE "CHARACTERS WILL BE PACKED TOCM1024.2
015300-        " A SIZE EQUAL TO THAT OF THE PHYSICAL LINE ".           CM1024.2
015400     02  LONG-MSG-S4 PIC X(69) VALUE "AND THEN OUTPUTTED TO THE DECM1024.2
015500-        "VICE.  THE PROCESS CONTINUES ON THE NEXT ".             CM1024.2
015600     02  LONG-MSG-S5 PIC X(73) VALUE "LINE WITH THE EXCESS CHARACTCM1024.2
015700-        "ERS.""  IF THIS ENTIRE PARAGRAPH WAS RECEIVED ".        CM1024.2
015800     02  LONG-MSG-S6 PIC X(71) VALUE "BY THE DESIGNATED DEVICE, THCM1024.2
015900-        "EN THE FOREGOING RULE IS SUPPORTED BY THIS ".           CM1024.2
016000     02  LONG-MSG-S7 PIC X(9) VALUE "COMPILER.".                  CM1024.2
016100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         CM1024.2
016200 01  REC-CT PICTURE 99 VALUE ZERO.                                CM1024.2
016300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        CM1024.2
016400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  CM1024.2
016500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          CM1024.2
016600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             CM1024.2
016700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              CM1024.2
016800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               CM1024.2
016900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           CM1024.2
017000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            CM1024.2
017100 01  CCVS-H-1.                                                    CM1024.2
017200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     CM1024.2
017300     02 FILLER PICTURE X(67) VALUE                                CM1024.2
017400     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  CM1024.2
017500-    " SYSTEM".                                                   CM1024.2
017600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   CM1024.2
017700 01  CCVS-H-2.                                                    CM1024.2
017800     02 FILLER PICTURE X(52) VALUE IS                             CM1024.2
017900     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   CM1024.2
018000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   CM1024.2
018100     02 TEST-ID PICTURE IS X(9).                                  CM1024.2
018200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   CM1024.2
018300 01  CCVS-H-3.                                                    CM1024.2
018400     02  FILLER PICTURE X(34) VALUE                               CM1024.2
018500     " FOR OFFICIAL USE ONLY    ".                                CM1024.2
018600     02  FILLER PICTURE X(58) VALUE                               CM1024.2
018700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".CM1024.2
018800     02  FILLER PICTURE X(28) VALUE                               CM1024.2
018900     "  COPYRIGHT   1974 ".                                       CM1024.2
019000 01  CCVS-E-1.                                                    CM1024.2
019100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   CM1024.2
019200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        CM1024.2
019300     02 ID-AGAIN PICTURE IS X(9).                                 CM1024.2
019400     02 FILLER PICTURE X(45) VALUE IS                             CM1024.2
019500     " NTIS DISTRIBUTION COBOL 74".                               CM1024.2
019600 01  CCVS-E-2.                                                    CM1024.2
019700     02  FILLER                   PICTURE X(31)  VALUE            CM1024.2
019800     SPACE.                                                       CM1024.2
019900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     CM1024.2
020000     02 CCVS-E-2-2.                                               CM1024.2
020100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            CM1024.2
020200         03 FILLER PICTURE IS X VALUE IS SPACE.                   CM1024.2
020300         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      CM1024.2
020400 01  CCVS-E-3.                                                    CM1024.2
020500     02  FILLER PICTURE X(22) VALUE                               CM1024.2
020600     " FOR OFFICIAL USE ONLY".                                    CM1024.2
020700     02  FILLER PICTURE X(12) VALUE SPACE.                        CM1024.2
020800     02  FILLER PICTURE X(58) VALUE                               CM1024.2
020900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".CM1024.2
021000     02  FILLER PICTURE X(13) VALUE SPACE.                        CM1024.2
021100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 CM1024.2
021200 01  CCVS-E-4.                                                    CM1024.2
021300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           CM1024.2
021400     02 FILLER PIC XXXX VALUE " OF ".                             CM1024.2
021500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           CM1024.2
021600     02 FILLER PIC X(40) VALUE                                    CM1024.2
021700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       CM1024.2
021800 01  XXINFO.                                                      CM1024.2
021900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    CM1024.2
022000     02 INFO-TEXT.                                                CM1024.2
022100     04 FILLER PIC X(20) VALUE SPACE.                             CM1024.2
022200     04 XXCOMPUTED PIC X(20).                                     CM1024.2
022300     04 FILLER PIC X(5) VALUE SPACE.                              CM1024.2
022400     04 XXCORRECT PIC X(20).                                      CM1024.2
022500 01  HYPHEN-LINE.                                                 CM1024.2
022600     02 FILLER PICTURE IS X VALUE IS SPACE.                       CM1024.2
022700     02 FILLER PICTURE IS X(65) VALUE IS "************************CM1024.2
022800-    "*****************************************".                 CM1024.2
022900     02 FILLER PICTURE IS X(54) VALUE IS "************************CM1024.2
023000-    "******************************".                            CM1024.2
023100 01  CCVS-PGM-ID PIC X(6) VALUE                                   CM1024.2
023200     "CM102M".                                                    CM1024.2
023300 01  TEST-RESULTS.                                                CM1024.2
023400     02 FILLER                    PICTURE X VALUE SPACE.          CM1024.2
023500     02 FEATURE                   PICTURE X(18).                  CM1024.2
023600     02 FILLER                    PICTURE X VALUE SPACE.          CM1024.2
023700     02 P-OR-F                    PICTURE X(5).                   CM1024.2
023800     02 FILLER                    PICTURE X  VALUE SPACE.         CM1024.2
023900     02  PAR-NAME PIC X(20).                                      CM1024.2
024000     02 FILLER                    PICTURE X VALUE SPACE.          CM1024.2
024100     02 COMPUTED-A                PICTURE X(20).                  CM1024.2
024200     02  COMPUTED-SLASH-SET REDEFINES COMPUTED-A.                 CM1024.2
024300         03  FILLER PIC X(8).                                     CM1024.2
024400         03  COMPUTED-STATUS PIC XX.                              CM1024.2
024500         03  SLASH PIC X.                                         CM1024.2
024600         03  COMPUTED-ERR-KEY PIC X.                              CM1024.2
024700         03  FILLER PIC X(8).                                     CM1024.2
024800     02 FILLER                    PICTURE X VALUE SPACE.          CM1024.2
024900     02 CORRECT-A                 PICTURE X(20).                  CM1024.2
025000     02  CORRECT-SLASH-SET REDEFINES CORRECT-A.                   CM1024.2
025100         03  FILLER PIC X(8).                                     CM1024.2
025200         03  CORRECT-2SLASH1 PIC 99/9.                            CM1024.2
025300         03  FILLER PIC X(8).                                     CM1024.2
025400     02 FILLER                    PICTURE X VALUE SPACE.          CM1024.2
025500     02 RE-MARK                   PICTURE X(30).                  CM1024.2
025600 01  COLUMNS-LINE-1.                                              CM1024.2
025700     02  FILLER PIC X(3) VALUE SPACES.                            CM1024.2
025800     02  FILLER PIC X(17) VALUE "FEATURE TESTED".                 CM1024.2
025900     02  FILLER PIC X(9) VALUE "RESLT".                           CM1024.2
026000     02  FILLER PIC X(21) VALUE "PARAGRAPH NAME".                 CM1024.2
026100     02  FILLER PIC X(22) VALUE "COMPUTED DATA".                  CM1024.2
026200     02  FILLER PIC X(29) VALUE "CORRECT DATA".                   CM1024.2
026300     02  FILLER PIC X(7) VALUE "REMARKS".                         CM1024.2
026400 01  COLUMNS-LINE-2.                                              CM1024.2
026500     02  FILLER PIC X VALUE SPACE.                                CM1024.2
026600     02  FILLER PIC X(18) VALUE ALL "-".                          CM1024.2
026700     02  FILLER PIC X VALUE SPACE.                                CM1024.2
026800     02  FILLER PIC X(5) VALUE ALL "-".                           CM1024.2
026900     02  FILLER PIC X VALUE SPACE.                                CM1024.2
027000     02  FILLER PIC X(20) VALUE ALL "-".                          CM1024.2
027100     02  FILLER PIC X VALUE SPACE.                                CM1024.2
027200     02  FILLER PIC X(20) VALUE ALL "-".                          CM1024.2
027300     02  FILLER PIC X VALUE SPACE.                                CM1024.2
027400     02  FILLER PIC X(20) VALUE ALL "-".                          CM1024.2
027500     02  FILLER PIC X VALUE SPACE.                                CM1024.2
027600     02  FILLER PIC X(31) VALUE ALL "-".                          CM1024.2
027700 COMMUNICATION SECTION.                                           CM1024.2
027800 CD  CM-OUTQUE-1 FOR OUTPUT                                       CM1024.2
027900     DESTINATION COUNT IS ONE                                     CM1024.2
028000     TEXT LENGTH IS MSG-LENGTH                                    CM1024.2
028100     STATUS KEY IS STATUS-KEY                                     CM1024.2
028200     ERROR KEY IS ERR-KEY                                         CM1024.2
028300     SYMBOLIC DESTINATION IS SYM-DEST.                            CM1024.2
028400 PROCEDURE    DIVISION.                                           CM1024.2
028500 SECT-CM102M-0001 SECTION.                                        CM1024.2
028600 CM102M-INIT.                                                     CM1024.2
028700     OPEN     OUTPUT PRINT-FILE.                                  CM1024.2
028800     MOVE "CM102M     " TO TEST-ID.                               CM1024.2
028900     MOVE     TEST-ID TO ID-AGAIN.                                CM1024.2
029000     MOVE    SPACE TO TEST-RESULTS.                               CM1024.2
029100     PERFORM HEAD-ROUTINE.                                        CM1024.2
029200     PERFORM COLUMN-NAMES-ROUTINE.                                CM1024.2
029300     MOVE "MCS STATUS WORD" TO FEATURE.                           CM1024.2
029400 DISAB-STATUS-TEST-01.                                            CM1024.2
029500     MOVE "INITIAL DISABLE TO OUTPUT CD" TO RE-MARK.              CM1024.2
029600     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
029700     MOVE 1 TO ONE.                                               CM1024.2
029800     MOVE                                                         CM1024.2
029900     XXXXX032                                                     CM1024.2
030000         TO SYM-DEST.                                             CM1024.2
030100     DISABLE OUTPUT CM-OUTQUE-1 WITH KEY                          CM1024.2
030200     XXXXX033.                                                    CM1024.2
030300     MOVE "INFO" TO P-OR-F.                                       CM1024.2
030400     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
030500     MOVE "/" TO SLASH.                                           CM1024.2
030600     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
030700     MOVE "       INFO TEST FOR" TO CORRECT-A.                    CM1024.2
030800     GO TO DISAB-STATUS-WRITE-01.                                 CM1024.2
030900 DISAB-STATUS-DELETE-01.                                          CM1024.2
031000     PERFORM DE-LETE.                                             CM1024.2
031100 DISAB-STATUS-WRITE-01.                                           CM1024.2
031200     MOVE "DISAB-STATUS-TEST-01" TO PAR-NAME.                     CM1024.2
031300     PERFORM PRINT-DETAIL.                                        CM1024.2
031400 DISAB-STATUS-TEST-02.                                            CM1024.2
031500     MOVE "NO DESTINATION SPECIFIED" TO RE-MARK.                  CM1024.2
031600     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
031700     MOVE "GARBAGE" TO SYM-DEST.                                  CM1024.2
031800     MOVE 1 TO ONE.                                               CM1024.2
031900     DISABLE OUTPUT CM-OUTQUE-1 WITH KEY                          CM1024.2
032000     XXXXX033.                                                    CM1024.2
032100     IF STATUS-KEY IS EQUAL TO "20"                               CM1024.2
032200         AND ERR-KEY IS EQUAL TO "1"                              CM1024.2
032300         PERFORM PASS GO TO DISAB-STATUS-WRITE-02.                CM1024.2
032400     MOVE 201 TO CORRECT-2SLASH1.                                 CM1024.2
032500     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
032600     MOVE "/" TO SLASH.                                           CM1024.2
032700     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
032800     PERFORM FAIL.                                                CM1024.2
032900     GO TO DISAB-STATUS-WRITE-02.                                 CM1024.2
033000 DISAB-STATUS-DELETE-02.                                          CM1024.2
033100     PERFORM DE-LETE.                                             CM1024.2
033200 DISAB-STATUS-WRITE-02.                                           CM1024.2
033300     MOVE "DISAB-STATUS-TEST-02" TO PAR-NAME.                     CM1024.2
033400     PERFORM PRINT-DETAIL.                                        CM1024.2
033500 DISAB-STATUS-TEST-03.                                            CM1024.2
033600     MOVE "INVALID PASSWORD USED" TO RE-MARK.                     CM1024.2
033700     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
033800     MOVE 1 TO ONE.                                               CM1024.2
033900     MOVE                                                         CM1024.2
034000     XXXXX032                                                     CM1024.2
034100         TO SYM-DEST.                                             CM1024.2
034200     DISABLE OUTPUT CM-OUTQUE-1 WITH KEY                          CM1024.2
034300         "GARBAGE".                                               CM1024.2
034400     IF STATUS-KEY IS EQUAL TO "40"                               CM1024.2
034500         PERFORM PASS GO TO DISAB-STATUS-WRITE-03.                CM1024.2
034600     MOVE 400 TO CORRECT-2SLASH1.                                 CM1024.2
034700     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
034800     MOVE "/" TO SLASH.                                           CM1024.2
034900     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
035000     PERFORM FAIL.                                                CM1024.2
035100     GO TO DISAB-STATUS-WRITE-03.                                 CM1024.2
035200 DISAB-STATUS-DELETE-03.                                          CM1024.2
035300     PERFORM DE-LETE.                                             CM1024.2
035400 DISAB-STATUS-WRITE-03.                                           CM1024.2
035500     MOVE "DISAB-STATUS-TEST-03" TO PAR-NAME.                     CM1024.2
035600     PERFORM PRINT-DETAIL.                                        CM1024.2
035700 DISAB-STATUS-TEST-04.                                            CM1024.2
035800     MOVE "INVALID DESTINATION COUNT (0)" TO RE-MARK.             CM1024.2
035900     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
036000     MOVE                                                         CM1024.2
036100     XXXXX032                                                     CM1024.2
036200         TO SYM-DEST.                                             CM1024.2
036300     MOVE 0 TO ONE.                                               CM1024.2
036400     DISABLE OUTPUT CM-OUTQUE-1 WITH KEY                          CM1024.2
036500     XXXXX033.                                                    CM1024.2
036600     IF STATUS-KEY IS EQUAL TO "30"                               CM1024.2
036700         PERFORM PASS GO TO DISAB-STATUS-WRITE-04.                CM1024.2
036800     MOVE 300 TO CORRECT-2SLASH1.                                 CM1024.2
036900     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
037000     MOVE "/" TO SLASH.                                           CM1024.2
037100     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
037200     PERFORM FAIL.                                                CM1024.2
037300     GO TO DISAB-STATUS-WRITE-04.                                 CM1024.2
037400 DISAB-STATUS-DELETE-04.                                          CM1024.2
037500     PERFORM DE-LETE.                                             CM1024.2
037600 DISAB-STATUS-WRITE-04.                                           CM1024.2
037700     MOVE "DISAB-STATUS-TEST-04" TO PAR-NAME.                     CM1024.2
037800     PERFORM PRINT-DETAIL.                                        CM1024.2
037900 DISAB-STATUS-TEST-05.                                            CM1024.2
038000     MOVE "COMBINATION ERROR" TO RE-MARK.                         CM1024.2
038100     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
038200     MOVE SPACES TO SYM-DEST.                                     CM1024.2
038300     MOVE 0 TO ONE.                                               CM1024.2
038400     DISABLE OUTPUT CM-OUTQUE-1 WITH KEY                          CM1024.2
038500         "GARBAGE".                                               CM1024.2
038600     MOVE "INFO" TO P-OR-F.                                       CM1024.2
038700     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
038800     MOVE "/" TO SLASH.                                           CM1024.2
038900     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
039000     GO TO DISAB-STATUS-WRITE-05.                                 CM1024.2
039100 DISAB-STATUS-DELETE-05.                                          CM1024.2
039200     PERFORM DE-LETE.                                             CM1024.2
039300 DISAB-STATUS-WRITE-05.                                           CM1024.2
039400     MOVE "DISAB-STATUS-TEST-05" TO PAR-NAME.                     CM1024.2
039500     PERFORM PRINT-DETAIL.                                        CM1024.2
039600 SEND-STATUS-TEST-01.                                             CM1024.2
039700     MOVE "DESTINATION DISABLED" TO RE-MARK.                      CM1024.2
039800     MOVE "CM102M- I AM THE FIRST MESSAGE IN QUEUE;" TO MSG-70.   CM1024.2
039900     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
040000     MOVE                                                         CM1024.2
040100     XXXXX032                                                     CM1024.2
040200         TO SYM-DEST.                                             CM1024.2
040300     MOVE 1 TO ONE.                                               CM1024.2
040400     MOVE 45 TO MSG-LENGTH.                                       CM1024.2
040500     SEND CM-OUTQUE-1 FROM MSG-70 WITH EMI                        CM1024.2
040600         AFTER ADVANCING PAGE.                                    CM1024.2
040700     MOVE "THOU SHALT HAVE NO OTHER MESSAGES BEFORE ME." TO MSG-70CM1024.2
040800     SEND CM-OUTQUE-1 FROM MSG-70 WITH EMI.                       CM1024.2
040900     MOVE SPACES TO MSG-70.                                       CM1024.2
041000     MOVE 1 TO MSG-LENGTH.                                        CM1024.2
041100     SEND CM-OUTQUE-1 FROM MSG-70 WITH EGI.                       CM1024.2
041200     IF STATUS-KEY IS EQUAL TO "10"                               CM1024.2
041300         PERFORM PASS GO TO SEND-STATUS-WRITE-01.                 CM1024.2
041400     MOVE 100 TO CORRECT-2SLASH1.                                 CM1024.2
041500     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
041600     MOVE "/" TO SLASH.                                           CM1024.2
041700     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
041800     PERFORM FAIL.                                                CM1024.2
041900     GO TO SEND-STATUS-WRITE-01.                                  CM1024.2
042000 SEND-STATUS-DELETE-01.                                           CM1024.2
042100     PERFORM DE-LETE.                                             CM1024.2
042200 SEND-STATUS-WRITE-01.                                            CM1024.2
042300     MOVE "SEND-STATUS-TEST-01" TO PAR-NAME.                      CM1024.2
042400     PERFORM PRINT-DETAIL.                                        CM1024.2
042500 SEND-STATUS-TEST-02.                                             CM1024.2
042600     MOVE "COMBINATION ERROR" TO RE-MARK.                         CM1024.2
042700     MOVE SPACES TO SYM-DEST.                                     CM1024.2
042800     MOVE 0 TO ONE.                                               CM1024.2
042900     MOVE 100 TO MSG-LENGTH.                                      CM1024.2
043000     MOVE "S-02" TO TEST-IND.                                     CM1024.2
043100     SEND CM-OUTQUE-1 FROM ERR-MSG WITH EMI.                      CM1024.2
043200     MOVE "INFO" TO P-OR-F.                                       CM1024.2
043300     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
043400     MOVE "/" TO SLASH.                                           CM1024.2
043500     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
043600     GO TO SEND-STATUS-WRITE-02.                                  CM1024.2
043700 SEND-STATUS-DELETE-02.                                           CM1024.2
043800     PERFORM DE-LETE.                                             CM1024.2
043900 SEND-STATUS-WRITE-02.                                            CM1024.2
044000     MOVE "SEND-STATUS-TEST-02" TO PAR-NAME.                      CM1024.2
044100     PERFORM PRINT-DETAIL.                                        CM1024.2
044200 ENABL-STATUS-TEST-01.                                            CM1024.2
044300     MOVE "DESTINATION NOT SPECIFIED" TO RE-MARK.                 CM1024.2
044400     MOVE SPACES TO SYM-DEST.                                     CM1024.2
044500     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
044600     MOVE 1 TO ONE.                                               CM1024.2
044700     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM1024.2
044800     XXXXX033.                                                    CM1024.2
044900     IF STATUS-KEY IS EQUAL TO "20"                               CM1024.2
045000         AND ERR-KEY IS EQUAL TO "1"                              CM1024.2
045100         PERFORM PASS GO TO ENABL-STATUS-WRITE-01.                CM1024.2
045200     MOVE 201 TO CORRECT-2SLASH1.                                 CM1024.2
045300     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
045400     MOVE "/" TO SLASH.                                           CM1024.2
045500     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
045600     PERFORM FAIL.                                                CM1024.2
045700     GO TO ENABL-STATUS-WRITE-01.                                 CM1024.2
045800 ENABL-STATUS-DELETE-01.                                          CM1024.2
045900     PERFORM DE-LETE.                                             CM1024.2
046000 ENABL-STATUS-WRITE-01.                                           CM1024.2
046100     MOVE "ENABL-STATUS-TEST-01" TO PAR-NAME.                     CM1024.2
046200     PERFORM PRINT-DETAIL.                                        CM1024.2
046300 ENABL-STATUS-TEST-02.                                            CM1024.2
046400     MOVE "INVALID DESTINATION COUNT (0)" TO RE-MARK.             CM1024.2
046500     MOVE                                                         CM1024.2
046600     XXXXX032                                                     CM1024.2
046700         TO SYM-DEST.                                             CM1024.2
046800     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
046900     MOVE 0 TO ONE.                                               CM1024.2
047000     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM1024.2
047100     XXXXX033.                                                    CM1024.2
047200     IF STATUS-KEY IS EQUAL TO "30"                               CM1024.2
047300         PERFORM PASS GO TO ENABL-STATUS-WRITE-02.                CM1024.2
047400     MOVE 300 TO CORRECT-2SLASH1.                                 CM1024.2
047500     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
047600     MOVE "/" TO SLASH.                                           CM1024.2
047700     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
047800     PERFORM FAIL.                                                CM1024.2
047900     GO TO ENABL-STATUS-WRITE-02.                                 CM1024.2
048000 ENABL-STATUS-DELETE-02.                                          CM1024.2
048100     PERFORM DE-LETE.                                             CM1024.2
048200 ENABL-STATUS-WRITE-02.                                           CM1024.2
048300     MOVE "ENABL-STATUS-TEST-02" TO PAR-NAME.                     CM1024.2
048400     PERFORM PRINT-DETAIL.                                        CM1024.2
048500 ENABL-STATUS-TEST-03.                                            CM1024.2
048600     MOVE "INVALID PASSWORD USED" TO RE-MARK.                     CM1024.2
048700     MOVE                                                         CM1024.2
048800     XXXXX032                                                     CM1024.2
048900         TO SYM-DEST.                                             CM1024.2
049000     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
049100     MOVE 1 TO ONE.                                               CM1024.2
049200     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM1024.2
049300         "GARBAGE".                                               CM1024.2
049400     IF STATUS-KEY IS EQUAL TO "40"                               CM1024.2
049500         PERFORM PASS GO TO ENABL-STATUS-WRITE-03.                CM1024.2
049600     MOVE 400 TO CORRECT-2SLASH1.                                 CM1024.2
049700     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
049800     MOVE "/" TO SLASH.                                           CM1024.2
049900     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
050000     PERFORM FAIL.                                                CM1024.2
050100     GO TO ENABL-STATUS-WRITE-03.                                 CM1024.2
050200 ENABL-STATUS-DELETE-03.                                          CM1024.2
050300     PERFORM DE-LETE.                                             CM1024.2
050400 ENABL-STATUS-WRITE-03.                                           CM1024.2
050500     MOVE "ENABL-STATUS-TEST-03" TO PAR-NAME.                     CM1024.2
050600     PERFORM PRINT-DETAIL.                                        CM1024.2
050700 ENABL-STATUS-TEST-04.                                            CM1024.2
050800     MOVE "VALID ENABLE/NO ERROR EXPECTED" TO RE-MARK.            CM1024.2
050900     MOVE                                                         CM1024.2
051000     XXXXX032                                                     CM1024.2
051100         TO SYM-DEST.                                             CM1024.2
051200     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
051300     MOVE 1 TO ONE.                                               CM1024.2
051400     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM1024.2
051500     XXXXX033.                                                    CM1024.2
051600     IF STATUS-KEY IS EQUAL TO ZERO                               CM1024.2
051700         PERFORM PASS GO TO ENABL-STATUS-WRITE-04.                CM1024.2
051800     MOVE 0 TO CORRECT-2SLASH1.                                   CM1024.2
051900     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
052000     MOVE "/" TO SLASH.                                           CM1024.2
052100     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
052200     PERFORM FAIL.                                                CM1024.2
052300     GO TO ENABL-STATUS-WRITE-04.                                 CM1024.2
052400 ENABL-STATUS-DELETE-04.                                          CM1024.2
052500     PERFORM DE-LETE.                                             CM1024.2
052600 ENABL-STATUS-WRITE-04.                                           CM1024.2
052700     MOVE "ENABL-STATUS-TEST-04" TO PAR-NAME.                     CM1024.2
052800     PERFORM PRINT-DETAIL.                                        CM1024.2
052900 SEND-STATUS-TEST-03.                                             CM1024.2
053000     MOVE "DESTINATION UNKNOWN" TO RE-MARK.                       CM1024.2
053100     MOVE "GARBAGE" TO SYM-DEST.                                  CM1024.2
053200     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
053300     MOVE 1 TO ONE.                                               CM1024.2
053400     MOVE 37 TO MSG-LENGTH.                                       CM1024.2
053500     MOVE "S-03" TO TEST-IND.                                     CM1024.2
053600     SEND CM-OUTQUE-1 FROM ERR-MSG WITH EMI.                      CM1024.2
053700     IF STATUS-KEY IS EQUAL TO "20"                               CM1024.2
053800         AND ERR-KEY IS EQUAL TO "1"                              CM1024.2
053900         PERFORM PASS GO TO SEND-STATUS-WRITE-03.                 CM1024.2
054000     MOVE 201 TO CORRECT-2SLASH1.                                 CM1024.2
054100     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
054200     MOVE "/" TO SLASH.                                           CM1024.2
054300     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
054400     PERFORM FAIL.                                                CM1024.2
054500     GO TO SEND-STATUS-WRITE-03.                                  CM1024.2
054600 SEND-STATUS-DELETE-03.                                           CM1024.2
054700     PERFORM DE-LETE.                                             CM1024.2
054800 SEND-STATUS-WRITE-03.                                            CM1024.2
054900     MOVE "SEND-STATUS-TEST-03" TO PAR-NAME.                      CM1024.2
055000     PERFORM PRINT-DETAIL.                                        CM1024.2
055100 SEND-STATUS-TEST-04.                                             CM1024.2
055200     MOVE "DESTINATION COUNT INVALID (0)" TO RE-MARK.             CM1024.2
055300     MOVE                                                         CM1024.2
055400     XXXXX032                                                     CM1024.2
055500         TO SYM-DEST.                                             CM1024.2
055600     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
055700     MOVE 0 TO ONE.                                               CM1024.2
055800     MOVE 37 TO MSG-LENGTH.                                       CM1024.2
055900     MOVE "S-04" TO TEST-IND.                                     CM1024.2
056000     SEND CM-OUTQUE-1 FROM ERR-MSG WITH EMI.                      CM1024.2
056100     IF STATUS-KEY IS EQUAL TO "30"                               CM1024.2
056200         PERFORM PASS GO TO SEND-STATUS-WRITE-04.                 CM1024.2
056300     MOVE 300 TO CORRECT-2SLASH1.                                 CM1024.2
056400     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
056500     MOVE "/" TO SLASH.                                           CM1024.2
056600     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
056700     PERFORM FAIL.                                                CM1024.2
056800     GO TO SEND-STATUS-WRITE-04.                                  CM1024.2
056900 SEND-STATUS-DELETE-04.                                           CM1024.2
057000     PERFORM DE-LETE.                                             CM1024.2
057100 SEND-STATUS-WRITE-04.                                            CM1024.2
057200     MOVE "SEND-STATUS-TEST-04" TO PAR-NAME.                      CM1024.2
057300     PERFORM PRINT-DETAIL.                                        CM1024.2
057400 SEND-STATUS-TEST-05.                                             CM1024.2
057500     MOVE "CHARACTER COUNT EXCESSIVE" TO RE-MARK.                 CM1024.2
057600     MOVE                                                         CM1024.2
057700     XXXXX032                                                     CM1024.2
057800         TO SYM-DEST.                                             CM1024.2
057900     MOVE "9" TO STATUS-KEY ERR-KEY.                              CM1024.2
058000     MOVE 1 TO ONE.                                               CM1024.2
058100     MOVE 38 TO MSG-LENGTH.                                       CM1024.2
058200     MOVE "S-05" TO TEST-IND.                                     CM1024.2
058300     SEND CM-OUTQUE-1 FROM ERR-MSG WITH EMI.                      CM1024.2
058400     IF STATUS-KEY IS EQUAL TO "50"                               CM1024.2
058500         PERFORM PASS GO TO SEND-STATUS-WRITE-05.                 CM1024.2
058600     MOVE 500 TO CORRECT-2SLASH1.                                 CM1024.2
058700     MOVE STATUS-KEY TO COMPUTED-STATUS.                          CM1024.2
058800     MOVE "/" TO SLASH.                                           CM1024.2
058900     MOVE ERR-KEY TO COMPUTED-ERR-KEY.                            CM1024.2
059000     PERFORM FAIL.                                                CM1024.2
059100     GO TO SEND-STATUS-WRITE-05.                                  CM1024.2
059200 SEND-STATUS-DELETE-05.                                           CM1024.2
059300     PERFORM DE-LETE.                                             CM1024.2
059400 SEND-STATUS-WRITE-05.                                            CM1024.2
059500     MOVE "SEND-STATUS-TEST-05" TO PAR-NAME.                      CM1024.2
059600     PERFORM PRINT-DETAIL.                                        CM1024.2
059700 STATUS-TESTS-COMPLETED.                                          CM1024.2
059800     PERFORM END-ROUTINE.                                         CM1024.2
059900     PERFORM END-ROUTINE-1 THRU END-ROUTINE-3.                    CM1024.2
060000     PERFORM END-ROUTINE.                                         CM1024.2
060100     MOVE LOG-HDR-1 TO PRINT-REC.                                 CM1024.2
060200     WRITE PRINT-REC                                              CM1024.2
060300         AFTER 3 LINES.                                           CM1024.2
060400     MOVE LOG-HDR-2 TO PRINT-REC.                                 CM1024.2
060500     WRITE PRINT-REC                                              CM1024.2
060600         AFTER 3 LINES.                                           CM1024.2
060700     MOVE LOG-HDR-3 TO PRINT-REC.                                 CM1024.2
060800     WRITE PRINT-REC.                                             CM1024.2
060900     PERFORM BLANK-LINE-PRINT.                                    CM1024.2
061000 VARIABLE-LENGTH-MSGS.                                            CM1024.2
061100     MOVE 1 TO ONE.                                               CM1024.2
061200     MOVE                                                         CM1024.2
061300     XXXXX032                                                     CM1024.2
061400         TO SYM-DEST.                                             CM1024.2
061500     MOVE 1 TO MSG-NO SEND-SWITCH.                                CM1024.2
061600     MOVE 28 TO MSG-LENGTH.                                       CM1024.2
061700     MOVE MSG-A TO MSG-OUT.                                       CM1024.2
061800     PERFORM SEND-AND-LOG.                                        CM1024.2
061900     MOVE 2 TO MSG-NO.                                            CM1024.2
062000     MOVE 27 TO MSG-LENGTH.                                       CM1024.2
062100     MOVE MSG-B TO MSG-OUT.                                       CM1024.2
062200     PERFORM SEND-AND-LOG.                                        CM1024.2
062300     MOVE 3 TO MSG-NO.                                            CM1024.2
062400     MOVE 26 TO MSG-LENGTH.                                       CM1024.2
062500     MOVE MSG-C TO MSG-OUT.                                       CM1024.2
062600     PERFORM SEND-AND-LOG.                                        CM1024.2
062700     MOVE 4 TO MSG-NO.                                            CM1024.2
062800     MOVE 25 TO MSG-LENGTH.                                       CM1024.2
062900     MOVE MSG-D TO MSG-OUT.                                       CM1024.2
063000     PERFORM SEND-AND-LOG.                                        CM1024.2
063100     MOVE 2 TO SEND-SWITCH.                                       CM1024.2
063200     MOVE 5 TO MSG-NO.                                            CM1024.2
063300     MOVE 24 TO MSG-LENGTH.                                       CM1024.2
063400     MOVE MSG-E TO MSG-OUT.                                       CM1024.2
063500     PERFORM SEND-AND-LOG.                                        CM1024.2
063600 AFTER-PAGE-MSGS.                                                 CM1024.2
063700     MOVE 6 TO MSG-NUM.                                           CM1024.2
063800     MOVE 3 TO SEND-SWITCH.                                       CM1024.2
063900     MOVE 63 TO MSG-LENGTH.                                       CM1024.2
064000     PERFORM AFTER-PAGE-MSGS-01 5 TIMES.                          CM1024.2
064100     GO TO AFTER-THREE-MSGS.                                      CM1024.2
064200 AFTER-PAGE-MSGS-01.                                              CM1024.2
064300     MOVE MSG-NUM TO MSG-F-NO.                                    CM1024.2
064400     ADD 1 TO MSG-NUM.                                            CM1024.2
064500     MOVE MSG-F TO MSG-OUT.                                       CM1024.2
064600     PERFORM SEND-AND-LOG.                                        CM1024.2
064700 AFTER-THREE-MSGS.                                                CM1024.2
064800     MOVE 64 TO MSG-LENGTH.                                       CM1024.2
064900     PERFORM AFTER-THREE-MSGS-01 5 TIMES.                         CM1024.2
065000     GO TO EGI-ONLY.                                              CM1024.2
065100 AFTER-THREE-MSGS-01.                                             CM1024.2
065200     MOVE MSG-NUM TO MSG-G-NO.                                    CM1024.2
065300     ADD 1 TO MSG-NUM SEND-SWITCH.                                CM1024.2
065400     MOVE MSG-G TO MSG-OUT.                                       CM1024.2
065500     PERFORM SEND-AND-LOG.                                        CM1024.2
065600 EGI-ONLY.                                                        CM1024.2
065700     MOVE "ONLY EGI WAS SENT.  NO MESSAGE ACCOMPANYING" TO MSG-OUTCM1024.2
065800     ADD 1 TO SEND-SWITCH.                                        CM1024.2
065900     MOVE 0 TO MSG-LENGTH.                                        CM1024.2
066000     PERFORM SEND-AND-LOG.                                        CM1024.2
066100 BEFORE-ADV-INIT.                                                 CM1024.2
066200     MOVE "0LTH" TO TEST-IND.                                     CM1024.2
066300     ADD 1 TO SEND-SWITCH.                                        CM1024.2
066400     MOVE ERR-MSG TO MSG-OUT.                                     CM1024.2
066500     PERFORM SEND-AND-LOG.                                        CM1024.2
066600 BEFORE-PAGE-MSGS.                                                CM1024.2
066700     MOVE 63 TO MSG-LENGTH.                                       CM1024.2
066800     PERFORM AFTER-PAGE-MSGS-01 5 TIMES.                          CM1024.2
066900 BEFORE-TWO-MSGS.                                                 CM1024.2
067000     MOVE 64 TO MSG-LENGTH.                                       CM1024.2
067100     PERFORM BEFORE-TWO-MSGS-01 5 TIMES.                          CM1024.2
067200     GO TO ZERO-LINES-MSGS.                                       CM1024.2
067300 BEFORE-TWO-MSGS-01.                                              CM1024.2
067400     MOVE MSG-NUM TO MSG-H-NO.                                    CM1024.2
067500     ADD 1 TO MSG-NUM.                                            CM1024.2
067600     ADD 1 TO SEND-SWITCH.                                        CM1024.2
067700     MOVE MSG-H TO MSG-OUT.                                       CM1024.2
067800     PERFORM SEND-AND-LOG.                                        CM1024.2
067900 ZERO-LINES-MSGS.                                                 CM1024.2
068000     ADD 1 TO SEND-SWITCH.                                        CM1024.2
068100     MOVE 59 TO MSG-LENGTH.                                       CM1024.2
068200     MOVE "ALL" TO SUPERIMPOSITION.                               CM1024.2
068300     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
068400     MOVE "WORDS" TO S-WORDS.                                     CM1024.2
068500     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
068600     MOVE "IN" TO S-IN.                                           CM1024.2
068700     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
068800     MOVE "THIS" TO S-THIS.                                       CM1024.2
068900     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
069000     MOVE "MESSAGE" TO S-MESSAGE.                                 CM1024.2
069100     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
069200     MOVE "SHOULD" TO S-SHOULD.                                   CM1024.2
069300     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
069400     ADD 1 TO SEND-SWITCH.                                        CM1024.2
069500     MOVE "COME" TO S-COME.                                       CM1024.2
069600     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
069700     MOVE "OUT" TO S-OUT.                                         CM1024.2
069800     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
069900     MOVE "ON" TO S-ON.                                           CM1024.2
070000     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
070100     MOVE "THE" TO S-THE.                                         CM1024.2
070200     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
070300     MOVE "SAME" TO S-SAME.                                       CM1024.2
070400     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
070500     MOVE "LINE." TO S-LINE.                                      CM1024.2
070600     PERFORM ZERO-LINES-MSGS-01.                                  CM1024.2
070700     GO TO 433-CHARACTER-MSG.                                     CM1024.2
070800 ZERO-LINES-MSGS-01.                                              CM1024.2
070900     MOVE SUPERIMPOSITION TO MSG-OUT.                             CM1024.2
071000     PERFORM SEND-AND-LOG.                                        CM1024.2
071100     MOVE SPACES TO SUPERIMPOSITION.                              CM1024.2
071200 433-CHARACTER-MSG.                                               CM1024.2
071300     ADD 1 TO SEND-SWITCH.                                        CM1024.2
071400     MOVE 433 TO MSG-LENGTH.                                      CM1024.2
071500     MOVE LONG-MSG-S1 TO MSG-OUT.                                 CM1024.2
071600     PERFORM SEND-AND-LOG.                                        CM1024.2
071700     MOVE LONG-MSG-S2 TO MSG-FLD.                                 CM1024.2
071800     WRITE PRINT-REC FROM LOG-LINE-1.                             CM1024.2
071900     MOVE LONG-MSG-S3 TO MSG-FLD.                                 CM1024.2
072000     WRITE PRINT-REC FROM LOG-LINE-1.                             CM1024.2
072100     MOVE LONG-MSG-S4 TO MSG-FLD.                                 CM1024.2
072200     WRITE PRINT-REC FROM LOG-LINE-1.                             CM1024.2
072300     MOVE LONG-MSG-S5 TO MSG-FLD.                                 CM1024.2
072400     WRITE PRINT-REC FROM LOG-LINE-1.                             CM1024.2
072500     MOVE LONG-MSG-S6 TO MSG-FLD.                                 CM1024.2
072600     WRITE PRINT-REC FROM LOG-LINE-1.                             CM1024.2
072700     MOVE LONG-MSG-S7 TO MSG-FLD.                                 CM1024.2
072800     WRITE PRINT-REC FROM LOG-LINE-1.                             CM1024.2
072900 MSG-BEFORE-DELAY-AND-DISABLE.                                    CM1024.2
073000     MOVE "EXPECT A PAUSE OF UP TO 30 SECONDS BEFORE TRANSMISSION CM1024.2
073100-        "OF NEXT MESSAGE." TO MSG-OUT.                           CM1024.2
073200     MOVE 72 TO MSG-LENGTH.                                       CM1024.2
073300     MOVE 4 TO SEND-SWITCH.                                       CM1024.2
073400     PERFORM SEND-AND-LOG.                                        CM1024.2
073500 DELAY-FOR-30-SECS.                                               CM1024.2
073600     ACCEPT SYSTEM-TIME FROM TIME.                                CM1024.2
073700     IF (HOURS OF SYSTEM-TIME * 3600 + MINUTES OF SYSTEM-TIME * 60CM1024.2
073800         + SECONDS OF SYSTEM-TIME) - (COMP-HRS * 3600 + COMP-MINS CM1024.2
073900         * 60 + COMP-SECS) IS LESS THAN 30                        CM1024.2
074000         GO TO DELAY-FOR-30-SECS.                                 CM1024.2
074100 DISABLE-DEVICE.                                                  CM1024.2
074200     MOVE "****  DEVICE DISABLED  ****" TO MSG-OUT.               CM1024.2
074300     MOVE 0 TO MSG-LENGTH.                                        CM1024.2
074400     MOVE 19 TO SEND-SWITCH.                                      CM1024.2
074500     PERFORM SEND-AND-LOG.                                        CM1024.2
074600 10-WHILE-DISABLED.                                               CM1024.2
074700     MOVE "TRANSMISSION NOW RESUMED." TO MSG-OUT.                 CM1024.2
074800     MOVE 25 TO MSG-LENGTH.                                       CM1024.2
074900     MOVE 1 TO SEND-SWITCH.                                       CM1024.2
075000     PERFORM SEND-AND-LOG.                                        CM1024.2
075100     MOVE 24 TO MSG-LENGTH.                                       CM1024.2
075200     PERFORM 10-WHILE-DISABLED-01 8 TIMES.                        CM1024.2
075300     GO TO 10-WHILE-DISABLED-02.                                  CM1024.2
075400 10-WHILE-DISABLED-01.                                            CM1024.2
075500     MOVE MSG-NUM TO MSG-NO.                                      CM1024.2
075600     ADD 1 TO MSG-NUM.                                            CM1024.2
075700     MOVE MSG-E TO MSG-OUT.                                       CM1024.2
075800     PERFORM SEND-AND-LOG.                                        CM1024.2
075900 10-WHILE-DISABLED-02.                                            CM1024.2
076000     MOVE "THERE SHOULD BE NO ABNORMAL DELAY IN RECEIVING THE NEXTCM1024.2
076100-        " MESSAGE." TO MSG-OUT.                                  CM1024.2
076200     MOVE 63 TO MSG-LENGTH.                                       CM1024.2
076300     PERFORM SEND-AND-LOG.                                        CM1024.2
076400 RE-ENABLE-OUTQUE.                                                CM1024.2
076500     MOVE "****  DEVICE NOW RE-ENABLED  ****" TO MSG-OUT.         CM1024.2
076600     MOVE 0 TO MSG-LENGTH.                                        CM1024.2
076700     MOVE 20 TO SEND-SWITCH.                                      CM1024.2
076800     PERFORM SEND-AND-LOG.                                        CM1024.2
076900 ENQUEUE-500-MORE.                                                CM1024.2
077000     MOVE "THIS IS THAT NEXT MESSAGE." TO MSG-OUT.                CM1024.2
077100     MOVE 26 TO MSG-LENGTH.                                       CM1024.2
077200     MOVE 2 TO SEND-SWITCH.                                       CM1024.2
077300     PERFORM SEND-AND-LOG.                                        CM1024.2
077400     MOVE 24 TO MSG-LENGTH.                                       CM1024.2
077500     PERFORM 10-WHILE-DISABLED-01 500 TIMES.                      CM1024.2
077600 DELAY-DISABLE-DELAY-AND-STOP.                                    CM1024.2
077700     PERFORM DELAY-FOR-30-SECS.                                   CM1024.2
077800     PERFORM DISABLE-DEVICE.                                      CM1024.2
077900     PERFORM DELAY-FOR-30-SECS.                                   CM1024.2
078000     PERFORM END-ROUTINE THRU PARA-Z.                             CM1024.2
078100     PERFORM END-ROUTINE-4.                                       CM1024.2
078200     CLOSE PRINT-FILE.                                            CM1024.2
078300     STOP RUN.                                                    CM1024.2
078400 SEND-AND-LOG.                                                    CM1024.2
078500     ACCEPT SYSTEM-TIME FROM TIME.                                CM1024.2
078600     PERFORM UNIFORM-SEND.                                        CM1024.2
078700     ACCEPT COMP-TIME FROM TIME.                                  CM1024.2
078800     MOVE CORR SYSTEM-TIME TO START-TIME.                         CM1024.2
078900     COMPUTE ELAPSED =                                            CM1024.2
079000         (COMP-HRS * 3600 + COMP-MINS * 60 + COMP-SECS) -         CM1024.2
079100         (HOURS OF SYSTEM-TIME * 3600 + MINUTES OF SYSTEM-TIME *  CM1024.2
079200         60 + SECONDS OF SYSTEM-TIME).                            CM1024.2
079300     MOVE STATUS-KEY TO STAT.                                     CM1024.2
079400     MOVE ERR-KEY TO ERR.                                         CM1024.2
079500     MOVE MSG-LENGTH TO LNTH.                                     CM1024.2
079600     MOVE LOG-LINE TO PRINT-REC.                                  CM1024.2
079700     PERFORM WRITE-LINE.                                          CM1024.2
079800 UNIFORM-SEND SECTION.                                            CM1024.2
079900 UNIFORM-SEND-SWITCH.                                             CM1024.2
080000     GO TO                                                        CM1024.2
080100         SEND-EMI-A1                                              CM1024.2
080200         SEND-EGI-A1                                              CM1024.2
080300         SEND-EMI-AP                                              CM1024.2
080400         SEND-EMI-A3-01                                           CM1024.2
080500         SEND-EMI-A3-02                                           CM1024.2
080600         SEND-EMI-A3-03                                           CM1024.2
080700         SEND-EMI-A3-04                                           CM1024.2
080800         SEND-EMI-A3-05                                           CM1024.2
080900         SEND-EGI-ONLY                                            CM1024.2
081000         SEND-EMI-BP                                              CM1024.2
081100         SEND-EMI-B2-01                                           CM1024.2
081200         SEND-EMI-B2-02                                           CM1024.2
081300         SEND-EMI-B2-03                                           CM1024.2
081400         SEND-EMI-B2-04                                           CM1024.2
081500         SEND-EMI-B2-05                                           CM1024.2
081600         SEND-EMI-A0                                              CM1024.2
081700         SEND-EMI-B0                                              CM1024.2
081800         SEND-LONG-MSG                                            CM1024.2
081900         DISABLE-OUTQUE                                           CM1024.2
082000         ENABLE-OUTQUE                                            CM1024.2
082100             DEPENDING ON SEND-SWITCH.                            CM1024.2
082200 SEND-EMI-A1.                                                     CM1024.2
082300     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI.                      CM1024.2
082400     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
082500 SEND-EGI-A1.                                                     CM1024.2
082600     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EGI.                      CM1024.2
082700     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
082800 SEND-EMI-AP.                                                     CM1024.2
082900     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI AFTER PAGE.           CM1024.2
083000     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
083100 SEND-EMI-A3-01.                                                  CM1024.2
083200     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI AFTER ADVANCING 3     CM1024.2
083300         LINES.                                                   CM1024.2
083400     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
083500 SEND-EMI-A3-02.                                                  CM1024.2
083600     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
083700         AFTER ADVANCING THREE LINES.                             CM1024.2
083800     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
083900 SEND-EMI-A3-03.                                                  CM1024.2
084000     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
084100         AFTER 3 LINE.                                            CM1024.2
084200     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
084300 SEND-EMI-A3-04.                                                  CM1024.2
084400     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
084500         AFTER COMP-THREE.                                        CM1024.2
084600     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
084700 SEND-EMI-A3-05.                                                  CM1024.2
084800     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
084900         AFTER 3.                                                 CM1024.2
085000     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
085100 SEND-EGI-ONLY.                                                   CM1024.2
085200     SEND CM-OUTQUE-1 WITH EGI.                                   CM1024.2
085300     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
085400 SEND-EMI-BP.                                                     CM1024.2
085500     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
085600         BEFORE ADVANCING PAGE.                                   CM1024.2
085700     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
085800 SEND-EMI-B2-01.                                                  CM1024.2
085900     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
086000         BEFORE ADVANCING 2 LINES.                                CM1024.2
086100     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
086200 SEND-EMI-B2-02.                                                  CM1024.2
086300     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
086400         BEFORE ADVANCING TWO LINES.                              CM1024.2
086500     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
086600 SEND-EMI-B2-03.                                                  CM1024.2
086700     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
086800         BEFORE 2 LINE.                                           CM1024.2
086900     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
087000 SEND-EMI-B2-04.                                                  CM1024.2
087100     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
087200         BEFORE COMP-TWO.                                         CM1024.2
087300     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
087400 SEND-EMI-B2-05.                                                  CM1024.2
087500     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
087600         BEFORE 2.                                                CM1024.2
087700     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
087800 SEND-EMI-A0.                                                     CM1024.2
087900     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
088000         AFTER 0 LINES.                                           CM1024.2
088100     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
088200 SEND-EMI-B0.                                                     CM1024.2
088300     SEND CM-OUTQUE-1 FROM MSG-OUT WITH EMI                       CM1024.2
088400         BEFORE ZERO LINES.                                       CM1024.2
088500     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
088600 SEND-LONG-MSG.                                                   CM1024.2
088700     SEND CM-OUTQUE-1 FROM LONG-MSG WITH EMI AFTER PAGE.          CM1024.2
088800     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
088900 DISABLE-OUTQUE.                                                  CM1024.2
089000     DISABLE OUTPUT CM-OUTQUE-1 KEY                               CM1024.2
089100     PASSWORD1.                                                   CM1024.2
089200     GO TO UNIFORM-SEND-EXIT.                                     CM1024.2
089300 ENABLE-OUTQUE.                                                   CM1024.2
089400     ENABLE OUTPUT CM-OUTQUE-1 WITH KEY                           CM1024.2
089500     XXXXX033.                                                    CM1024.2
089600 UNIFORM-SEND-EXIT.                                               CM1024.2
089700     EXIT.                                                        CM1024.2
089800 COMMON-SUBROUTINES SECTION.                                      CM1024.2
089900 PASS.                                                            CM1024.2
090000     MOVE "PASS" TO P-OR-F.                                       CM1024.2
090100 FAIL.                                                            CM1024.2
090200     ADD      1 TO ERROR-COUNTER.                                 CM1024.2
090300     MOVE "FAIL*" TO P-OR-F.                                      CM1024.2
090400 DE-LETE.                                                         CM1024.2
090500     MOVE     SPACE TO P-OR-F.                                    CM1024.2
090600     MOVE     "    ************    " TO COMPUTED-A.               CM1024.2
090700     MOVE     "    ************    " TO CORRECT-A.                CM1024.2
090800     MOVE "****TEST DELETED****" TO RE-MARK.                      CM1024.2
090900     ADD 1 TO DELETE-CNT.                                         CM1024.2
091000 PRINT-DETAIL.                                                    CM1024.2
091100     MOVE     TEST-RESULTS TO PRINT-REC.                          CM1024.2
091200     PERFORM WRITE-LINE.                                          CM1024.2
091300     MOVE     SPACE TO P-OR-F.                                    CM1024.2
091400     MOVE     SPACE TO COMPUTED-A.                                CM1024.2
091500     MOVE SPACE TO CORRECT-A.                                     CM1024.2
091600 COLUMN-NAMES-ROUTINE.                                            CM1024.2
091700     MOVE     COLUMNS-LINE-1 TO DUMMY-RECORD.                     CM1024.2
091800     PERFORM WRITE-LINE.                                          CM1024.2
091900     MOVE     COLUMNS-LINE-2 TO DUMMY-RECORD.                     CM1024.2
092000     PERFORM WRITE-LINE.                                          CM1024.2
092100     PERFORM  BLANK-LINE-PRINT.                                   CM1024.2
092200 END-ROUTINE.                                                     CM1024.2
092300     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        CM1024.2
092400     PERFORM WRITE-LINE.                                          CM1024.2
092500 PARA-Z.                                                          CM1024.2
092600     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           CM1024.2
092700     MOVE     CCVS-E-1 TO DUMMY-RECORD.                           CM1024.2
092800     PERFORM WRITE-LINE.                                          CM1024.2
092900 END-ROUTINE-1.                                                   CM1024.2
093000     PERFORM  BLANK-LINE-PRINT.                                   CM1024.2
093100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      CM1024.2
093200              GO TO END-ROUTINE-2.                                CM1024.2
093300     MOVE     ERROR-COUNTER TO ERROR-TOTAL.                       CM1024.2
093400     GO TO    END-ROUTINE-3.                                      CM1024.2
093500 END-ROUTINE-2.                                                   CM1024.2
093600     MOVE " NO" TO ERROR-TOTAL.                                   CM1024.2
093700 END-ROUTINE-3.                                                   CM1024.2
093800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           CM1024.2
093900     PERFORM WRITE-LINE.                                          CM1024.2
094000     IF DELETE-CNT IS EQUAL TO ZERO                               CM1024.2
094100         MOVE " NO" TO ERROR-TOTAL  ELSE                          CM1024.2
094200     MOVE DELETE-CNT TO ERROR-TOTAL.                              CM1024.2
094300     MOVE "TESTS DELETED     " TO ENDER-DESC.                     CM1024.2
094400     MOVE CCVS-E-2 TO DUMMY-RECORD.                               CM1024.2
094500     PERFORM WRITE-LINE.                                          CM1024.2
094600 END-ROUTINE-4.                                                   CM1024.2
094700     MOVE CCVS-E-3 TO DUMMY-RECORD.                               CM1024.2
094800     PERFORM WRITE-LINE.                                          CM1024.2
094900 BLANK-LINE-PRINT.                                                CM1024.2
095000     MOVE     SPACE TO DUMMY-RECORD.                              CM1024.2
095100     PERFORM WRITE-LINE.                                          CM1024.2
095200 WRITE-LINE.                                                      CM1024.2
095300     WRITE DUMMY-RECORD AFTER ADVANCING 1 LINE.                   CM1024.2
095400 HEAD-ROUTINE.                                                    CM1024.2
095500     MOVE CCVS-H-1 TO PRINT-REC                                   CM1024.2
095600     WRITE PRINT-REC                                              CM1024.2
095700         AFTER ADVANCING PAGE.                                    CM1024.2
095800     MOVE CCVS-H-2 TO PRINT-REC.                                  CM1024.2
095900     WRITE PRINT-REC                                              CM1024.2
096000         AFTER 2 LINES.                                           CM1024.2
096100     MOVE CCVS-H-3 TO PRINT-REC.                                  CM1024.2
096200     WRITE PRINT-REC                                              CM1024.2
096300         AFTER 5 LINES.                                           CM1024.2
096400     MOVE HYPHEN-LINE TO PRINT-REC.                               CM1024.2
096500     PERFORM WRITE-LINE.                                          CM1024.2
      *END-OF,CM102M                                                                  
