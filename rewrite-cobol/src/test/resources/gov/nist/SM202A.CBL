      *HEADER,COBOL,SM201A,SUBPRG,SM202A                                              
000100 IDENTIFICATION DIVISION.                                         SM2024.2
000200 PROGRAM-ID.                                                      SM2024.2
000300     SM202A.                                                      SM2024.2
000400****************************************************************  SM2024.2
000500*                                                              *  SM2024.2
000600*    VALIDATION FOR:-                                          *  SM2024.2
000700*                                                              *  SM2024.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2024.2
000900*                                                              *  SM2024.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2024.2
001100*                                                              *  SM2024.2
001200****************************************************************  SM2024.2
001300*                                                              *  SM2024.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2024.2
001500*                                                              *  SM2024.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2024.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2024.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2024.2
001900*                                                              *  SM2024.2
002000****************************************************************  SM2024.2
002100*                                                              *  SM2024.2
002200*    PROGRAM SM202A READS THE FILE PRODUCED BY SM201A TO       *  SM2024.2
002300*    VERIFY THE PROPER EXECUTION OF THE "COPY REPLACING"       *  SM2024.2
002400*    STATEMENTS IN SM201A.  A NUMBER OF FURTHER TESTS USING    *  SM2024.2
002500*    VARIOUS NUMERIC AMD ALPHANUMERIC LITERALS, QUALIFIED      *  SM2024.2
002600*    DATA NAMES AND MULTIPLE "REPLACING" OPERANDS ARE ALSO     *  SM2024.2
002700*    CARRIED OUT.                                              *  SM2024.2
002800*                                                              *  SM2024.2
002900****************************************************************  SM2024.2
003000 ENVIRONMENT DIVISION.                                            SM2024.2
003100 CONFIGURATION SECTION.                                           SM2024.2
003200 SOURCE-COMPUTER.                                                 SM2024.2
003300     XXXXX082.                                                    SM2024.2
003400 OBJECT-COMPUTER.                                                 SM2024.2
003500     XXXXX083.                                                    SM2024.2
003600 INPUT-OUTPUT SECTION.                                            SM2024.2
003700 FILE-CONTROL.                                                    SM2024.2
003800     SELECT PRINT-FILE ASSIGN TO                                  SM2024.2
003900     XXXXX055.                                                    SM2024.2
004000     SELECT TEST-FILE ASSIGN TO                                   SM2024.2
004100     XXXXD001.                                                    SM2024.2
004200 DATA DIVISION.                                                   SM2024.2
004300 FILE SECTION.                                                    SM2024.2
004400 FD  PRINT-FILE.                                                  SM2024.2
004500 01  PRINT-REC PICTURE X(120).                                    SM2024.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 SM2024.2
004700 FD  TEST-FILE                                                    SM2024.2
004800     LABEL RECORD STANDARD                                        SM2024.2
004900C    VALUE OF                                                     SM2024.2
005000C    XXXXX074                                                     SM2024.2
005100C    IS                                                           SM2024.2
005200C    XXXXX075                                                     SM2024.2
005300*    XXXXX069                                                     SM2024.2
005400     DATA RECORD IS PROOF-REC.                                    SM2024.2
005500 01  PROOF-REC.                                                   SM2024.2
005600     02  TF-1 PICTURE 9(5).                                       SM2024.2
005700     02  FILLER PICTURE X(115).                                   SM2024.2
005800 WORKING-STORAGE SECTION.                                         SM2024.2
005900 01  COUNTER-16 PICTURE 9 VALUE 1.                                SM2024.2
006000 01  TOTAL-AREA.                                                  SM2024.2
006100     02 AREA-1          PICTURE AAAAA.                            SM2024.2
006200     02 AREA-2          PICTURE XXXXB.                            SM2024.2
006300     02 AREA-3          PICTURE XXXXX.                            SM2024.2
006400     02 AREA-4          PICTURE ZZZZZ.                            SM2024.2
006500 01  MISLEADING-DATA.                                             SM2024.2
006600     02 FALSE-DATA-1    PICTURE AAAAA VALUE "FALSE".              SM2024.2
006700     02 FALSE-DATA-2    PICTURE XXXXX VALUE " TENT".              SM2024.2
006800     02 FALSE-DATA-3    PICTURE XXXXX VALUE "- 5 =".              SM2024.2
006900     02 FALSE-DATA-4    PICTURE 99999 VALUE 00012.                SM2024.2
007000 01  QUALIFIED-DATA.                                              SM2024.2
007100     02 TRUE-Q-02.                                                SM2024.2
007200         03 TRUE-Q-03.                                            SM2024.2
007300             04 TRUE-Q-04 PICTURE A(5) VALUE "TRUE ".             SM2024.2
007400         03 FALSE-Q-03.                                           SM2024.2
007500             04 TRUE-Q-04 PICTURE A(5) VALUE "FIGHT".             SM2024.2
007600     02 FALSE-Q-02.                                               SM2024.2
007700         03 TRUE-Q-03.                                            SM2024.2
007800             04 TRUE-Q-04 PICTURE A(5) VALUE "DRIVE".             SM2024.2
007900         03 FALSE-Q-03.                                           SM2024.2
008000             04 TRUE-Q-04 PICTURE A(5) VALUE "THROW".             SM2024.2
008100 01  RE-SUB-DATA        PICTURE X(40) VALUE                       SM2024.2
008200     "ABCDEFGHIJKLMNOPQRST+ 2 =UVWXYZYXWVUTSRQ".                  SM2024.2
008300 01  SUBSCRIPTED-DATA REDEFINES RE-SUB-DATA.                      SM2024.2
008400     02 X OCCURS 2 TIMES.                                         SM2024.2
008500         03 Y OCCURS 2 TIMES.                                     SM2024.2
008600             04 Z OCCURS 2 TIMES PICTURE X(5).                    SM2024.2
008700 01  TEST-RESULTS.                                                SM2024.2
008800     02 FILLER                   PIC X      VALUE SPACE.          SM2024.2
008900     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2024.2
009000     02 FILLER                   PIC X      VALUE SPACE.          SM2024.2
009100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2024.2
009200     02 FILLER                   PIC X      VALUE SPACE.          SM2024.2
009300     02  PAR-NAME.                                                SM2024.2
009400       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2024.2
009500       03  PARDOT-X              PIC X      VALUE SPACE.          SM2024.2
009600       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2024.2
009700     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2024.2
009800     02 RE-MARK                  PIC X(61).                       SM2024.2
009900 01  TEST-COMPUTED.                                               SM2024.2
010000     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2024.2
010100     02 FILLER                   PIC X(17)  VALUE                 SM2024.2
010200            "       COMPUTED=".                                   SM2024.2
010300     02 COMPUTED-X.                                               SM2024.2
010400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2024.2
010500     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2024.2
010600                                 PIC -9(9).9(9).                  SM2024.2
010700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2024.2
010800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2024.2
010900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2024.2
011000     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2024.2
011100         04 COMPUTED-18V0                    PIC -9(18).          SM2024.2
011200         04 FILLER                           PIC X.               SM2024.2
011300     03 FILLER PIC X(50) VALUE SPACE.                             SM2024.2
011400 01  TEST-CORRECT.                                                SM2024.2
011500     02 FILLER PIC X(30) VALUE SPACE.                             SM2024.2
011600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2024.2
011700     02 CORRECT-X.                                                SM2024.2
011800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2024.2
011900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2024.2
012000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2024.2
012100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2024.2
012200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2024.2
012300     03      CR-18V0 REDEFINES CORRECT-A.                         SM2024.2
012400         04 CORRECT-18V0                     PIC -9(18).          SM2024.2
012500         04 FILLER                           PIC X.               SM2024.2
012600     03 FILLER PIC X(2) VALUE SPACE.                              SM2024.2
012700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2024.2
012800 01  CCVS-C-1.                                                    SM2024.2
012900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2024.2
013000-    "SS  PARAGRAPH-NAME                                          SM2024.2
013100-    "       REMARKS".                                            SM2024.2
013200     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2024.2
013300 01  CCVS-C-2.                                                    SM2024.2
013400     02 FILLER                     PIC X        VALUE SPACE.      SM2024.2
013500     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2024.2
013600     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2024.2
013700     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2024.2
013800     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2024.2
013900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2024.2
014000 01  REC-CT                        PIC 99       VALUE ZERO.       SM2024.2
014100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2024.2
014200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2024.2
014300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2024.2
014400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2024.2
014500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2024.2
014600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2024.2
014700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2024.2
014800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2024.2
014900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2024.2
015000 01  CCVS-H-1.                                                    SM2024.2
015100     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2024.2
015200     02  FILLER                    PIC X(42)    VALUE             SM2024.2
015300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2024.2
015400     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2024.2
015500 01  CCVS-H-2A.                                                   SM2024.2
015600   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2024.2
015700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2024.2
015800   02  FILLER                        PIC XXXX   VALUE             SM2024.2
015900     "4.2 ".                                                      SM2024.2
016000   02  FILLER                        PIC X(28)  VALUE             SM2024.2
016100            " COPY - NOT FOR DISTRIBUTION".                       SM2024.2
016200   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2024.2
016300                                                                  SM2024.2
016400 01  CCVS-H-2B.                                                   SM2024.2
016500   02  FILLER                        PIC X(15)  VALUE             SM2024.2
016600            "TEST RESULT OF ".                                    SM2024.2
016700   02  TEST-ID                       PIC X(9).                    SM2024.2
016800   02  FILLER                        PIC X(4)   VALUE             SM2024.2
016900            " IN ".                                               SM2024.2
017000   02  FILLER                        PIC X(12)  VALUE             SM2024.2
017100     " HIGH       ".                                              SM2024.2
017200   02  FILLER                        PIC X(22)  VALUE             SM2024.2
017300            " LEVEL VALIDATION FOR ".                             SM2024.2
017400   02  FILLER                        PIC X(58)  VALUE             SM2024.2
017500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2024.2
017600 01  CCVS-H-3.                                                    SM2024.2
017700     02  FILLER                      PIC X(34)  VALUE             SM2024.2
017800            " FOR OFFICIAL USE ONLY    ".                         SM2024.2
017900     02  FILLER                      PIC X(58)  VALUE             SM2024.2
018000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2024.2
018100     02  FILLER                      PIC X(28)  VALUE             SM2024.2
018200            "  COPYRIGHT   1985 ".                                SM2024.2
018300 01  CCVS-E-1.                                                    SM2024.2
018400     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2024.2
018500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2024.2
018600     02 ID-AGAIN                     PIC X(9).                    SM2024.2
018700     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2024.2
018800 01  CCVS-E-2.                                                    SM2024.2
018900     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2024.2
019000     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2024.2
019100     02 CCVS-E-2-2.                                               SM2024.2
019200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2024.2
019300         03 FILLER                   PIC X      VALUE SPACE.      SM2024.2
019400         03 ENDER-DESC               PIC X(44)  VALUE             SM2024.2
019500            "ERRORS ENCOUNTERED".                                 SM2024.2
019600 01  CCVS-E-3.                                                    SM2024.2
019700     02  FILLER                      PIC X(22)  VALUE             SM2024.2
019800            " FOR OFFICIAL USE ONLY".                             SM2024.2
019900     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2024.2
020000     02  FILLER                      PIC X(58)  VALUE             SM2024.2
020100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2024.2
020200     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2024.2
020300     02 FILLER                       PIC X(15)  VALUE             SM2024.2
020400             " COPYRIGHT 1985".                                   SM2024.2
020500 01  CCVS-E-4.                                                    SM2024.2
020600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2024.2
020700     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2024.2
020800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2024.2
020900     02 FILLER                       PIC X(40)  VALUE             SM2024.2
021000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2024.2
021100 01  XXINFO.                                                      SM2024.2
021200     02 FILLER                       PIC X(19)  VALUE             SM2024.2
021300            "*** INFORMATION ***".                                SM2024.2
021400     02 INFO-TEXT.                                                SM2024.2
021500       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2024.2
021600       04 XXCOMPUTED                 PIC X(20).                   SM2024.2
021700       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2024.2
021800       04 XXCORRECT                  PIC X(20).                   SM2024.2
021900     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2024.2
022000 01  HYPHEN-LINE.                                                 SM2024.2
022100     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2024.2
022200     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2024.2
022300-    "*****************************************".                 SM2024.2
022400     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2024.2
022500-    "******************************".                            SM2024.2
022600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2024.2
022700     "SM202A".                                                    SM2024.2
022800 PROCEDURE DIVISION.                                              SM2024.2
022900 CCVS1 SECTION.                                                   SM2024.2
023000 OPEN-FILES.                                                      SM2024.2
023100     OPEN     OUTPUT PRINT-FILE.                                  SM2024.2
023200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2024.2
023300     MOVE    SPACE TO TEST-RESULTS.                               SM2024.2
023400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2024.2
023500     GO TO CCVS1-EXIT.                                            SM2024.2
023600 CLOSE-FILES.                                                     SM2024.2
023700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2024.2
023800 TERMINATE-CCVS.                                                  SM2024.2
023900*    EXIT PROGRAM.                                                SM2024.2
024000*TERMINATE-CALL.                                                  SM2024.2
024100     STOP     RUN.                                                SM2024.2
024200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2024.2
024300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2024.2
024400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2024.2
024500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2024.2
024600     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2024.2
024700 PRINT-DETAIL.                                                    SM2024.2
024800     IF REC-CT NOT EQUAL TO ZERO                                  SM2024.2
024900             MOVE "." TO PARDOT-X                                 SM2024.2
025000             MOVE REC-CT TO DOTVALUE.                             SM2024.2
025100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2024.2
025200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2024.2
025300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2024.2
025400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2024.2
025500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2024.2
025600     MOVE SPACE TO CORRECT-X.                                     SM2024.2
025700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2024.2
025800     MOVE     SPACE TO RE-MARK.                                   SM2024.2
025900 HEAD-ROUTINE.                                                    SM2024.2
026000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2024.2
026100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2024.2
026200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2024.2
026300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2024.2
026400 COLUMN-NAMES-ROUTINE.                                            SM2024.2
026500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2024.2
026600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2024.2
026700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2024.2
026800 END-ROUTINE.                                                     SM2024.2
026900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2024.2
027000 END-RTN-EXIT.                                                    SM2024.2
027100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2024.2
027200 END-ROUTINE-1.                                                   SM2024.2
027300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2024.2
027400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2024.2
027500      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2024.2
027600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2024.2
027700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2024.2
027800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2024.2
027900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2024.2
028000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2024.2
028100  END-ROUTINE-12.                                                 SM2024.2
028200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2024.2
028300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2024.2
028400         MOVE "NO " TO ERROR-TOTAL                                SM2024.2
028500         ELSE                                                     SM2024.2
028600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2024.2
028700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2024.2
028800     PERFORM WRITE-LINE.                                          SM2024.2
028900 END-ROUTINE-13.                                                  SM2024.2
029000     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2024.2
029100         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2024.2
029200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2024.2
029300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2024.2
029400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2024.2
029500      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2024.2
029600          MOVE "NO " TO ERROR-TOTAL                               SM2024.2
029700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2024.2
029800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2024.2
029900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2024.2
030000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2024.2
030100 WRITE-LINE.                                                      SM2024.2
030200     ADD 1 TO RECORD-COUNT.                                       SM2024.2
030300*    IF RECORD-COUNT GREATER 50                                   SM2024.2
030400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2024.2
030500*        MOVE SPACE TO DUMMY-RECORD                               SM2024.2
030600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2024.2
030700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2024.2
030800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2024.2
030900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2024.2
031000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2024.2
031100*        MOVE ZERO TO RECORD-COUNT.                               SM2024.2
031200     PERFORM WRT-LN.                                              SM2024.2
031300 WRT-LN.                                                          SM2024.2
031400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2024.2
031500     MOVE SPACE TO DUMMY-RECORD.                                  SM2024.2
031600 BLANK-LINE-PRINT.                                                SM2024.2
031700     PERFORM WRT-LN.                                              SM2024.2
031800 FAIL-ROUTINE.                                                    SM2024.2
031900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2024.2
032000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2024.2
032100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2024.2
032200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2024.2
032300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2024.2
032400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2024.2
032500     GO TO  FAIL-ROUTINE-EX.                                      SM2024.2
032600 FAIL-ROUTINE-WRITE.                                              SM2024.2
032700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2024.2
032800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2024.2
032900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2024.2
033000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2024.2
033100 FAIL-ROUTINE-EX. EXIT.                                           SM2024.2
033200 BAIL-OUT.                                                        SM2024.2
033300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2024.2
033400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2024.2
033500 BAIL-OUT-WRITE.                                                  SM2024.2
033600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2024.2
033700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2024.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2024.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2024.2
034000 BAIL-OUT-EX. EXIT.                                               SM2024.2
034100 CCVS1-EXIT.                                                      SM2024.2
034200     EXIT.                                                        SM2024.2
034300 INITIALIZATION SECTION.                                          SM2024.2
034400 SM202A-INIT.                                                     SM2024.2
034500     OPEN     INPUT TEST-FILE.                                    SM2024.2
034600     MOVE     "SM202A CHECKS A FILE WHICH" TO RE-MARK.            SM2024.2
034700     PERFORM  PRINT-DETAIL.                                       SM2024.2
034800     MOVE     "WAS GENERATED IN SM201A."   TO RE-MARK.            SM2024.2
034900     PERFORM  PRINT-DETAIL.                                       SM2024.2
035000     MOVE     "COPY FD REPLACING" TO FEATURE.                     SM2024.2
035100 FD-TEST SECTION.                                                 SM2024.2
035200 COPY-TEST-11.                                                    SM2024.2
035300     PERFORM  READ-TEST-FILE.                                     SM2024.2
035400     IF       TF-1 EQUAL TO 97532                                 SM2024.2
035500              PERFORM PASS GO TO COPY-WRITE-11.                   SM2024.2
035600     GO       TO COPY-FAIL-11.                                    SM2024.2
035700 COPY-DELETE-11.                                                  SM2024.2
035800     PERFORM  DE-LETE.                                            SM2024.2
035900     GO       TO COPY-WRITE-11.                                   SM2024.2
036000 COPY-FAIL-11.                                                    SM2024.2
036100     MOVE     TF-1 TO COMPUTED-N.                                 SM2024.2
036200     MOVE     97532 TO CORRECT-N.                                 SM2024.2
036300     PERFORM  FAIL.                                               SM2024.2
036400 COPY-WRITE-11.                                                   SM2024.2
036500     MOVE     "COPY-TEST-11 " TO PAR-NAME.                        SM2024.2
036600     PERFORM  PRINT-DETAIL.                                       SM2024.2
036700 COPY-TEST-12.                                                    SM2024.2
036800     PERFORM  READ-TEST-FILE.                                     SM2024.2
036900     IF       TF-1 EQUAL TO 23479                                 SM2024.2
037000              PERFORM PASS GO TO COPY-WRITE-12.                   SM2024.2
037100     GO       TO COPY-FAIL-12.                                    SM2024.2
037200 COPY-DELETE-12.                                                  SM2024.2
037300     PERFORM  DE-LETE.                                            SM2024.2
037400     GO       TO COPY-WRITE-12.                                   SM2024.2
037500 COPY-FAIL-12.                                                    SM2024.2
037600     MOVE     TF-1 TO COMPUTED-N.                                 SM2024.2
037700     MOVE     23479 TO CORRECT-N.                                 SM2024.2
037800     PERFORM  FAIL.                                               SM2024.2
037900 COPY-WRITE-12.                                                   SM2024.2
038000     MOVE     "COPY-TEST-12 " TO PAR-NAME.                        SM2024.2
038100     PERFORM  PRINT-DETAIL.                                       SM2024.2
038200 COPY-TEST-13.                                                    SM2024.2
038300     PERFORM  READ-TEST-FILE 3 TIMES.                             SM2024.2
038400     IF       TF-1 EQUAL TO 14003                                 SM2024.2
038500              PERFORM PASS GO TO COPY-WRITE-13.                   SM2024.2
038600     GO       TO COPY-FAIL-13.                                    SM2024.2
038700 COPY-DELETE-13.                                                  SM2024.2
038800     PERFORM  DE-LETE.                                            SM2024.2
038900     GO       TO COPY-WRITE-13.                                   SM2024.2
039000 COPY-FAIL-13.                                                    SM2024.2
039100     MOVE     TF-1 TO COMPUTED-N.                                 SM2024.2
039200     MOVE     14003 TO CORRECT-N.                                 SM2024.2
039300     PERFORM  FAIL.                                               SM2024.2
039400 COPY-WRITE-13.                                                   SM2024.2
039500     MOVE     "COPY-TEST-13 " TO PAR-NAME.                        SM2024.2
039600     PERFORM  PRINT-DETAIL.                                       SM2024.2
039700 COPY-TEST-14.                                                    SM2024.2
039800     PERFORM  READ-TEST-FILE 2 TIMES.                             SM2024.2
039900     IF       TF-1 EQUAL TO 03543                                 SM2024.2
040000              PERFORM PASS GO TO COPY-WRITE-14.                   SM2024.2
040100     GO       TO COPY-FAIL-14.                                    SM2024.2
040200 COPY-DELETE-14.                                                  SM2024.2
040300     PERFORM  DE-LETE.                                            SM2024.2
040400     GO       TO COPY-WRITE-14.                                   SM2024.2
040500 COPY-FAIL-14.                                                    SM2024.2
040600     MOVE     TF-1 TO COMPUTED-N.                                 SM2024.2
040700     MOVE     03543 TO CORRECT-N.                                 SM2024.2
040800     PERFORM  FAIL.                                               SM2024.2
040900 COPY-WRITE-14.                                                   SM2024.2
041000     MOVE     "COPY-TEST-14 " TO PAR-NAME.                        SM2024.2
041100     PERFORM  PRINT-DETAIL.                                       SM2024.2
041200 COPY-INIT-A.                                                     SM2024.2
041300     MOVE     "COPY REPLACING ---  " TO FEATURE.                  SM2024.2
041400     PERFORM  PRINT-DETAIL.                                       SM2024.2
041500     MOVE     "  PARAGRAPH-NAMES   " TO FEATURE.                  SM2024.2
041600 COPY-TEST-15 SECTION.                                            SM2024.2
041700                                                                  SM2024.2
041800                                                                  SM2024.2
041900                                                                  SM2024.2
042000                                                                  SM2024.2
042100                                                                  SM2024.2
042200*                                                                 SM2024.2
042300*********************** COPY STATEMENT USED **********************SM2024.2
042400*                                                                 SM2024.2
042500*                                                      COPY K2SEA SM2024.2
042600*             REPLACING PARA-X BY PARA-2                          SM2024.2
042700*                       12345 BY PARA-3                           SM2024.2
042800*                       DUMMY-PASS BY PASS.                       SM2024.2
042900*                                                                 SM2024.2
043000******************** COPIED TEXT BEGINS BELOW ********************SM2024.2
043100                                                       COPY K2SEA SM2024.2
043200              REPLACING PARA-X BY PARA-2                          SM2024.2
043300                        12345 BY PARA-3                           SM2024.2
043400                        DUMMY-PASS BY PASS.                       SM2024.2
043500*********************** END OF COPIED TEXT ***********************SM2024.2
043600 COPY-A-15 SECTION.                                               SM2024.2
043700 COPY-DELETE-15.                                                  SM2024.2
043800     PERFORM  DE-LETE.                                            SM2024.2
043900 COPY-WRITE-15.                                                   SM2024.2
044000     MOVE     "COPY-TEST-15" TO PAR-NAME.                         SM2024.2
044100     PERFORM  PRINT-DETAIL.                                       SM2024.2
044200 COPY-PARA SECTION.                                               SM2024.2
044300 COPY-INIT-B.                                                     SM2024.2
044400     MOVE     "  BY LITERALS      " TO FEATURE.                   SM2024.2
044500 COPY-TEST-16.                                                    SM2024.2
044600                                                                  SM2024.2
044700                                                                  SM2024.2
044800                                                                  SM2024.2
044900                                                                  SM2024.2
045000                                                                  SM2024.2
045100*                                                                 SM2024.2
045200*********************** COPY STATEMENT USED **********************SM2024.2
045300*                                                                 SM2024.2
045400*                                                      COPY K2PRA SM2024.2
045500*             REPLACING FALSE-DATA-1 BY "TRUE "                   SM2024.2
045600*                       FALSE-DATA-2 BY " TWO$"                   SM2024.2
045700*                       FALSE-DATA-3 BY "+ 2 ="                   SM2024.2
045800*                       FALSE-DATA-4 BY 4.                        SM2024.2
045900*                                                                 SM2024.2
046000******************** COPIED TEXT BEGINS BELOW ********************SM2024.2
046100                                                       COPY K2PRA SM2024.2
046200              REPLACING FALSE-DATA-1 BY "TRUE "                   SM2024.2
046300                        FALSE-DATA-2 BY " TWO$"                   SM2024.2
046400                        FALSE-DATA-3 BY "+ 2 ="                   SM2024.2
046500                        FALSE-DATA-4 BY 4.                        SM2024.2
046600*********************** END OF COPIED TEXT ***********************SM2024.2
046700 COPY-DELETE-16.                                                  SM2024.2
046800     PERFORM  DE-LETE.                                            SM2024.2
046900 COPY-WRITE-16.                                                   SM2024.2
047000     IF COUNTER-16 IS EQUAL TO 0                                  SM2024.2
047100         PERFORM FAIL                                             SM2024.2
047200         GO TO COPY-WRITE-17                                      SM2024.2
047300     ELSE                                                         SM2024.2
047400         SUBTRACT 1 FROM COUNTER-16.                              SM2024.2
047500     IF       P-OR-F EQUAL TO "FAIL*"                             SM2024.2
047600              MOVE TOTAL-AREA TO COMPUTED-A                       SM2024.2
047700              MOVE "TRUE  TWO + 2 =    4" TO CORRECT-A.           SM2024.2
047800     MOVE     "COPY-TEST-16" TO PAR-NAME.                         SM2024.2
047900     PERFORM  PRINT-DETAIL.                                       SM2024.2
048000 COPY-INIT-17.                                                    SM2024.2
048100     MOVE     SPACE TO TOTAL-AREA.                                SM2024.2
048200 COPY-TEST-17.                                                    SM2024.2
048300                                                                  SM2024.2
048400                                                                  SM2024.2
048500                                                                  SM2024.2
048600                                                                  SM2024.2
048700                                                                  SM2024.2
048800*                                                                 SM2024.2
048900*********************** COPY STATEMENT USED **********************SM2024.2
049000*                                                                 SM2024.2
049100*                                                      COPY K2PRA SM2024.2
049200*             REPLACING FALSE-DATA-1 BY TRUE-Q-04 OF TRUE-Q-03    SM2024.2
049300*                                                 IN TRUE-Q-02    SM2024.2
049400*                       COPY-WRITE-16 BY COPY-WRITE-17            SM2024.2
049500*                       FALSE-DATA-2 BY " TWO FIVE "              SM2024.2
049600*                       FALSE-DATA-3 BY Z(2, 1, 1)                SM2024.2
049700*                       FALSE-DATA-4 BY +000004.99.               SM2024.2
049800*                                                                 SM2024.2
049900******************** COPIED TEXT BEGINS BELOW ********************SM2024.2
050000                                                       COPY K2PRA SM2024.2
050100              REPLACING FALSE-DATA-1 BY TRUE-Q-04 OF TRUE-Q-03    SM2024.2
050200                                                  IN TRUE-Q-02    SM2024.2
050300                        COPY-WRITE-16 BY COPY-WRITE-17            SM2024.2
050400                        FALSE-DATA-2 BY " TWO FIVE "              SM2024.2
050500                        FALSE-DATA-3 BY Z (2, 1, 1)               SM2024.2
050600                        FALSE-DATA-4 BY +000004.99.               SM2024.2
050700*********************** END OF COPIED TEXT ***********************SM2024.2
050800 COPY-DELETE-17.                                                  SM2024.2
050900     PERFORM  DE-LETE.                                            SM2024.2
051000 COPY-WRITE-17.                                                   SM2024.2
051100     IF       P-OR-F EQUAL TO "FAIL*"                             SM2024.2
051200              MOVE TOTAL-AREA TO COMPUTED-A                       SM2024.2
051300              MOVE "TRUE  TWO + 2 =    4" TO CORRECT-A.           SM2024.2
051400     MOVE     "COPY-TEST-17" TO PAR-NAME.                         SM2024.2
051500     PERFORM  PRINT-DETAIL.                                       SM2024.2
051600     CLOSE    TEST-FILE.                                          SM2024.2
051700     GO TO CCVS-EXIT.                                             SM2024.2
051800 READ-TEST-FILE.                                                  SM2024.2
051900     READ     TEST-FILE          AT END GO TO BAD-FILE.           SM2024.2
052000 BAD-FILE.                                                        SM2024.2
052100     PERFORM  FAIL.                                               SM2024.2
052200     MOVE     "BAD-FILE" TO PAR-NAME.                             SM2024.2
052300     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 SM2024.2
052400     PERFORM  PRINT-DETAIL.                                       SM2024.2
052500     CLOSE    TEST-FILE.                                          SM2024.2
052600     GO TO CCVS-EXIT.                                             SM2024.2
052700 CCVS-EXIT SECTION.                                               SM2024.2
052800 CCVS-999999.                                                     SM2024.2
052900     GO TO CLOSE-FILES.                                           SM2024.2
      *END-OF,SM202A                                                                  
