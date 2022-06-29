      *HEADER,COBOL,SM205A                                                            
000100 IDENTIFICATION DIVISION.                                         SM2054.2
000200 PROGRAM-ID.                                                      SM2054.2
000300     SM205A.                                                      SM2054.2
000400****************************************************************  SM2054.2
000500*                                                              *  SM2054.2
000600*    VALIDATION FOR:-                                          *  SM2054.2
000700*                                                              *  SM2054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2054.2
000900*                                                              *  SM2054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2054.2
001100*                                                              *  SM2054.2
001200****************************************************************  SM2054.2
001300*                                                              *  SM2054.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2054.2
001500*                                                              *  SM2054.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2054.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2054.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2054.2
001900*                                                              *  SM2054.2
002000****************************************************************  SM2054.2
002100*                                                              *  SM2054.2
002200*    PROGRAM SM205A TESTS THE USE OF THE "COPY" STATEMENT WITH *  SM2054.2
002300*    ITS "REPLACING" PHRASE FOR A SORT DESCRIPTION AND RELATED *  SM2054.2
002400*    ENTRIES.  (THIS PROGRAM ASSUMES THAT PROGRAM ST101        *  SM2054.2
002500*    PERFORMS CORRECTLY).                                      *  SM2054.2
002600*                                                              *  SM2054.2
002700****************************************************************  SM2054.2
002800 ENVIRONMENT DIVISION.                                            SM2054.2
002900 CONFIGURATION SECTION.                                           SM2054.2
003000 SOURCE-COMPUTER.                                                 SM2054.2
003100     XXXXX082.                                                    SM2054.2
003200 OBJECT-COMPUTER.                                                 SM2054.2
003300     XXXXX083.                                                    SM2054.2
003400 INPUT-OUTPUT SECTION.                                            SM2054.2
003500 FILE-CONTROL.                                                    SM2054.2
003600     SELECT PRINT-FILE ASSIGN TO                                  SM2054.2
003700     XXXXX055.                                                    SM2054.2
003800     SELECT SORTFILE-2E ASSIGN TO                                 SM2054.2
003900     XXXXX027.                                                    SM2054.2
004000     SELECT SORTOUT-2E ASSIGN TO                                  SM2054.2
004100     XXXXX001.                                                    SM2054.2
004200 DATA DIVISION.                                                   SM2054.2
004300 FILE SECTION.                                                    SM2054.2
004400 FD  PRINT-FILE.                                                  SM2054.2
004500 01  PRINT-REC PICTURE X(120).                                    SM2054.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 SM2054.2
004700                                                                  SM2054.2
004800                                                                  SM2054.2
004900                                                                  SM2054.2
005000                                                                  SM2054.2
005100                                                                  SM2054.2
005200*                                                                 SM2054.2
005300*********************** COPY STATEMENT USED **********************SM2054.2
005400*                                                                 SM2054.2
005500*SD  SORTFILE-2E                                       COPY K5SDB SM2054.2
005600*                   REPLACING J-RECORD BY S-RECORD.               SM2054.2
005700*                                                                 SM2054.2
005800******************** COPIED TEXT BEGINS BELOW ********************SM2054.2
005900 SD  SORTFILE-2E                                       COPY K5SDB SM2054.2
006000              REPLACING J-RECORD BY S-RECORD.                     SM2054.2
006100*********************** END OF COPIED TEXT ***********************SM2054.2
006200                                                                  SM2054.2
006300                                                                  SM2054.2
006400                                                                  SM2054.2
006500                                                                  SM2054.2
006600                                                                  SM2054.2
006700*                                                                 SM2054.2
006800*********************** COPY STATEMENT USED **********************SM2054.2
006900*                                                                 SM2054.2
007000*01  S-RECORD.                                         COPY K501B SM2054.2
007100*             REPLACING KEY-A BY KEY-1                            SM2054.2
007200*                       XYZ-KEYS BY RDF-KEYS.                     SM2054.2
007300*                                                                 SM2054.2
007400******************** COPIED TEXT BEGINS BELOW ********************SM2054.2
007500 01  S-RECORD.                                         COPY K501B SM2054.2
007600              REPLACING KEY-A BY KEY-1                            SM2054.2
007700                        XYZ-KEYS BY RDF-KEYS.                     SM2054.2
007800*********************** END OF COPIED TEXT ***********************SM2054.2
007900 FD  SORTOUT-2E                                                   SM2054.2
008000     BLOCK CONTAINS 10 RECORDS                                    SM2054.2
008100     LABEL RECORDS ARE STANDARD                                   SM2054.2
008200C    VALUE OF                                                     SM2054.2
008300C    XXXXX074                                                     SM2054.2
008400C    IS                                                           SM2054.2
008500C    XXXXX076                                                     SM2054.2
008600*    XXXXX069                                                     SM2054.2
008700     DATA RECORD SORTED.                                          SM2054.2
008800 01  SORTED PICTURE X(120).                                       SM2054.2
008900 WORKING-STORAGE SECTION.                                         SM2054.2
009000 77  C0 PICTURE 9 VALUE 0.                                        SM2054.2
009100 77  C1 PICTURE 9 VALUE 1.                                        SM2054.2
009200 77  C2 PICTURE 9 VALUE 2.                                        SM2054.2
009300 77  C6 PICTURE 9 VALUE 6.                                        SM2054.2
009400 77  C3 PICTURE 9 VALUE 3.                                        SM2054.2
009500 01  WKEYS-GROUP.                                                 SM2054.2
009600     02  WKEY-1  PICTURE 9.                                       SM2054.2
009700     02  WKEY-2  PICTURE 99.                                      SM2054.2
009800     02  WKEY-3  PICTURE 999.                                     SM2054.2
009900     02  WKEY-4  PICTURE 9999.                                    SM2054.2
010000     02  WKEY-5 PICTURE 9(5).                                     SM2054.2
010100 01  WKEYS-RDF REDEFINES WKEYS-GROUP PICTURE 9(15).               SM2054.2
010200 01  TEST-RESULTS.                                                SM2054.2
010300     02 FILLER                   PIC X      VALUE SPACE.          SM2054.2
010400     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2054.2
010500     02 FILLER                   PIC X      VALUE SPACE.          SM2054.2
010600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2054.2
010700     02 FILLER                   PIC X      VALUE SPACE.          SM2054.2
010800     02  PAR-NAME.                                                SM2054.2
010900       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2054.2
011000       03  PARDOT-X              PIC X      VALUE SPACE.          SM2054.2
011100       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2054.2
011200     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2054.2
011300     02 RE-MARK                  PIC X(61).                       SM2054.2
011400 01  TEST-COMPUTED.                                               SM2054.2
011500     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2054.2
011600     02 FILLER                   PIC X(17)  VALUE                 SM2054.2
011700            "       COMPUTED=".                                   SM2054.2
011800     02 COMPUTED-X.                                               SM2054.2
011900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2054.2
012000     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2054.2
012100                                 PIC -9(9).9(9).                  SM2054.2
012200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2054.2
012300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2054.2
012400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2054.2
012500     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2054.2
012600         04 COMPUTED-18V0                    PIC -9(18).          SM2054.2
012700         04 FILLER                           PIC X.               SM2054.2
012800     03 FILLER PIC X(50) VALUE SPACE.                             SM2054.2
012900 01  TEST-CORRECT.                                                SM2054.2
013000     02 FILLER PIC X(30) VALUE SPACE.                             SM2054.2
013100     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2054.2
013200     02 CORRECT-X.                                                SM2054.2
013300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2054.2
013400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2054.2
013500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2054.2
013600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2054.2
013700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2054.2
013800     03      CR-18V0 REDEFINES CORRECT-A.                         SM2054.2
013900         04 CORRECT-18V0                     PIC -9(18).          SM2054.2
014000         04 FILLER                           PIC X.               SM2054.2
014100     03 FILLER PIC X(2) VALUE SPACE.                              SM2054.2
014200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2054.2
014300 01  CCVS-C-1.                                                    SM2054.2
014400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2054.2
014500-    "SS  PARAGRAPH-NAME                                          SM2054.2
014600-    "       REMARKS".                                            SM2054.2
014700     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2054.2
014800 01  CCVS-C-2.                                                    SM2054.2
014900     02 FILLER                     PIC X        VALUE SPACE.      SM2054.2
015000     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2054.2
015100     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2054.2
015200     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2054.2
015300     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2054.2
015400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2054.2
015500 01  REC-CT                        PIC 99       VALUE ZERO.       SM2054.2
015600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2054.2
015700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2054.2
015800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2054.2
015900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2054.2
016000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2054.2
016100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2054.2
016200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2054.2
016300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2054.2
016400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2054.2
016500 01  CCVS-H-1.                                                    SM2054.2
016600     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2054.2
016700     02  FILLER                    PIC X(42)    VALUE             SM2054.2
016800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2054.2
016900     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2054.2
017000 01  CCVS-H-2A.                                                   SM2054.2
017100   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2054.2
017200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2054.2
017300   02  FILLER                        PIC XXXX   VALUE             SM2054.2
017400     "4.2 ".                                                      SM2054.2
017500   02  FILLER                        PIC X(28)  VALUE             SM2054.2
017600            " COPY - NOT FOR DISTRIBUTION".                       SM2054.2
017700   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2054.2
017800                                                                  SM2054.2
017900 01  CCVS-H-2B.                                                   SM2054.2
018000   02  FILLER                        PIC X(15)  VALUE             SM2054.2
018100            "TEST RESULT OF ".                                    SM2054.2
018200   02  TEST-ID                       PIC X(9).                    SM2054.2
018300   02  FILLER                        PIC X(4)   VALUE             SM2054.2
018400            " IN ".                                               SM2054.2
018500   02  FILLER                        PIC X(12)  VALUE             SM2054.2
018600     " HIGH       ".                                              SM2054.2
018700   02  FILLER                        PIC X(22)  VALUE             SM2054.2
018800            " LEVEL VALIDATION FOR ".                             SM2054.2
018900   02  FILLER                        PIC X(58)  VALUE             SM2054.2
019000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2054.2
019100 01  CCVS-H-3.                                                    SM2054.2
019200     02  FILLER                      PIC X(34)  VALUE             SM2054.2
019300            " FOR OFFICIAL USE ONLY    ".                         SM2054.2
019400     02  FILLER                      PIC X(58)  VALUE             SM2054.2
019500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2054.2
019600     02  FILLER                      PIC X(28)  VALUE             SM2054.2
019700            "  COPYRIGHT   1985 ".                                SM2054.2
019800 01  CCVS-E-1.                                                    SM2054.2
019900     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2054.2
020000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2054.2
020100     02 ID-AGAIN                     PIC X(9).                    SM2054.2
020200     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2054.2
020300 01  CCVS-E-2.                                                    SM2054.2
020400     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2054.2
020500     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2054.2
020600     02 CCVS-E-2-2.                                               SM2054.2
020700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2054.2
020800         03 FILLER                   PIC X      VALUE SPACE.      SM2054.2
020900         03 ENDER-DESC               PIC X(44)  VALUE             SM2054.2
021000            "ERRORS ENCOUNTERED".                                 SM2054.2
021100 01  CCVS-E-3.                                                    SM2054.2
021200     02  FILLER                      PIC X(22)  VALUE             SM2054.2
021300            " FOR OFFICIAL USE ONLY".                             SM2054.2
021400     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2054.2
021500     02  FILLER                      PIC X(58)  VALUE             SM2054.2
021600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2054.2
021700     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2054.2
021800     02 FILLER                       PIC X(15)  VALUE             SM2054.2
021900             " COPYRIGHT 1985".                                   SM2054.2
022000 01  CCVS-E-4.                                                    SM2054.2
022100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2054.2
022200     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2054.2
022300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2054.2
022400     02 FILLER                       PIC X(40)  VALUE             SM2054.2
022500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2054.2
022600 01  XXINFO.                                                      SM2054.2
022700     02 FILLER                       PIC X(19)  VALUE             SM2054.2
022800            "*** INFORMATION ***".                                SM2054.2
022900     02 INFO-TEXT.                                                SM2054.2
023000       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2054.2
023100       04 XXCOMPUTED                 PIC X(20).                   SM2054.2
023200       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2054.2
023300       04 XXCORRECT                  PIC X(20).                   SM2054.2
023400     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2054.2
023500 01  HYPHEN-LINE.                                                 SM2054.2
023600     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2054.2
023700     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2054.2
023800-    "*****************************************".                 SM2054.2
023900     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2054.2
024000-    "******************************".                            SM2054.2
024100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2054.2
024200     "SM205A".                                                    SM2054.2
024300 PROCEDURE DIVISION.                                              SM2054.2
024400 CCVS1 SECTION.                                                   SM2054.2
024500 OPEN-FILES.                                                      SM2054.2
024600     OPEN     OUTPUT PRINT-FILE.                                  SM2054.2
024700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2054.2
024800     MOVE    SPACE TO TEST-RESULTS.                               SM2054.2
024900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2054.2
025000     GO TO CCVS1-EXIT.                                            SM2054.2
025100 CLOSE-FILES.                                                     SM2054.2
025200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2054.2
025300 TERMINATE-CCVS.                                                  SM2054.2
025400*    EXIT PROGRAM.                                                SM2054.2
025500*TERMINATE-CALL.                                                  SM2054.2
025600     STOP     RUN.                                                SM2054.2
025700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2054.2
025800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2054.2
025900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2054.2
026000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2054.2
026100     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2054.2
026200 PRINT-DETAIL.                                                    SM2054.2
026300     IF REC-CT NOT EQUAL TO ZERO                                  SM2054.2
026400             MOVE "." TO PARDOT-X                                 SM2054.2
026500             MOVE REC-CT TO DOTVALUE.                             SM2054.2
026600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2054.2
026700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2054.2
026800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2054.2
026900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2054.2
027000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2054.2
027100     MOVE SPACE TO CORRECT-X.                                     SM2054.2
027200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2054.2
027300     MOVE     SPACE TO RE-MARK.                                   SM2054.2
027400 HEAD-ROUTINE.                                                    SM2054.2
027500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2054.2
027600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2054.2
027700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2054.2
027800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2054.2
027900 COLUMN-NAMES-ROUTINE.                                            SM2054.2
028000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2054.2
028100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2054.2
028200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2054.2
028300 END-ROUTINE.                                                     SM2054.2
028400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2054.2
028500 END-RTN-EXIT.                                                    SM2054.2
028600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2054.2
028700 END-ROUTINE-1.                                                   SM2054.2
028800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2054.2
028900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2054.2
029000      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2054.2
029100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2054.2
029200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2054.2
029300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2054.2
029400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2054.2
029500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2054.2
029600  END-ROUTINE-12.                                                 SM2054.2
029700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2054.2
029800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2054.2
029900         MOVE "NO " TO ERROR-TOTAL                                SM2054.2
030000         ELSE                                                     SM2054.2
030100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2054.2
030200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2054.2
030300     PERFORM WRITE-LINE.                                          SM2054.2
030400 END-ROUTINE-13.                                                  SM2054.2
030500     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2054.2
030600         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2054.2
030700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2054.2
030800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2054.2
030900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2054.2
031000      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2054.2
031100          MOVE "NO " TO ERROR-TOTAL                               SM2054.2
031200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2054.2
031300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2054.2
031400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2054.2
031500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2054.2
031600 WRITE-LINE.                                                      SM2054.2
031700     ADD 1 TO RECORD-COUNT.                                       SM2054.2
031800*    IF RECORD-COUNT GREATER 50                                   SM2054.2
031900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2054.2
032000*        MOVE SPACE TO DUMMY-RECORD                               SM2054.2
032100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2054.2
032200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2054.2
032300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2054.2
032400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2054.2
032500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2054.2
032600*        MOVE ZERO TO RECORD-COUNT.                               SM2054.2
032700     PERFORM WRT-LN.                                              SM2054.2
032800 WRT-LN.                                                          SM2054.2
032900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2054.2
033000     MOVE SPACE TO DUMMY-RECORD.                                  SM2054.2
033100 BLANK-LINE-PRINT.                                                SM2054.2
033200     PERFORM WRT-LN.                                              SM2054.2
033300 FAIL-ROUTINE.                                                    SM2054.2
033400     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2054.2
033500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2054.2
033600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2054.2
033700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2054.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2054.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2054.2
034000     GO TO  FAIL-ROUTINE-EX.                                      SM2054.2
034100 FAIL-ROUTINE-WRITE.                                              SM2054.2
034200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2054.2
034300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2054.2
034400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2054.2
034500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2054.2
034600 FAIL-ROUTINE-EX. EXIT.                                           SM2054.2
034700 BAIL-OUT.                                                        SM2054.2
034800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2054.2
034900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2054.2
035000 BAIL-OUT-WRITE.                                                  SM2054.2
035100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2054.2
035200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2054.2
035300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2054.2
035400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2054.2
035500 BAIL-OUT-EX. EXIT.                                               SM2054.2
035600 CCVS1-EXIT.                                                      SM2054.2
035700     EXIT.                                                        SM2054.2
035800 SORT-INIT SECTION.                                               SM2054.2
035900 I-1.                                                             SM2054.2
036000     SORT SORTFILE-2E                                             SM2054.2
036100     ON ASCENDING KEY KEY-1                                       SM2054.2
036200     ON DESCENDING KEY KEY-2                                      SM2054.2
036300     ON ASCENDING KEY KEY-3                                       SM2054.2
036400     DESCENDING KEY-4 KEY-5                                       SM2054.2
036500     INPUT PROCEDURE IS INSORT                                    SM2054.2
036600     OUTPUT PROCEDURE IS OUTP1 THRU OUTP3.                        SM2054.2
036700 I-2.                                                             SM2054.2
036800     GO TO    CLOSE-FILES.                                        SM2054.2
036900 INSORT SECTION.                                                  SM2054.2
037000 IN-1.                                                            SM2054.2
037100*        NOTE TESTS ORDINARY COPYING OF ENTRIES WHICH ARE ALSO    SM2054.2
037200*             COPIED WITH REPLACEMENT.                            SM2054.2
037300 IN-2.                                                            SM2054.2
037400     MOVE 900009000000000 TO RDF-KEYS.                            SM2054.2
037500     RELEASE S-RECORD.                                            SM2054.2
037600     MOVE 009000000900009 TO RDF-KEYS.                            SM2054.2
037700     RELEASE S-RECORD.                                            SM2054.2
037800     MOVE 900008000000000 TO RDF-KEYS.                            SM2054.2
037900     RELEASE S-RECORD.                                            SM2054.2
038000     MOVE 009000000900008 TO RDF-KEYS.                            SM2054.2
038100     RELEASE S-RECORD.                                            SM2054.2
038200*    NOTE HI-LOW CONTROL RECORDS DONE.                            SM2054.2
038300     MOVE 300003000000000 TO WKEYS-RDF.                           SM2054.2
038400 IN-3.                                                            SM2054.2
038500     PERFORM IN-4 2 TIMES.                                        SM2054.2
038600     GO TO IN-EXIT.                                               SM2054.2
038700 IN-4.                                                            SM2054.2
038800     SUBTRACT C1 FROM WKEY-1.                                     SM2054.2
038900     PERFORM IN-5 6 TIMES.                                        SM2054.2
039000 IN-5.                                                            SM2054.2
039100     IF WKEY-2 IS EQUAL TO C6                                     SM2054.2
039200         MOVE C0 TO WKEY-2.                                       SM2054.2
039300     ADD C1 TO WKEY-2.                                            SM2054.2
039400     PERFORM IN-6 2 TIMES.                                        SM2054.2
039500 IN-6.                                                            SM2054.2
039600     IF WKEY-3 IS EQUAL TO C1                                     SM2054.2
039700         MOVE C3 TO WKEY-3.                                       SM2054.2
039800     SUBTRACT C1 FROM WKEY-3.                                     SM2054.2
039900     PERFORM IN-7 2 TIMES.                                        SM2054.2
040000 IN-7.                                                            SM2054.2
040100     IF       WKEY-4 EQUAL TO C2                                  SM2054.2
040200         MOVE C0 TO WKEY-4.                                       SM2054.2
040300     ADD C1 TO WKEY-4.                                            SM2054.2
040400     PERFORM IN-8 2 TIMES.                                        SM2054.2
040500 IN-8.                                                            SM2054.2
040600     IF WKEY-5 IS EQUAL TO C2                                     SM2054.2
040700         MOVE C0 TO WKEY-5.                                       SM2054.2
040800     ADD C1 TO WKEY-5.                                            SM2054.2
040900     MOVE WKEYS-RDF TO RDF-KEYS.                                  SM2054.2
041000     RELEASE S-RECORD.                                            SM2054.2
041100 IN-EXIT.                                                         SM2054.2
041200     EXIT.                                                        SM2054.2
041300 OUTP1 SECTION.                                                   SM2054.2
041400 WOUTPT1.                                                         SM2054.2
041500     OPEN     OUTPUT SORTOUT-2E.                                  SM2054.2
041600     MOVE     SPACE TO TEST-RESULTS.                              SM2054.2
041700     MOVE     "COPY SD REPLACING" TO FEATURE.                     SM2054.2
041800 COPY-TEST-1.                                                     SM2054.2
041900     PERFORM  RET-1.                                              SM2054.2
042000     IF       RDF-KEYS EQUAL TO 009000000900009                   SM2054.2
042100              PERFORM PASS-1 GO TO COPY-WRITE-1.                  SM2054.2
042200     GO       TO COPY-FAIL-1-1.                                   SM2054.2
042300 COPY-DELETE-1.                                                   SM2054.2
042400     PERFORM  DE-LETE-1.                                          SM2054.2
042500     GO       TO COPY-WRITE-1.                                    SM2054.2
042600 COPY-FAIL-1-1.                                                   SM2054.2
042700     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
042800     MOVE     009000000900009 TO CORRECT-18V0.                    SM2054.2
042900     PERFORM  FAIL-1.                                             SM2054.2
043000 COPY-WRITE-1.                                                    SM2054.2
043100     MOVE     "COPY-TEST-1 " TO PAR-NAME.                         SM2054.2
043200     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
043300 COPY-TEST-2.                                                     SM2054.2
043400     PERFORM  RET-1.                                              SM2054.2
043500     IF       RDF-KEYS EQUAL TO 009000000900008                   SM2054.2
043600              PERFORM PASS-1 GO TO COPY-WRITE-2.                  SM2054.2
043700     GO       TO COPY-FAIL-1-2.                                   SM2054.2
043800 COPY-DELETE-2.                                                   SM2054.2
043900     PERFORM  DE-LETE-1.                                          SM2054.2
044000     GO       TO COPY-WRITE-2.                                    SM2054.2
044100 COPY-FAIL-1-2.                                                   SM2054.2
044200     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
044300     MOVE     009000000900008 TO CORRECT-18V0.                    SM2054.2
044400     PERFORM  FAIL-1.                                             SM2054.2
044500 COPY-WRITE-2.                                                    SM2054.2
044600     MOVE     "COPY-TEST-2 " TO PAR-NAME.                         SM2054.2
044700     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
044800 COPY-TEST-3.                                                     SM2054.2
044900     PERFORM  RET-1.                                              SM2054.2
045000     IF       RDF-KEYS EQUAL TO 106001000200002                   SM2054.2
045100              PERFORM PASS-1 GO TO COPY-WRITE-3.                  SM2054.2
045200     GO       TO COPY-FAIL-1-3.                                   SM2054.2
045300 COPY-DELETE-3.                                                   SM2054.2
045400     PERFORM  DE-LETE-1.                                          SM2054.2
045500     GO       TO COPY-WRITE-3.                                    SM2054.2
045600 COPY-FAIL-1-3.                                                   SM2054.2
045700     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
045800     MOVE     106001000200002 TO CORRECT-18V0.                    SM2054.2
045900     PERFORM  FAIL-1.                                             SM2054.2
046000 COPY-WRITE-3.                                                    SM2054.2
046100     MOVE     "COPY-TEST-3 " TO PAR-NAME.                         SM2054.2
046200     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
046300 COPY-TEST-4.                                                     SM2054.2
046400     PERFORM  RET-2 48 TIMES.                                     SM2054.2
046500     IF       RDF-KEYS EQUAL TO 206001000200002                   SM2054.2
046600              PERFORM PASS-1 GO TO COPY-WRITE-4.                  SM2054.2
046700     GO       TO COPY-FAIL-1-4.                                   SM2054.2
046800 COPY-DELETE-4.                                                   SM2054.2
046900     PERFORM  DE-LETE-1.                                          SM2054.2
047000     GO       TO COPY-WRITE-4.                                    SM2054.2
047100 COPY-FAIL-1-4.                                                   SM2054.2
047200     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
047300     MOVE     206001000200002 TO CORRECT-18V0.                    SM2054.2
047400     PERFORM  FAIL-1.                                             SM2054.2
047500 COPY-WRITE-4.                                                    SM2054.2
047600*        NOTE COPYING OF A PROCEDURE WHICH REFERENCES COPIED DATA.SM2054.2
047700     MOVE "COPY-TEST-4 " TO PAR-NAME.                             SM2054.2
047800     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
047900 COPY-TEST-5.                                                     SM2054.2
048000     PERFORM  RET-2 40 TIMES.                                     SM2054.2
048100     IF       RDF-KEYS EQUAL TO 201001000200002                   SM2054.2
048200              PERFORM PASS-1 GO TO COPY-WRITE-5.                  SM2054.2
048300     GO       TO COPY-FAIL-1-5.                                   SM2054.2
048400 COPY-DELETE-5.                                                   SM2054.2
048500     PERFORM  DE-LETE-1.                                          SM2054.2
048600     GO       TO COPY-WRITE-5.                                    SM2054.2
048700 COPY-FAIL-1-5.                                                   SM2054.2
048800     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
048900     MOVE     201001000200002 TO CORRECT-18V0.                    SM2054.2
049000     PERFORM  FAIL-1.                                             SM2054.2
049100 COPY-WRITE-5.                                                    SM2054.2
049200     MOVE     "COPY-TEST-5 " TO PAR-NAME.                         SM2054.2
049300     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
049400 COPY-TEST-6.                                                     SM2054.2
049500     PERFORM  RET-2  7 TIMES.                                     SM2054.2
049600     IF       RDF-KEYS EQUAL TO 201002000100001                   SM2054.2
049700              PERFORM PASS-1 GO TO COPY-WRITE-6.                  SM2054.2
049800     GO       TO COPY-FAIL-1-6.                                   SM2054.2
049900 COPY-DELETE-6.                                                   SM2054.2
050000     PERFORM  DE-LETE-1.                                          SM2054.2
050100     GO       TO COPY-WRITE-6.                                    SM2054.2
050200 COPY-FAIL-1-6.                                                   SM2054.2
050300     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
050400     MOVE     201002000100001 TO CORRECT-18V0.                    SM2054.2
050500     PERFORM  FAIL-1.                                             SM2054.2
050600 COPY-WRITE-6.                                                    SM2054.2
050700     MOVE     "COPY-TEST-6 " TO PAR-NAME.                         SM2054.2
050800     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
050900 COPY-TEST-7.                                                     SM2054.2
051000     PERFORM  RET-2.                                              SM2054.2
051100     IF       RDF-KEYS EQUAL TO 900008000000000                   SM2054.2
051200              PERFORM PASS-1 GO TO COPY-WRITE-7.                  SM2054.2
051300     GO       TO COPY-FAIL-1-7.                                   SM2054.2
051400 COPY-DELETE-7.                                                   SM2054.2
051500     PERFORM  DE-LETE-1.                                          SM2054.2
051600     GO       TO COPY-WRITE-7.                                    SM2054.2
051700 COPY-FAIL-1-7.                                                   SM2054.2
051800     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
051900     MOVE     900008000000000 TO CORRECT-18V0.                    SM2054.2
052000     PERFORM  FAIL-1.                                             SM2054.2
052100 COPY-WRITE-7.                                                    SM2054.2
052200     MOVE     "COPY-TEST-7 " TO PAR-NAME.                         SM2054.2
052300     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
052400 COPY-TEST-8.                                                     SM2054.2
052500     PERFORM  RET-2.                                              SM2054.2
052600     IF       RDF-KEYS EQUAL TO 900009000000000                   SM2054.2
052700              PERFORM PASS-1 GO TO COPY-WRITE-8.                  SM2054.2
052800     GO       TO COPY-FAIL-1-8.                                   SM2054.2
052900 COPY-DELETE-8.                                                   SM2054.2
053000     PERFORM  DE-LETE-1.                                          SM2054.2
053100     GO       TO COPY-WRITE-8.                                    SM2054.2
053200 COPY-FAIL-1-8.                                                   SM2054.2
053300     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
053400     MOVE     900009000000000 TO CORRECT-18V0.                    SM2054.2
053500     PERFORM  FAIL-1.                                             SM2054.2
053600 COPY-WRITE-8.                                                    SM2054.2
053700     MOVE     "COPY-TEST-8 " TO PAR-NAME.                         SM2054.2
053800     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
053900 OUTP2 SECTION.                                                   SM2054.2
054000 COPY-TEST-9.                                                     SM2054.2
054100     RETURN   SORTFILE-2E END                                     SM2054.2
054200              PERFORM PASS-1 GO TO COPY-WRITE-9.                  SM2054.2
054300*    NOTE     THE FOLLOWING STATEMENTS SHOULD NOT BE EXECUTED.    SM2054.2
054400     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM2054.2
054500     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 SM2054.2
054600 COPY-DELETE-9.                                                   SM2054.2
054700     PERFORM  DE-LETE-1.                                          SM2054.2
054800 COPY-WRITE-9.                                                    SM2054.2
054900     MOVE     "COPY-TEST-9 " TO PAR-NAME.                         SM2054.2
055000     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
055100     CLOSE    SORTOUT-2E.                                         SM2054.2
055200     GO       TO LIB2E-EXIT.                                      SM2054.2
055300 OUTP3 SECTION.                                                   SM2054.2
055400 RET-1.                                                           SM2054.2
055500     RETURN   SORTFILE-2E RECORD AT END GO TO BAD-FILE.           SM2054.2
055600     MOVE     S-RECORD TO SORTED.                                 SM2054.2
055700     WRITE    SORTED.                                             SM2054.2
055800 RET-2.                                                           SM2054.2
055900     RETURN   SORTFILE-2E           END GO TO BAD-FILE.           SM2054.2
056000     MOVE     S-RECORD TO SORTED.                                 SM2054.2
056100     WRITE    SORTED.                                             SM2054.2
056200 BAD-FILE.                                                        SM2054.2
056300     PERFORM  FAIL-1.                                             SM2054.2
056400     MOVE     "BAD-FILE" TO PAR-NAME.                             SM2054.2
056500     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 SM2054.2
056600     PERFORM  PRINT-DETAIL-1.                                     SM2054.2
056700     CLOSE    SORTOUT-2E.                                         SM2054.2
056800     GO TO LIB2E-EXIT.                                            SM2054.2
056900 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       SM2054.2
057000 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         SM2054.2
057100 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        SM2054.2
057200 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    SM2054.2
057300     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2054.2
057400 PRINT-DETAIL-1.                                                  SM2054.2
057500     IF REC-CT NOT EQUAL TO ZERO                                  SM2054.2
057600             MOVE "." TO PARDOT-X                                 SM2054.2
057700             MOVE REC-CT TO DOTVALUE.                             SM2054.2
057800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    SM2054.2
057900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             SM2054.2
058000        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             SM2054.2
058100          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             SM2054.2
058200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2054.2
058300     MOVE SPACE TO CORRECT-X.                                     SM2054.2
058400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2054.2
058500     MOVE     SPACE TO RE-MARK.                                   SM2054.2
058600 WRITE-LINE-1.                                                    SM2054.2
058700     ADD 1 TO RECORD-COUNT.                                       SM2054.2
058800*    IF RECORD-COUNT GREATER 50                                   SM2054.2
058900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2054.2
059000*        MOVE SPACE TO DUMMY-RECORD                               SM2054.2
059100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2054.2
059200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           SM2054.2
059300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   SM2054.2
059400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        SM2054.2
059500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2054.2
059600*        MOVE ZERO TO RECORD-COUNT.                               SM2054.2
059700     PERFORM WRT-LN-1.                                            SM2054.2
059800 WRT-LN-1.                                                        SM2054.2
059900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2054.2
060000     MOVE SPACE TO DUMMY-RECORD.                                  SM2054.2
060100 BLANK-LINE-PRINT-1.                                              SM2054.2
060200     PERFORM WRT-LN-1.                                            SM2054.2
060300 FAIL-ROUTINE-1.                                                  SM2054.2
060400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     SM2054.2
060500     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      SM2054.2
060600     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SM2054.2
060700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   SM2054.2
060800     GO TO FAIL-ROUTINE-EX-1.                                     SM2054.2
060900 FAIL-RTN-WRITE-1.                                                SM2054.2
061000     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         SM2054.2
061100     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. SM2054.2
061200 FAIL-ROUTINE-EX-1. EXIT.                                         SM2054.2
061300 BAIL-OUT-1.                                                      SM2054.2
061400     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     SM2054.2
061500     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             SM2054.2
061600 BAIL-OUT-WRITE-1.                                                SM2054.2
061700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2054.2
061800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   SM2054.2
061900 BAIL-OUT-EX-1. EXIT.                                             SM2054.2
062000 LIB2E-EXIT.                                                      SM2054.2
062100     EXIT.                                                        SM2054.2
      *END-OF,SM205A                                                                  
