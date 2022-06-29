      *HEADER,COBOL,SM105A                                                            
000100 IDENTIFICATION DIVISION.                                         SM1054.2
000200 PROGRAM-ID.                                                      SM1054.2
000300     SM105A.                                                      SM1054.2
000400****************************************************************  SM1054.2
000500*                                                              *  SM1054.2
000600*    VALIDATION FOR:-                                          *  SM1054.2
000700*                                                              *  SM1054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1054.2
000900*                                                              *  SM1054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1054.2
001100*                                                              *  SM1054.2
001200****************************************************************  SM1054.2
001300*                                                              *  SM1054.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM1054.2
001500*                                                              *  SM1054.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM1054.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM1054.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM1054.2
001900*                                                              *  SM1054.2
002000****************************************************************  SM1054.2
002100*                                                              *  SM1054.2
002200*    PROGRAM NC105A TESTS THE USE OF THE "COPY" STATEMENT IN   *  SM1054.2
002300*    THE DATA DIVISION FOR A SORT DESCRIPTION ENTRY AND THE    *  SM1054.2
002400*    ASSOCIATED RECORD DESCRIPTION ENTRIES.                    *  SM1054.2
002500*                                                              *  SM1054.2
002600****************************************************************  SM1054.2
002700                                                                  SM1054.2
002800 ENVIRONMENT DIVISION.                                            SM1054.2
002900 CONFIGURATION SECTION.                                           SM1054.2
003000 SOURCE-COMPUTER.                                                 SM1054.2
003100     XXXXX082.                                                    SM1054.2
003200 OBJECT-COMPUTER.                                                 SM1054.2
003300     XXXXX083.                                                    SM1054.2
003400 INPUT-OUTPUT SECTION.                                            SM1054.2
003500 FILE-CONTROL.                                                    SM1054.2
003600     SELECT PRINT-FILE ASSIGN TO                                  SM1054.2
003700     XXXXX055.                                                    SM1054.2
003800     SELECT SORTFILE-1E ASSIGN TO                                 SM1054.2
003900     XXXXX027.                                                    SM1054.2
004000     SELECT SORTOUT-1E ASSIGN TO                                  SM1054.2
004100     XXXXX001.                                                    SM1054.2
004200 DATA DIVISION.                                                   SM1054.2
004300 FILE SECTION.                                                    SM1054.2
004400 FD  PRINT-FILE.                                                  SM1054.2
004500 01  PRINT-REC PICTURE X(120).                                    SM1054.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 SM1054.2
004700                                                                  SM1054.2
004800                                                                  SM1054.2
004900                                                                  SM1054.2
005000                                                                  SM1054.2
005100                                                                  SM1054.2
005200*                                                                 SM1054.2
005300*********************** COPY STATEMENT USED **********************SM1054.2
005400*                                                                 SM1054.2
005500*SD  SORTFILE-1E                                      COPY K5SDA. SM1054.2
005600*                                                                 SM1054.2
005700******************** COPIED TEXT BEGINS BELOW ********************SM1054.2
005800 SD  SORTFILE-1E                                       COPY K5SDA.SM1054.2
005900*********************** END OF COPIED TEXT ***********************SM1054.2
006000                                                                  SM1054.2
006100                                                                  SM1054.2
006200                                                                  SM1054.2
006300                                                                  SM1054.2
006400                                                                  SM1054.2
006500 01  S-RECORD.                                                    SM1054.2
006600*                                                                 SM1054.2
006700*********************** COPY STATEMENT USED **********************SM1054.2
006800*                                                                 SM1054.2
006900*                                                     COPY K501A. SM1054.2
007000*                                                                 SM1054.2
007100******************** COPIED TEXT BEGINS BELOW ********************SM1054.2
007200                                                       COPY K501A.SM1054.2
007300*********************** END OF COPIED TEXT ***********************SM1054.2
007400 FD  SORTOUT-1E                                                   SM1054.2
007500     BLOCK CONTAINS 10 RECORDS                                    SM1054.2
007600     LABEL RECORDS ARE STANDARD                                   SM1054.2
007700C    VALUE OF                                                     SM1054.2
007800C    XXXXX074                                                     SM1054.2
007900C    IS                                                           SM1054.2
008000C    XXXXX075                                                     SM1054.2
008100*    XXXXX069                                                     SM1054.2
008200     DATA RECORD SORTED.                                          SM1054.2
008300 01  SORTED PICTURE X(120).                                       SM1054.2
008400 WORKING-STORAGE SECTION.                                         SM1054.2
008500 77  C0 PICTURE 9 VALUE 0.                                        SM1054.2
008600 77  C1 PICTURE 9 VALUE 1.                                        SM1054.2
008700 77  C2 PICTURE 9 VALUE 2.                                        SM1054.2
008800 77  C6 PICTURE 9 VALUE 6.                                        SM1054.2
008900 77  C3 PICTURE 9 VALUE 3.                                        SM1054.2
009000 01  WKEYS-GROUP.                                                 SM1054.2
009100     02  WKEY-1  PICTURE 9.                                       SM1054.2
009200     02  WKEY-2  PICTURE 99.                                      SM1054.2
009300     02  WKEY-3  PICTURE 999.                                     SM1054.2
009400     02  WKEY-4  PICTURE 9999.                                    SM1054.2
009500     02  WKEY-5 PICTURE 9(5).                                     SM1054.2
009600 01  WKEYS-RDF REDEFINES WKEYS-GROUP PICTURE 9(15).               SM1054.2
009700 01  TEST-RESULTS.                                                SM1054.2
009800     02 FILLER                   PIC X      VALUE SPACE.          SM1054.2
009900     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM1054.2
010000     02 FILLER                   PIC X      VALUE SPACE.          SM1054.2
010100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM1054.2
010200     02 FILLER                   PIC X      VALUE SPACE.          SM1054.2
010300     02  PAR-NAME.                                                SM1054.2
010400       03 FILLER                 PIC X(19)  VALUE SPACE.          SM1054.2
010500       03  PARDOT-X              PIC X      VALUE SPACE.          SM1054.2
010600       03 DOTVALUE               PIC 99     VALUE ZERO.           SM1054.2
010700     02 FILLER                   PIC X(8)   VALUE SPACE.          SM1054.2
010800     02 RE-MARK                  PIC X(61).                       SM1054.2
010900 01  TEST-COMPUTED.                                               SM1054.2
011000     02 FILLER                   PIC X(30)  VALUE SPACE.          SM1054.2
011100     02 FILLER                   PIC X(17)  VALUE                 SM1054.2
011200            "       COMPUTED=".                                   SM1054.2
011300     02 COMPUTED-X.                                               SM1054.2
011400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM1054.2
011500     03 COMPUTED-N               REDEFINES COMPUTED-A             SM1054.2
011600                                 PIC -9(9).9(9).                  SM1054.2
011700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM1054.2
011800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM1054.2
011900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM1054.2
012000     03       CM-18V0 REDEFINES COMPUTED-A.                       SM1054.2
012100         04 COMPUTED-18V0                    PIC -9(18).          SM1054.2
012200         04 FILLER                           PIC X.               SM1054.2
012300     03 FILLER PIC X(50) VALUE SPACE.                             SM1054.2
012400 01  TEST-CORRECT.                                                SM1054.2
012500     02 FILLER PIC X(30) VALUE SPACE.                             SM1054.2
012600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM1054.2
012700     02 CORRECT-X.                                                SM1054.2
012800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM1054.2
012900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM1054.2
013000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM1054.2
013100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM1054.2
013200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM1054.2
013300     03      CR-18V0 REDEFINES CORRECT-A.                         SM1054.2
013400         04 CORRECT-18V0                     PIC -9(18).          SM1054.2
013500         04 FILLER                           PIC X.               SM1054.2
013600     03 FILLER PIC X(2) VALUE SPACE.                              SM1054.2
013700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM1054.2
013800 01  CCVS-C-1.                                                    SM1054.2
013900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM1054.2
014000-    "SS  PARAGRAPH-NAME                                          SM1054.2
014100-    "       REMARKS".                                            SM1054.2
014200     02 FILLER                     PIC X(20)    VALUE SPACE.      SM1054.2
014300 01  CCVS-C-2.                                                    SM1054.2
014400     02 FILLER                     PIC X        VALUE SPACE.      SM1054.2
014500     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM1054.2
014600     02 FILLER                     PIC X(15)    VALUE SPACE.      SM1054.2
014700     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM1054.2
014800     02 FILLER                     PIC X(94)    VALUE SPACE.      SM1054.2
014900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM1054.2
015000 01  REC-CT                        PIC 99       VALUE ZERO.       SM1054.2
015100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM1054.2
015200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM1054.2
015300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM1054.2
015400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM1054.2
015500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM1054.2
015600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM1054.2
015700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM1054.2
015800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM1054.2
015900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM1054.2
016000 01  CCVS-H-1.                                                    SM1054.2
016100     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1054.2
016200     02  FILLER                    PIC X(42)    VALUE             SM1054.2
016300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM1054.2
016400     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1054.2
016500 01  CCVS-H-2A.                                                   SM1054.2
016600   02  FILLER                        PIC X(41)  VALUE SPACE.      SM1054.2
016700   02  FILLER                        PIC X(39)  VALUE             SM1054.2
016800            "CCVS85  NCC COPY - NOT FOR DISTRIBUTION".            SM1054.2
016900   02  FILLER                        PIC X(40)  VALUE SPACE.      SM1054.2
017000                                                                  SM1054.2
017100 01  CCVS-H-2B.                                                   SM1054.2
017200   02  FILLER                        PIC X(15)  VALUE             SM1054.2
017300            "TEST RESULT OF ".                                    SM1054.2
017400   02  TEST-ID                       PIC X(9).                    SM1054.2
017500   02  FILLER                        PIC X(4)   VALUE             SM1054.2
017600            " IN ".                                               SM1054.2
017700   02  FILLER                        PIC X(12)  VALUE             SM1054.2
017800     " HIGH       ".                                              SM1054.2
017900   02  FILLER                        PIC X(22)  VALUE             SM1054.2
018000            " LEVEL VALIDATION FOR ".                             SM1054.2
018100   02  FILLER                        PIC X(58)  VALUE             SM1054.2
018200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1054.2
018300 01  CCVS-H-3.                                                    SM1054.2
018400     02  FILLER                      PIC X(34)  VALUE             SM1054.2
018500            " FOR OFFICIAL USE ONLY    ".                         SM1054.2
018600     02  FILLER                      PIC X(58)  VALUE             SM1054.2
018700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1054.2
018800     02  FILLER                      PIC X(28)  VALUE             SM1054.2
018900            "  COPYRIGHT   1985 ".                                SM1054.2
019000 01  CCVS-E-1.                                                    SM1054.2
019100     02 FILLER                       PIC X(52)  VALUE SPACE.      SM1054.2
019200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM1054.2
019300     02 ID-AGAIN                     PIC X(9).                    SM1054.2
019400     02 FILLER                       PIC X(45)  VALUE             SM1054.2
019500            " NTIS DISTRIBUTION COBOL 85".                        SM1054.2
019600 01  CCVS-E-2.                                                    SM1054.2
019700     02  FILLER                      PIC X(31)  VALUE SPACE.      SM1054.2
019800     02  FILLER                      PIC X(21)  VALUE SPACE.      SM1054.2
019900     02 CCVS-E-2-2.                                               SM1054.2
020000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM1054.2
020100         03 FILLER                   PIC X      VALUE SPACE.      SM1054.2
020200         03 ENDER-DESC               PIC X(44)  VALUE             SM1054.2
020300            "ERRORS ENCOUNTERED".                                 SM1054.2
020400 01  CCVS-E-3.                                                    SM1054.2
020500     02  FILLER                      PIC X(22)  VALUE             SM1054.2
020600            " FOR OFFICIAL USE ONLY".                             SM1054.2
020700     02  FILLER                      PIC X(12)  VALUE SPACE.      SM1054.2
020800     02  FILLER                      PIC X(58)  VALUE             SM1054.2
020900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1054.2
021000     02  FILLER                      PIC X(13)  VALUE SPACE.      SM1054.2
021100     02 FILLER                       PIC X(15)  VALUE             SM1054.2
021200             " COPYRIGHT 1985".                                   SM1054.2
021300 01  CCVS-E-4.                                                    SM1054.2
021400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM1054.2
021500     02 FILLER                       PIC X(4)   VALUE " OF ".     SM1054.2
021600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM1054.2
021700     02 FILLER                       PIC X(40)  VALUE             SM1054.2
021800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM1054.2
021900 01  XXINFO.                                                      SM1054.2
022000     02 FILLER                       PIC X(19)  VALUE             SM1054.2
022100            "*** INFORMATION ***".                                SM1054.2
022200     02 INFO-TEXT.                                                SM1054.2
022300       04 FILLER                     PIC X(8)   VALUE SPACE.      SM1054.2
022400       04 XXCOMPUTED                 PIC X(20).                   SM1054.2
022500       04 FILLER                     PIC X(5)   VALUE SPACE.      SM1054.2
022600       04 XXCORRECT                  PIC X(20).                   SM1054.2
022700     02 INF-ANSI-REFERENCE           PIC X(48).                   SM1054.2
022800 01  HYPHEN-LINE.                                                 SM1054.2
022900     02 FILLER  PIC IS X VALUE IS SPACE.                          SM1054.2
023000     02 FILLER  PIC IS X(65)    VALUE IS "************************SM1054.2
023100-    "*****************************************".                 SM1054.2
023200     02 FILLER  PIC IS X(54)    VALUE IS "************************SM1054.2
023300-    "******************************".                            SM1054.2
023400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM1054.2
023500     "SM105A".                                                    SM1054.2
023600 PROCEDURE DIVISION.                                              SM1054.2
023700 CCVS1 SECTION.                                                   SM1054.2
023800 OPEN-FILES.                                                      SM1054.2
023900     OPEN     OUTPUT PRINT-FILE.                                  SM1054.2
024000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM1054.2
024100     MOVE    SPACE TO TEST-RESULTS.                               SM1054.2
024200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM1054.2
024300     GO TO CCVS1-EXIT.                                            SM1054.2
024400 CLOSE-FILES.                                                     SM1054.2
024500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM1054.2
024600 TERMINATE-CCVS.                                                  SM1054.2
024700*    EXIT PROGRAM.                                                SM1054.2
024800*TERMINATE-CALL.                                                  SM1054.2
024900     STOP     RUN.                                                SM1054.2
025000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM1054.2
025100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM1054.2
025200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM1054.2
025300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM1054.2
025400     MOVE "****TEST DELETED****" TO RE-MARK.                      SM1054.2
025500 PRINT-DETAIL.                                                    SM1054.2
025600     IF REC-CT NOT EQUAL TO ZERO                                  SM1054.2
025700             MOVE "." TO PARDOT-X                                 SM1054.2
025800             MOVE REC-CT TO DOTVALUE.                             SM1054.2
025900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM1054.2
026000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM1054.2
026100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM1054.2
026200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM1054.2
026300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM1054.2
026400     MOVE SPACE TO CORRECT-X.                                     SM1054.2
026500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM1054.2
026600     MOVE     SPACE TO RE-MARK.                                   SM1054.2
026700 HEAD-ROUTINE.                                                    SM1054.2
026800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1054.2
026900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1054.2
027000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1054.2
027100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1054.2
027200 COLUMN-NAMES-ROUTINE.                                            SM1054.2
027300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1054.2
027400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1054.2
027500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM1054.2
027600 END-ROUTINE.                                                     SM1054.2
027700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM1054.2
027800 END-RTN-EXIT.                                                    SM1054.2
027900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1054.2
028000 END-ROUTINE-1.                                                   SM1054.2
028100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM1054.2
028200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM1054.2
028300      ADD PASS-COUNTER TO ERROR-HOLD.                             SM1054.2
028400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM1054.2
028500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM1054.2
028600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM1054.2
028700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM1054.2
028800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM1054.2
028900  END-ROUTINE-12.                                                 SM1054.2
029000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM1054.2
029100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM1054.2
029200         MOVE "NO " TO ERROR-TOTAL                                SM1054.2
029300         ELSE                                                     SM1054.2
029400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM1054.2
029500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM1054.2
029600     PERFORM WRITE-LINE.                                          SM1054.2
029700 END-ROUTINE-13.                                                  SM1054.2
029800     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM1054.2
029900         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM1054.2
030000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM1054.2
030100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM1054.2
030200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1054.2
030300      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM1054.2
030400          MOVE "NO " TO ERROR-TOTAL                               SM1054.2
030500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM1054.2
030600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM1054.2
030700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM1054.2
030800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1054.2
030900 WRITE-LINE.                                                      SM1054.2
031000     ADD 1 TO RECORD-COUNT.                                       SM1054.2
031100*    IF RECORD-COUNT GREATER 50                                   SM1054.2
031200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM1054.2
031300*        MOVE SPACE TO DUMMY-RECORD                               SM1054.2
031400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM1054.2
031500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM1054.2
031600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM1054.2
031700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM1054.2
031800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM1054.2
031900*        MOVE ZERO TO RECORD-COUNT.                               SM1054.2
032000     PERFORM WRT-LN.                                              SM1054.2
032100 WRT-LN.                                                          SM1054.2
032200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM1054.2
032300     MOVE SPACE TO DUMMY-RECORD.                                  SM1054.2
032400 BLANK-LINE-PRINT.                                                SM1054.2
032500     PERFORM WRT-LN.                                              SM1054.2
032600 FAIL-ROUTINE.                                                    SM1054.2
032700     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM1054.2
032800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM1054.2
032900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1054.2
033000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM1054.2
033100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1054.2
033200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1054.2
033300     GO TO  FAIL-ROUTINE-EX.                                      SM1054.2
033400 FAIL-ROUTINE-WRITE.                                              SM1054.2
033500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM1054.2
033600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM1054.2
033700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM1054.2
033800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM1054.2
033900 FAIL-ROUTINE-EX. EXIT.                                           SM1054.2
034000 BAIL-OUT.                                                        SM1054.2
034100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM1054.2
034200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM1054.2
034300 BAIL-OUT-WRITE.                                                  SM1054.2
034400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM1054.2
034500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1054.2
034600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1054.2
034700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1054.2
034800 BAIL-OUT-EX. EXIT.                                               SM1054.2
034900 CCVS1-EXIT.                                                      SM1054.2
035000     EXIT.                                                        SM1054.2
035100 SORT-INIT SECTION.                                               SM1054.2
035200 I-1.                                                             SM1054.2
035300     SORT SORTFILE-1E                                             SM1054.2
035400     ON ASCENDING KEY KEY-1                                       SM1054.2
035500     ON DESCENDING KEY KEY-2                                      SM1054.2
035600     ON ASCENDING KEY KEY-3                                       SM1054.2
035700     DESCENDING KEY-4 KEY-5                                       SM1054.2
035800     INPUT PROCEDURE IS INSORT                                    SM1054.2
035900     OUTPUT PROCEDURE IS OUTP1 THRU OUTP3.                        SM1054.2
036000 I-2.                                                             SM1054.2
036100     GO TO    CLOSE-FILES.                                        SM1054.2
036200 INSORT SECTION.                                                  SM1054.2
036300 IN-2.                                                            SM1054.2
036400     MOVE 900009000000000 TO RDF-KEYS.                            SM1054.2
036500     RELEASE S-RECORD.                                            SM1054.2
036600     MOVE 009000000900009 TO RDF-KEYS.                            SM1054.2
036700     RELEASE S-RECORD.                                            SM1054.2
036800     MOVE 900008000000000 TO RDF-KEYS.                            SM1054.2
036900     RELEASE S-RECORD.                                            SM1054.2
037000     MOVE 009000000900008 TO RDF-KEYS.                            SM1054.2
037100     RELEASE S-RECORD.                                            SM1054.2
037200*    NOTE HI-LOW CONTROL RECORDS DONE.                            SM1054.2
037300     MOVE 300003000000000 TO WKEYS-RDF.                           SM1054.2
037400 IN-3.                                                            SM1054.2
037500     PERFORM IN-4 2 TIMES.                                        SM1054.2
037600     GO TO IN-EXIT.                                               SM1054.2
037700 IN-4.                                                            SM1054.2
037800     SUBTRACT C1 FROM WKEY-1.                                     SM1054.2
037900     PERFORM IN-5 6 TIMES.                                        SM1054.2
038000 IN-5.                                                            SM1054.2
038100     IF WKEY-2 IS EQUAL TO C6                                     SM1054.2
038200         MOVE C0 TO WKEY-2.                                       SM1054.2
038300     ADD C1 TO WKEY-2.                                            SM1054.2
038400     PERFORM IN-6 2 TIMES.                                        SM1054.2
038500 IN-6.                                                            SM1054.2
038600     IF WKEY-3 IS EQUAL TO C1                                     SM1054.2
038700         MOVE C3 TO WKEY-3.                                       SM1054.2
038800     SUBTRACT C1 FROM WKEY-3.                                     SM1054.2
038900     PERFORM IN-7 2 TIMES.                                        SM1054.2
039000 IN-7.                                                            SM1054.2
039100     IF WKEY-4 EQUAL TO C2                                        SM1054.2
039200         MOVE C0 TO WKEY-4.                                       SM1054.2
039300     ADD C1 TO WKEY-4.                                            SM1054.2
039400     PERFORM IN-8 2 TIMES.                                        SM1054.2
039500 IN-8.                                                            SM1054.2
039600     IF WKEY-5 IS EQUAL TO C2                                     SM1054.2
039700         MOVE C0 TO WKEY-5.                                       SM1054.2
039800     ADD C1 TO WKEY-5.                                            SM1054.2
039900     MOVE WKEYS-RDF TO RDF-KEYS.                                  SM1054.2
040000     RELEASE S-RECORD.                                            SM1054.2
040100 IN-EXIT.                                                         SM1054.2
040200     EXIT.                                                        SM1054.2
040300 OUTP1 SECTION.                                                   SM1054.2
040400 SM105-INIT.                                                      SM1054.2
040500     OPEN     OUTPUT SORTOUT-1E.                                  SM1054.2
040600     MOVE     "COPY SORT DESCR" TO FEATURE.                       SM1054.2
040700 COPY-TEST-1.                                                     SM1054.2
040800     PERFORM  RET-1.                                              SM1054.2
040900     IF       RDF-KEYS EQUAL TO 009000000900009                   SM1054.2
041000              PERFORM PASS-1 GO TO COPY-WRITE-1.                  SM1054.2
041100     GO       TO COPY-FAIL-1-1.                                   SM1054.2
041200 COPY-DELETE-1.                                                   SM1054.2
041300     PERFORM  DE-LETE-1.                                          SM1054.2
041400     GO       TO COPY-WRITE-1.                                    SM1054.2
041500 COPY-FAIL-1-1.                                                   SM1054.2
041600     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
041700     MOVE     009000000900009 TO CORRECT-18V0.                    SM1054.2
041800     PERFORM  FAIL-1.                                             SM1054.2
041900 COPY-WRITE-1.                                                    SM1054.2
042000     MOVE     "COPY-TEST-1 " TO PAR-NAME.                         SM1054.2
042100     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
042200 COPY-TEST-2.                                                     SM1054.2
042300     PERFORM  RET-1.                                              SM1054.2
042400     IF       RDF-KEYS EQUAL TO 009000000900008                   SM1054.2
042500              PERFORM PASS-1 GO TO COPY-WRITE-2.                  SM1054.2
042600     GO       TO COPY-FAIL-1-2.                                   SM1054.2
042700 COPY-DELETE-2.                                                   SM1054.2
042800     PERFORM  DE-LETE-1.                                          SM1054.2
042900     GO       TO COPY-WRITE-2.                                    SM1054.2
043000 COPY-FAIL-1-2.                                                   SM1054.2
043100     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
043200     MOVE     009000000900008 TO CORRECT-18V0.                    SM1054.2
043300     PERFORM  FAIL-1.                                             SM1054.2
043400 COPY-WRITE-2.                                                    SM1054.2
043500     MOVE     "COPY-TEST-2 " TO PAR-NAME.                         SM1054.2
043600     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
043700 COPY-TEST-3.                                                     SM1054.2
043800     PERFORM  RET-1.                                              SM1054.2
043900     IF       RDF-KEYS EQUAL TO 106001000200002                   SM1054.2
044000              PERFORM PASS-1 GO TO COPY-WRITE-3.                  SM1054.2
044100     GO       TO COPY-FAIL-1-3.                                   SM1054.2
044200 COPY-DELETE-3.                                                   SM1054.2
044300     PERFORM  DE-LETE-1.                                          SM1054.2
044400     GO       TO COPY-WRITE-3.                                    SM1054.2
044500 COPY-FAIL-1-3.                                                   SM1054.2
044600     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
044700     MOVE     106001000200002 TO CORRECT-18V0.                    SM1054.2
044800     PERFORM  FAIL-1.                                             SM1054.2
044900 COPY-WRITE-3.                                                    SM1054.2
045000     MOVE     "COPY-TEST-3 " TO PAR-NAME.                         SM1054.2
045100     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
045200 OUTP2 SECTION.                                                   SM1054.2
045300 COPY-TEST-4.                                                     SM1054.2
045400     PERFORM  RET-2 48 TIMES.                                     SM1054.2
045500     IF       RDF-KEYS EQUAL TO 206001000200002                   SM1054.2
045600              PERFORM PASS-1 GO TO COPY-WRITE-4.                  SM1054.2
045700     GO       TO COPY-FAIL-1-4.                                   SM1054.2
045800 COPY-DELETE-4.                                                   SM1054.2
045900     PERFORM  DE-LETE-1.                                          SM1054.2
046000     GO       TO COPY-WRITE-4.                                    SM1054.2
046100 COPY-FAIL-1-4.                                                   SM1054.2
046200     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
046300     MOVE     206001000200002 TO CORRECT-18V0.                    SM1054.2
046400     PERFORM  FAIL-1.                                             SM1054.2
046500 COPY-WRITE-4.                                                    SM1054.2
046600     MOVE     "COPY-TEST-4 " TO PAR-NAME.                         SM1054.2
046700     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
046800 COPY-TEST-5.                                                     SM1054.2
046900     PERFORM  RET-2 40 TIMES.                                     SM1054.2
047000     IF       RDF-KEYS EQUAL TO 201001000200002                   SM1054.2
047100              PERFORM PASS-1 GO TO COPY-WRITE-5.                  SM1054.2
047200     GO       TO COPY-FAIL-1-5.                                   SM1054.2
047300 COPY-DELETE-5.                                                   SM1054.2
047400     PERFORM  DE-LETE-1.                                          SM1054.2
047500     GO       TO COPY-WRITE-5.                                    SM1054.2
047600 COPY-FAIL-1-5.                                                   SM1054.2
047700     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
047800     MOVE     201001000200002 TO CORRECT-18V0.                    SM1054.2
047900     PERFORM  FAIL-1.                                             SM1054.2
048000 COPY-WRITE-5.                                                    SM1054.2
048100     MOVE     "COPY-TEST-5 " TO PAR-NAME.                         SM1054.2
048200     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
048300 COPY-TEST-6.                                                     SM1054.2
048400     PERFORM  RET-2  7 TIMES.                                     SM1054.2
048500     IF       RDF-KEYS EQUAL TO 201002000100001                   SM1054.2
048600              PERFORM PASS-1 GO TO COPY-WRITE-6.                  SM1054.2
048700     GO       TO COPY-FAIL-1-6.                                   SM1054.2
048800 COPY-DELETE-6.                                                   SM1054.2
048900     PERFORM  DE-LETE-1.                                          SM1054.2
049000     GO       TO COPY-WRITE-6.                                    SM1054.2
049100 COPY-FAIL-1-6.                                                   SM1054.2
049200     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
049300     MOVE     201002000100001 TO CORRECT-18V0.                    SM1054.2
049400     PERFORM  FAIL-1.                                             SM1054.2
049500 COPY-WRITE-6.                                                    SM1054.2
049600     MOVE     "COPY-TEST-6 " TO PAR-NAME.                         SM1054.2
049700     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
049800 COPY-TEST-7.                                                     SM1054.2
049900     PERFORM  RET-2.                                              SM1054.2
050000     IF       RDF-KEYS EQUAL TO 900008000000000                   SM1054.2
050100              PERFORM PASS-1 GO TO COPY-WRITE-7.                  SM1054.2
050200     GO       TO COPY-FAIL-1-7.                                   SM1054.2
050300 COPY-DELETE-7.                                                   SM1054.2
050400     PERFORM  DE-LETE-1.                                          SM1054.2
050500     GO       TO COPY-WRITE-7.                                    SM1054.2
050600 COPY-FAIL-1-7.                                                   SM1054.2
050700     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
050800     MOVE     900008000000000 TO CORRECT-18V0.                    SM1054.2
050900     PERFORM  FAIL-1.                                             SM1054.2
051000 COPY-WRITE-7.                                                    SM1054.2
051100     MOVE     "COPY-TEST-7 " TO PAR-NAME.                         SM1054.2
051200     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
051300 COPY-TEST-8.                                                     SM1054.2
051400     PERFORM  RET-2.                                              SM1054.2
051500     IF       RDF-KEYS EQUAL TO 900009000000000                   SM1054.2
051600              PERFORM PASS-1 GO TO COPY-WRITE-8.                  SM1054.2
051700     GO       TO COPY-FAIL-1-8.                                   SM1054.2
051800 COPY-DELETE-8.                                                   SM1054.2
051900     PERFORM  DE-LETE-1.                                          SM1054.2
052000     GO       TO COPY-WRITE-8.                                    SM1054.2
052100 COPY-FAIL-1-8.                                                   SM1054.2
052200     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
052300     MOVE     900009000000000 TO CORRECT-18V0.                    SM1054.2
052400     PERFORM  FAIL-1.                                             SM1054.2
052500 COPY-WRITE-8.                                                    SM1054.2
052600     MOVE     "COPY-TEST-8 " TO PAR-NAME.                         SM1054.2
052700     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
052800 COPY-TEST-9.                                                     SM1054.2
052900     RETURN   SORTFILE-1E END                                     SM1054.2
053000              PERFORM PASS-1 GO TO COPY-WRITE-9.                  SM1054.2
053100*    NOTE     THE FOLLOWING STATEMENTS SHOULD NOT BE EXECUTED.    SM1054.2
053200     PERFORM  FAIL-1.                                             SM1054.2
053300     MOVE     RDF-KEYS TO COMPUTED-18V0.                          SM1054.2
053400     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 SM1054.2
053500     GO       TO COPY-WRITE-9.                                    SM1054.2
053600 COPY-DELETE-9.                                                   SM1054.2
053700     PERFORM  DE-LETE-1.                                          SM1054.2
053800 COPY-WRITE-9.                                                    SM1054.2
053900     MOVE     "COPY-TEST-9 " TO PAR-NAME.                         SM1054.2
054000     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
054100 OUTP3 SECTION.                                                   SM1054.2
054200 RET-0.                                                           SM1054.2
054300     CLOSE    SORTOUT-1E.                                         SM1054.2
054400     GO       TO LIB1E-EXIT.                                      SM1054.2
054500 RET-1.                                                           SM1054.2
054600     RETURN   SORTFILE-1E RECORD AT END GO TO BAD-FILE.           SM1054.2
054700     MOVE     S-RECORD TO SORTED.                                 SM1054.2
054800     WRITE    SORTED.                                             SM1054.2
054900 RET-2.                                                           SM1054.2
055000     RETURN   SORTFILE-1E           END GO TO BAD-FILE.           SM1054.2
055100     MOVE     S-RECORD TO SORTED.                                 SM1054.2
055200     WRITE    SORTED.                                             SM1054.2
055300 BAD-FILE.                                                        SM1054.2
055400     PERFORM  FAIL-1.                                             SM1054.2
055500     MOVE     "BAD-FILE" TO PAR-NAME.                             SM1054.2
055600     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 SM1054.2
055700     PERFORM  PRINT-DETAIL-1.                                     SM1054.2
055800     CLOSE    SORTOUT-1E.                                         SM1054.2
055900     GO TO    LIB1E-EXIT.                                         SM1054.2
056000 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       SM1054.2
056100 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         SM1054.2
056200 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        SM1054.2
056300 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    SM1054.2
056400     MOVE "****TEST DELETED****" TO RE-MARK.                      SM1054.2
056500 PRINT-DETAIL-1.                                                  SM1054.2
056600     IF REC-CT NOT EQUAL TO ZERO                                  SM1054.2
056700             MOVE "." TO PARDOT-X                                 SM1054.2
056800             MOVE REC-CT TO DOTVALUE.                             SM1054.2
056900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    SM1054.2
057000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             SM1054.2
057100        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             SM1054.2
057200          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             SM1054.2
057300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM1054.2
057400     MOVE SPACE TO CORRECT-X.                                     SM1054.2
057500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM1054.2
057600     MOVE     SPACE TO RE-MARK.                                   SM1054.2
057700 WRITE-LINE-1.                                                    SM1054.2
057800     ADD 1 TO RECORD-COUNT.                                       SM1054.2
057900*    IF RECORD-COUNT GREATER 50                                   SM1054.2
058000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM1054.2
058100*        MOVE SPACE TO DUMMY-RECORD                               SM1054.2
058200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM1054.2
058300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           SM1054.2
058400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   SM1054.2
058500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        SM1054.2
058600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM1054.2
058700*        MOVE ZERO TO RECORD-COUNT.                               SM1054.2
058800     PERFORM WRT-LN-1.                                            SM1054.2
058900 WRT-LN-1.                                                        SM1054.2
059000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM1054.2
059100     MOVE SPACE TO DUMMY-RECORD.                                  SM1054.2
059200 BLANK-LINE-PRINT-1.                                              SM1054.2
059300     PERFORM WRT-LN-1.                                            SM1054.2
059400 FAIL-ROUTINE-1.                                                  SM1054.2
059500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     SM1054.2
059600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      SM1054.2
059700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SM1054.2
059800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   SM1054.2
059900     GO TO FAIL-ROUTINE-EX-1.                                     SM1054.2
060000 FAIL-RTN-WRITE-1.                                                SM1054.2
060100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         SM1054.2
060200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. SM1054.2
060300 FAIL-ROUTINE-EX-1. EXIT.                                         SM1054.2
060400 BAIL-OUT-1.                                                      SM1054.2
060500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     SM1054.2
060600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             SM1054.2
060700 BAIL-OUT-WRITE-1.                                                SM1054.2
060800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM1054.2
060900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   SM1054.2
061000 BAIL-OUT-EX-1. EXIT.                                             SM1054.2
061100 LIB1E-EXIT.                                                      SM1054.2
061200     EXIT.                                                        SM1054.2
      *END-OF,SM105A                                                                  
