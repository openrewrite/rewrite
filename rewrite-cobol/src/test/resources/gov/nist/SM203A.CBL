      *HEADER,COBOL,SM203A                                                            
000100 IDENTIFICATION DIVISION.                                         SM2034.2
000200 PROGRAM-ID.                                                      SM2034.2
000300     SM203A.                                                      SM2034.2
000400****************************************************************  SM2034.2
000500*                                                              *  SM2034.2
000600*    VALIDATION FOR:-                                          *  SM2034.2
000700*                                                              *  SM2034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2034.2
000900*                                                              *  SM2034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2034.2
001100*                                                              *  SM2034.2
001200****************************************************************  SM2034.2
001300*                                                              *  SM2034.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2034.2
001500*                                                              *  SM2034.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2034.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2034.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2034.2
001900*                                                              *  SM2034.2
002000****************************************************************  SM2034.2
002100*                                                              *  SM2034.2
002200*    PROGRAM SM203A TESTS THE USE OF THE "COPY" STATEMENT      *  SM2034.2
002300*    "REPLACING" PHRASE IN THE ENVIRONMENT DIVISION.           *  SM2034.2
002400*    A SEQUENTIAL FILE IS PRODUCED USING "COPY"ED TEXT AND     *  SM2034.2
002500*    THIS IS CHECKED IN PROGRAM SM204A.                        *  SM2034.2
002600*                                                              *  SM2034.2
002700****************************************************************  SM2034.2
002800 ENVIRONMENT DIVISION.                                            SM2034.2
002900 CONFIGURATION SECTION.                                           SM2034.2
003000 SOURCE-COMPUTER.                                                 SM2034.2
003100     XXXXX082.                                                    SM2034.2
003200 OBJECT-COMPUTER.                                                 SM2034.2
003300     XXXXX083.                                                    SM2034.2
003400ASPECIAL-NAMES.                                        COPY K3SNB SM2034.2
003500A             REPLACING DUMMY-SW-1 BY SW-1                        SM2034.2
003600A                       DUMMY-ON BY SWITCH-ON                     SM2034.2
003700A                       DUMMY-OFF BY SWITCH-OFF.                  SM2034.2
003800 INPUT-OUTPUT SECTION.                                            SM2034.2
003900                                                                  SM2034.2
004000                                                                  SM2034.2
004100                                                                  SM2034.2
004200                                                                  SM2034.2
004300                                                                  SM2034.2
004400*                                                                 SM2034.2
004500*********************** COPY STATEMENT USED **********************SM2034.2
004600*                                                                 SM2034.2
004700*FILE-CONTROL.                                         COPY K3FCB SM2034.2
004800*             REPLACING DUMMY-TEST-FILE BY TEST-FILE.             SM2034.2
004900*                                                                 SM2034.2
005000******************** COPIED TEXT BEGINS BELOW ********************SM2034.2
005100 FILE-CONTROL.                                         COPY K3FCB SM2034.2
005200              REPLACING DUMMY-TEST-FILE BY TEST-FILE.             SM2034.2
005300*********************** END OF COPIED TEXT ***********************SM2034.2
005400                                                                  SM2034.2
005500                                                                  SM2034.2
005600                                                                  SM2034.2
005700                                                                  SM2034.2
005800                                                                  SM2034.2
005900*                                                                 SM2034.2
006000*********************** COPY STATEMENT USED **********************SM2034.2
006100*                                                                 SM2034.2
006200*I-O-CONTROL.                                          COPY K3IOB SM2034.2
006300*             REPLACING DUMMY-PRINT-FILE BY PRINT-FILE.           SM2034.2
006400*                                                                 SM2034.2
006500******************** COPIED TEXT BEGINS BELOW ********************SM2034.2
006600 I-O-CONTROL.                                          COPY K3IOB SM2034.2
006700              REPLACING DUMMY-PRINT-FILE BY PRINT-FILE.           SM2034.2
006800*********************** END OF COPIED TEXT ***********************SM2034.2
006900 DATA DIVISION.                                                   SM2034.2
007000 FILE SECTION.                                                    SM2034.2
007100 FD  PRINT-FILE.                                                  SM2034.2
007200 01  PRINT-REC PICTURE X(120).                                    SM2034.2
007300 01  DUMMY-RECORD PICTURE X(120).                                 SM2034.2
007400 FD  TEST-FILE                                                    SM2034.2
007500     LABEL RECORD STANDARD                                        SM2034.2
007600C    VALUE OF                                                     SM2034.2
007700C    XXXXX074                                                     SM2034.2
007800C    IS                                                           SM2034.2
007900C    XXXXX077                                                     SM2034.2
008000*    XXXXX069                                                     SM2034.2
008100     DATA RECORD IS PROOF-REC.                                    SM2034.2
008200 01  PROOF-REC.                                                   SM2034.2
008300     02  TF-1 PICTURE 9(5).                                       SM2034.2
008400     02  FILLER PICTURE X(115).                                   SM2034.2
008500 WORKING-STORAGE SECTION.                                         SM2034.2
008600 77  RCD-1 PICTURE 9(5) VALUE 97532.                              SM2034.2
008700 77  RCD-2 PICTURE 9(5) VALUE 23479.                              SM2034.2
008800 77  RCD-3 PICTURE 9(5) VALUE 10901.                              SM2034.2
008900 77  RCD-4 PICTURE 9(5) VALUE 02734.                              SM2034.2
009000 77  RCD-5 PICTURE 9(5) VALUE 14003.                              SM2034.2
009100 77  RCD-6 PICTURE 9(5) VALUE 19922.                              SM2034.2
009200 77  RCD-7 PICTURE 9(5) VALUE 03543.                              SM2034.2
009300 01  TEST-RESULTS.                                                SM2034.2
009400     02 FILLER                   PIC X      VALUE SPACE.          SM2034.2
009500     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2034.2
009600     02 FILLER                   PIC X      VALUE SPACE.          SM2034.2
009700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2034.2
009800     02 FILLER                   PIC X      VALUE SPACE.          SM2034.2
009900     02  PAR-NAME.                                                SM2034.2
010000       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2034.2
010100       03  PARDOT-X              PIC X      VALUE SPACE.          SM2034.2
010200       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2034.2
010300     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2034.2
010400     02 RE-MARK                  PIC X(61).                       SM2034.2
010500 01  TEST-COMPUTED.                                               SM2034.2
010600     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2034.2
010700     02 FILLER                   PIC X(17)  VALUE                 SM2034.2
010800            "       COMPUTED=".                                   SM2034.2
010900     02 COMPUTED-X.                                               SM2034.2
011000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2034.2
011100     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2034.2
011200                                 PIC -9(9).9(9).                  SM2034.2
011300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2034.2
011400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2034.2
011500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2034.2
011600     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2034.2
011700         04 COMPUTED-18V0                    PIC -9(18).          SM2034.2
011800         04 FILLER                           PIC X.               SM2034.2
011900     03 FILLER PIC X(50) VALUE SPACE.                             SM2034.2
012000 01  TEST-CORRECT.                                                SM2034.2
012100     02 FILLER PIC X(30) VALUE SPACE.                             SM2034.2
012200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2034.2
012300     02 CORRECT-X.                                                SM2034.2
012400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2034.2
012500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2034.2
012600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2034.2
012700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2034.2
012800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2034.2
012900     03      CR-18V0 REDEFINES CORRECT-A.                         SM2034.2
013000         04 CORRECT-18V0                     PIC -9(18).          SM2034.2
013100         04 FILLER                           PIC X.               SM2034.2
013200     03 FILLER PIC X(2) VALUE SPACE.                              SM2034.2
013300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2034.2
013400 01  CCVS-C-1.                                                    SM2034.2
013500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2034.2
013600-    "SS  PARAGRAPH-NAME                                          SM2034.2
013700-    "       REMARKS".                                            SM2034.2
013800     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2034.2
013900 01  CCVS-C-2.                                                    SM2034.2
014000     02 FILLER                     PIC X        VALUE SPACE.      SM2034.2
014100     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2034.2
014200     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2034.2
014300     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2034.2
014400     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2034.2
014500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2034.2
014600 01  REC-CT                        PIC 99       VALUE ZERO.       SM2034.2
014700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2034.2
014800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2034.2
014900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2034.2
015000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2034.2
015100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2034.2
015200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2034.2
015300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2034.2
015400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2034.2
015500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2034.2
015600 01  CCVS-H-1.                                                    SM2034.2
015700     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2034.2
015800     02  FILLER                    PIC X(42)    VALUE             SM2034.2
015900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2034.2
016000     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2034.2
016100 01  CCVS-H-2A.                                                   SM2034.2
016200   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2034.2
016300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2034.2
016400   02  FILLER                        PIC XXXX   VALUE             SM2034.2
016500     "4.2 ".                                                      SM2034.2
016600   02  FILLER                        PIC X(28)  VALUE             SM2034.2
016700            " COPY - NOT FOR DISTRIBUTION".                       SM2034.2
016800   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2034.2
016900                                                                  SM2034.2
017000 01  CCVS-H-2B.                                                   SM2034.2
017100   02  FILLER                        PIC X(15)  VALUE             SM2034.2
017200            "TEST RESULT OF ".                                    SM2034.2
017300   02  TEST-ID                       PIC X(9).                    SM2034.2
017400   02  FILLER                        PIC X(4)   VALUE             SM2034.2
017500            " IN ".                                               SM2034.2
017600   02  FILLER                        PIC X(12)  VALUE             SM2034.2
017700     " HIGH       ".                                              SM2034.2
017800   02  FILLER                        PIC X(22)  VALUE             SM2034.2
017900            " LEVEL VALIDATION FOR ".                             SM2034.2
018000   02  FILLER                        PIC X(58)  VALUE             SM2034.2
018100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2034.2
018200 01  CCVS-H-3.                                                    SM2034.2
018300     02  FILLER                      PIC X(34)  VALUE             SM2034.2
018400            " FOR OFFICIAL USE ONLY    ".                         SM2034.2
018500     02  FILLER                      PIC X(58)  VALUE             SM2034.2
018600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2034.2
018700     02  FILLER                      PIC X(28)  VALUE             SM2034.2
018800            "  COPYRIGHT   1985 ".                                SM2034.2
018900 01  CCVS-E-1.                                                    SM2034.2
019000     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2034.2
019100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2034.2
019200     02 ID-AGAIN                     PIC X(9).                    SM2034.2
019300     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2034.2
019400 01  CCVS-E-2.                                                    SM2034.2
019500     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2034.2
019600     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2034.2
019700     02 CCVS-E-2-2.                                               SM2034.2
019800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2034.2
019900         03 FILLER                   PIC X      VALUE SPACE.      SM2034.2
020000         03 ENDER-DESC               PIC X(44)  VALUE             SM2034.2
020100            "ERRORS ENCOUNTERED".                                 SM2034.2
020200 01  CCVS-E-3.                                                    SM2034.2
020300     02  FILLER                      PIC X(22)  VALUE             SM2034.2
020400            " FOR OFFICIAL USE ONLY".                             SM2034.2
020500     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2034.2
020600     02  FILLER                      PIC X(58)  VALUE             SM2034.2
020700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2034.2
020800     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2034.2
020900     02 FILLER                       PIC X(15)  VALUE             SM2034.2
021000             " COPYRIGHT 1985".                                   SM2034.2
021100 01  CCVS-E-4.                                                    SM2034.2
021200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2034.2
021300     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2034.2
021400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2034.2
021500     02 FILLER                       PIC X(40)  VALUE             SM2034.2
021600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2034.2
021700 01  XXINFO.                                                      SM2034.2
021800     02 FILLER                       PIC X(19)  VALUE             SM2034.2
021900            "*** INFORMATION ***".                                SM2034.2
022000     02 INFO-TEXT.                                                SM2034.2
022100       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2034.2
022200       04 XXCOMPUTED                 PIC X(20).                   SM2034.2
022300       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2034.2
022400       04 XXCORRECT                  PIC X(20).                   SM2034.2
022500     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2034.2
022600 01  HYPHEN-LINE.                                                 SM2034.2
022700     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2034.2
022800     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2034.2
022900-    "*****************************************".                 SM2034.2
023000     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2034.2
023100-    "******************************".                            SM2034.2
023200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2034.2
023300     "SM203A".                                                    SM2034.2
023400 PROCEDURE DIVISION.                                              SM2034.2
023500 CCVS1 SECTION.                                                   SM2034.2
023600 OPEN-FILES.                                                      SM2034.2
023700     OPEN     OUTPUT PRINT-FILE.                                  SM2034.2
023800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2034.2
023900     MOVE    SPACE TO TEST-RESULTS.                               SM2034.2
024000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2034.2
024100     GO TO CCVS1-EXIT.                                            SM2034.2
024200 CLOSE-FILES.                                                     SM2034.2
024300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2034.2
024400 TERMINATE-CCVS.                                                  SM2034.2
024500*    EXIT PROGRAM.                                                SM2034.2
024600*TERMINATE-CALL.                                                  SM2034.2
024700     STOP     RUN.                                                SM2034.2
024800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2034.2
024900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2034.2
025000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2034.2
025100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2034.2
025200     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2034.2
025300 PRINT-DETAIL.                                                    SM2034.2
025400     IF REC-CT NOT EQUAL TO ZERO                                  SM2034.2
025500             MOVE "." TO PARDOT-X                                 SM2034.2
025600             MOVE REC-CT TO DOTVALUE.                             SM2034.2
025700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2034.2
025800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2034.2
025900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2034.2
026000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2034.2
026100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2034.2
026200     MOVE SPACE TO CORRECT-X.                                     SM2034.2
026300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2034.2
026400     MOVE     SPACE TO RE-MARK.                                   SM2034.2
026500 HEAD-ROUTINE.                                                    SM2034.2
026600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2034.2
026700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2034.2
026800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2034.2
026900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2034.2
027000 COLUMN-NAMES-ROUTINE.                                            SM2034.2
027100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2034.2
027200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2034.2
027300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2034.2
027400 END-ROUTINE.                                                     SM2034.2
027500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2034.2
027600 END-RTN-EXIT.                                                    SM2034.2
027700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2034.2
027800 END-ROUTINE-1.                                                   SM2034.2
027900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2034.2
028000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2034.2
028100      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2034.2
028200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2034.2
028300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2034.2
028400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2034.2
028500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2034.2
028600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2034.2
028700  END-ROUTINE-12.                                                 SM2034.2
028800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2034.2
028900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2034.2
029000         MOVE "NO " TO ERROR-TOTAL                                SM2034.2
029100         ELSE                                                     SM2034.2
029200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2034.2
029300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2034.2
029400     PERFORM WRITE-LINE.                                          SM2034.2
029500 END-ROUTINE-13.                                                  SM2034.2
029600     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2034.2
029700         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2034.2
029800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2034.2
029900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2034.2
030000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2034.2
030100      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2034.2
030200          MOVE "NO " TO ERROR-TOTAL                               SM2034.2
030300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2034.2
030400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2034.2
030500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2034.2
030600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2034.2
030700 WRITE-LINE.                                                      SM2034.2
030800     ADD 1 TO RECORD-COUNT.                                       SM2034.2
030900*    IF RECORD-COUNT GREATER 50                                   SM2034.2
031000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2034.2
031100*        MOVE SPACE TO DUMMY-RECORD                               SM2034.2
031200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2034.2
031300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2034.2
031400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2034.2
031500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2034.2
031600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2034.2
031700*        MOVE ZERO TO RECORD-COUNT.                               SM2034.2
031800     PERFORM WRT-LN.                                              SM2034.2
031900 WRT-LN.                                                          SM2034.2
032000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2034.2
032100     MOVE SPACE TO DUMMY-RECORD.                                  SM2034.2
032200 BLANK-LINE-PRINT.                                                SM2034.2
032300     PERFORM WRT-LN.                                              SM2034.2
032400 FAIL-ROUTINE.                                                    SM2034.2
032500     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2034.2
032600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2034.2
032700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2034.2
032800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2034.2
032900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2034.2
033000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2034.2
033100     GO TO  FAIL-ROUTINE-EX.                                      SM2034.2
033200 FAIL-ROUTINE-WRITE.                                              SM2034.2
033300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2034.2
033400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2034.2
033500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2034.2
033600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2034.2
033700 FAIL-ROUTINE-EX. EXIT.                                           SM2034.2
033800 BAIL-OUT.                                                        SM2034.2
033900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2034.2
034000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2034.2
034100 BAIL-OUT-WRITE.                                                  SM2034.2
034200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2034.2
034300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2034.2
034400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2034.2
034500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2034.2
034600 BAIL-OUT-EX. EXIT.                                               SM2034.2
034700 CCVS1-EXIT.                                                      SM2034.2
034800     EXIT.                                                        SM2034.2
034900 INITIALIZATION SECTION.                                          SM2034.2
035000 SM203-INIT.                                                      SM2034.2
035100     OPEN     OUTPUT TEST-FILE.                                   SM2034.2
035200 BUILD SECTION.                                                   SM2034.2
035300 COPY-TEST-1.                                                     SM2034.2
035400     MOVE     RCD-1 TO TF-1.                                      SM2034.2
035500     WRITE    PROOF-REC.                                          SM2034.2
035600     MOVE     RCD-2 TO TF-1.                                      SM2034.2
035700     WRITE    PROOF-REC.                                          SM2034.2
035800     MOVE     RCD-3 TO TF-1.                                      SM2034.2
035900     WRITE    PROOF-REC.                                          SM2034.2
036000     MOVE     RCD-4 TO TF-1.                                      SM2034.2
036100     WRITE    PROOF-REC.                                          SM2034.2
036200     MOVE     RCD-5 TO TF-1.                                      SM2034.2
036300     WRITE    PROOF-REC.                                          SM2034.2
036400     MOVE     RCD-6 TO TF-1.                                      SM2034.2
036500     WRITE    PROOF-REC.                                          SM2034.2
036600     MOVE     RCD-7 TO TF-1.                                      SM2034.2
036700     WRITE    PROOF-REC.                                          SM2034.2
036800     PERFORM  PASS.                                               SM2034.2
036900     GO       TO COPY-WRITE-1.                                    SM2034.2
037000 COPY-DELETE-1.                                                   SM2034.2
037100     PERFORM  DE-LETE.                                            SM2034.2
037200 COPY-WRITE-1.                                                    SM2034.2
037300     MOVE     "COPY ENV DIV REPLAC" TO FEATURE.                   SM2034.2
037400     MOVE     "COPY-TEST-1 " TO PAR-NAME.                         SM2034.2
037500     PERFORM  PRINT-DETAIL.                                       SM2034.2
037600     CLOSE    TEST-FILE.                                          SM2034.2
037700 CCVS-EXIT SECTION.                                               SM2034.2
037800 CCVS-999999.                                                     SM2034.2
037900     GO TO CLOSE-FILES.                                           SM2034.2
      *END-OF,SM203A                                                                  
