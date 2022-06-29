      *HEADER,COBOL,SM101A                                                            
000100 IDENTIFICATION DIVISION.                                         SM1014.2
000200 PROGRAM-ID.                                                      SM1014.2
000300     SM101A.                                                      SM1014.2
000400****************************************************************  SM1014.2
000500*                                                              *  SM1014.2
000600*    VALIDATION FOR:-                                          *  SM1014.2
000700*                                                              *  SM1014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1014.2
000900*                                                              *  SM1014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1014.2
001100*                                                              *  SM1014.2
001200*                                                              *  SM1014.2
001300*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM1014.2
001400*                                                              *  SM1014.2
001500*        X-55  - SYSTEM PRINTER NAME.                          *  SM1014.2
001600*        X-82  - SOURCE COMPUTER NAME.                         *  SM1014.2
001700*        X-83  - OBJECT COMPUTER NAME.                         *  SM1014.2
001800*                                                              *  SM1014.2
001900****************************************************************  SM1014.2
002000*                                                              *  SM1014.2
002100*    PROGRAM SM101A TESTS THE USE OF THE "COPY" STATEMENT      *  SM1014.2
002200*    IN A FILE DESCRIPTION WITH ITS RELATED 01 ENTRIES IN THE  *  SM1014.2
002300*    WORKING-STORAGE SECTION AND IN THE PROCEDURE DIVISION.    *  SM1014.2
002400*    IT CREATES A SEQUENTIAL FILE WHICH IS INPUT TO SM102A TO  *  SM1014.2
002500*    CHECK THE PROPER EXECUTION OF THE "COPY" STATEMENT IN     *  SM1014.2
002600*    SM101A.  IT ALSO TESTS THE EFFECT OF A "COPY" STATEMENT   *  SM1014.2
002700*    APPEARING ON A DEBUGGING LINE.                            *  SM1014.2
002800*                                                              *  SM1014.2
002900****************************************************************  SM1014.2
003000 ENVIRONMENT DIVISION.                                            SM1014.2
003100 CONFIGURATION SECTION.                                           SM1014.2
003200 SOURCE-COMPUTER.                                                 SM1014.2
003300     XXXXX082.                                                    SM1014.2
003400 OBJECT-COMPUTER.                                                 SM1014.2
003500     XXXXX083.                                                    SM1014.2
003600 INPUT-OUTPUT SECTION.                                            SM1014.2
003700 FILE-CONTROL.                                                    SM1014.2
003800     SELECT PRINT-FILE ASSIGN TO                                  SM1014.2
003900     XXXXX055.                                                    SM1014.2
004000     SELECT TEST-FILE ASSIGN TO                                   SM1014.2
004100     XXXXP001.                                                    SM1014.2
004200 DATA DIVISION.                                                   SM1014.2
004300 FILE SECTION.                                                    SM1014.2
004400 FD  PRINT-FILE.                                                  SM1014.2
004500 01  PRINT-REC PICTURE X(120).                                    SM1014.2
004600 01  DUMMY-RECORD PICTURE X(120).                                 SM1014.2
004700                                                                  SM1014.2
004800                                                                  SM1014.2
004900                                                                  SM1014.2
005000                                                                  SM1014.2
005100                                                                  SM1014.2
005200*                                                                 SM1014.2
005300*********************** COPY STATEMENT USED **********************SM1014.2
005400*                                                                 SM1014.2
005500*FD  TEST-FILE                                        COPY K1FDA. SM1014.2
005600*                                                                 SM1014.2
005700******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
005800 FD  TEST-FILE                                         COPY K1FDA.SM1014.2
005900*********************** END OF COPIED TEXT ***********************SM1014.2
006000                                                                  SM1014.2
006100                                                                  SM1014.2
006200                                                                  SM1014.2
006300                                                                  SM1014.2
006400                                                                  SM1014.2
006500*                                                                 SM1014.2
006600*********************** COPY STATEMENT USED **********************SM1014.2
006700*                                                                 SM1014.2
006800*01  TST-TEST                                         COPY K101A. SM1014.2
006900*                                                                 SM1014.2
007000******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
007100 01  TST-TEST                                          COPY K101A.SM1014.2
007200*********************** END OF COPIED TEXT ***********************SM1014.2
007300 WORKING-STORAGE SECTION.                                         SM1014.2
007400*                                                                 SM1014.2
007500*********************** COPY STATEMENT USED **********************SM1014.2
007600*                                                                 SM1014.2
007700*77  RCD-1                                            COPY K1W01. SM1014.2
007800*                                                                 SM1014.2
007900******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
008000 77  RCD-1                                             COPY K1W01.SM1014.2
008100*********************** END OF COPIED TEXT ***********************SM1014.2
008200 77  RCD-3 PICTURE 9(5) VALUE 10901.                              SM1014.2
008300*                                                                 SM1014.2
008400*********************** COPY STATEMENT USED **********************SM1014.2
008500*                                                                 SM1014.2
008600*77  COPY K1W02.                                                  SM1014.2
008700*                                                                 SM1014.2
008800******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
008900 77  COPY K1W02.                                                  SM1014.2
009000*********************** END OF COPIED TEXT ***********************SM1014.2
009100                       14003.                                     SM1014.2
009200 77  RCD-6 PICTURE 9(5) VALUE 19922.                              SM1014.2
009300*                                                                 SM1014.2
009400*********************** COPY STATEMENT USED **********************SM1014.2
009500*                                                                 SM1014.2
009600*77  COPY K1W03.   VALUE 3543.                                    SM1014.2
009700*                                                                 SM1014.2
009800******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
009900 77  COPY K1W03.   VALUE 3543.                                    SM1014.2
010000*********************** END OF COPIED TEXT ***********************SM1014.2
010100 77  COPYSECT-1 PICTURE 9(5) VALUE 72459.                         SM1014.2
010200 77  COPYSECT-2 PICTURE 9(5) VALUE 12132.                         SM1014.2
010300 77  COPYSECT-3 PICTURE X(5) VALUE "TSTLI".                       SM1014.2
010400 77  COPYSECT-4 PICTURE X(5) VALUE "BCOPY".                       SM1014.2
010500*                                                                 SM1014.2
010600*********************** COPY STATEMENT USED **********************SM1014.2
010700*                                                                 SM1014.2
010800*COPY  K1W04.                                                     SM1014.2
010900*                                                                 SM1014.2
011000******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
011100 COPY  K1W04.                                                     SM1014.2
011200*********************** END OF COPIED TEXT ***********************SM1014.2
011300 77  PROC-1 PICTURE 999 VALUE 123.                                SM1014.2
011400 77  PROC-2 PICTURE 999 VALUE 456.                                SM1014.2
011500 77  WSTR-1  PICTURE X(3) VALUE "ABC".                            SM1014.2
011600                                                                  SM1014.2
011700                                                                  SM1014.2
011800                                                                  SM1014.2
011900                                                                  SM1014.2
012000                                                                  SM1014.2
012100 01  WSTR-2.                                                      SM1014.2
012200*                                                                 SM1014.2
012300*********************** COPY STATEMENT USED **********************SM1014.2
012400*                                                                 SM1014.2
012500*                                            COPY K1WKA.          SM1014.2
012600*                                                                 SM1014.2
012700******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
012800                                             COPY K1WKA.          SM1014.2
012900*********************** END OF COPIED TEXT ***********************SM1014.2
013000 01  TEST-RESULTS.                                                SM1014.2
013100     02 FILLER                   PIC X      VALUE SPACE.          SM1014.2
013200     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM1014.2
013300     02 FILLER                   PIC X      VALUE SPACE.          SM1014.2
013400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM1014.2
013500     02 FILLER                   PIC X      VALUE SPACE.          SM1014.2
013600     02  PAR-NAME.                                                SM1014.2
013700       03 FILLER                 PIC X(19)  VALUE SPACE.          SM1014.2
013800       03  PARDOT-X              PIC X      VALUE SPACE.          SM1014.2
013900       03 DOTVALUE               PIC 99     VALUE ZERO.           SM1014.2
014000     02 FILLER                   PIC X(8)   VALUE SPACE.          SM1014.2
014100     02 RE-MARK                  PIC X(61).                       SM1014.2
014200 01  TEST-COMPUTED.                                               SM1014.2
014300     02 FILLER                   PIC X(30)  VALUE SPACE.          SM1014.2
014400     02 FILLER                   PIC X(17)  VALUE                 SM1014.2
014500            "       COMPUTED=".                                   SM1014.2
014600     02 COMPUTED-X.                                               SM1014.2
014700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM1014.2
014800     03 COMPUTED-N               REDEFINES COMPUTED-A             SM1014.2
014900                                 PIC -9(9).9(9).                  SM1014.2
015000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM1014.2
015100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM1014.2
015200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM1014.2
015300     03       CM-18V0 REDEFINES COMPUTED-A.                       SM1014.2
015400         04 COMPUTED-18V0                    PIC -9(18).          SM1014.2
015500         04 FILLER                           PIC X.               SM1014.2
015600     03 FILLER PIC X(50) VALUE SPACE.                             SM1014.2
015700 01  TEST-CORRECT.                                                SM1014.2
015800     02 FILLER PIC X(30) VALUE SPACE.                             SM1014.2
015900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM1014.2
016000     02 CORRECT-X.                                                SM1014.2
016100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM1014.2
016200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM1014.2
016300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM1014.2
016400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM1014.2
016500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM1014.2
016600     03      CR-18V0 REDEFINES CORRECT-A.                         SM1014.2
016700         04 CORRECT-18V0                     PIC -9(18).          SM1014.2
016800         04 FILLER                           PIC X.               SM1014.2
016900     03 FILLER PIC X(2) VALUE SPACE.                              SM1014.2
017000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM1014.2
017100 01  CCVS-C-1.                                                    SM1014.2
017200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM1014.2
017300-    "SS  PARAGRAPH-NAME                                          SM1014.2
017400-    "       REMARKS".                                            SM1014.2
017500     02 FILLER                     PIC X(20)    VALUE SPACE.      SM1014.2
017600 01  CCVS-C-2.                                                    SM1014.2
017700     02 FILLER                     PIC X        VALUE SPACE.      SM1014.2
017800     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM1014.2
017900     02 FILLER                     PIC X(15)    VALUE SPACE.      SM1014.2
018000     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM1014.2
018100     02 FILLER                     PIC X(94)    VALUE SPACE.      SM1014.2
018200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM1014.2
018300 01  REC-CT                        PIC 99       VALUE ZERO.       SM1014.2
018400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM1014.2
018500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM1014.2
018600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM1014.2
018700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM1014.2
018800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM1014.2
018900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM1014.2
019000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM1014.2
019100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM1014.2
019200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM1014.2
019300 01  CCVS-H-1.                                                    SM1014.2
019400     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1014.2
019500     02  FILLER                    PIC X(42)    VALUE             SM1014.2
019600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM1014.2
019700     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1014.2
019800 01  CCVS-H-2A.                                                   SM1014.2
019900   02  FILLER                        PIC X(40)  VALUE SPACE.      SM1014.2
020000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM1014.2
020100   02  FILLER                        PIC XXXX   VALUE             SM1014.2
020200     "4.2 ".                                                      SM1014.2
020300   02  FILLER                        PIC X(28)  VALUE             SM1014.2
020400            " COPY - NOT FOR DISTRIBUTION".                       SM1014.2
020500   02  FILLER                        PIC X(41)  VALUE SPACE.      SM1014.2
020600                                                                  SM1014.2
020700 01  CCVS-H-2B.                                                   SM1014.2
020800   02  FILLER                        PIC X(15)  VALUE             SM1014.2
020900            "TEST RESULT OF ".                                    SM1014.2
021000   02  TEST-ID                       PIC X(9).                    SM1014.2
021100   02  FILLER                        PIC X(4)   VALUE             SM1014.2
021200            " IN ".                                               SM1014.2
021300   02  FILLER                        PIC X(12)  VALUE             SM1014.2
021400     " HIGH       ".                                              SM1014.2
021500   02  FILLER                        PIC X(22)  VALUE             SM1014.2
021600            " LEVEL VALIDATION FOR ".                             SM1014.2
021700   02  FILLER                        PIC X(58)  VALUE             SM1014.2
021800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1014.2
021900 01  CCVS-H-3.                                                    SM1014.2
022000     02  FILLER                      PIC X(34)  VALUE             SM1014.2
022100            " FOR OFFICIAL USE ONLY    ".                         SM1014.2
022200     02  FILLER                      PIC X(58)  VALUE             SM1014.2
022300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1014.2
022400     02  FILLER                      PIC X(28)  VALUE             SM1014.2
022500            "  COPYRIGHT   1985 ".                                SM1014.2
022600 01  CCVS-E-1.                                                    SM1014.2
022700     02 FILLER                       PIC X(52)  VALUE SPACE.      SM1014.2
022800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM1014.2
022900     02 ID-AGAIN                     PIC X(9).                    SM1014.2
023000     02 FILLER                       PIC X(45)  VALUE SPACES.     SM1014.2
023100 01  CCVS-E-2.                                                    SM1014.2
023200     02  FILLER                      PIC X(31)  VALUE SPACE.      SM1014.2
023300     02  FILLER                      PIC X(21)  VALUE SPACE.      SM1014.2
023400     02 CCVS-E-2-2.                                               SM1014.2
023500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM1014.2
023600         03 FILLER                   PIC X      VALUE SPACE.      SM1014.2
023700         03 ENDER-DESC               PIC X(44)  VALUE             SM1014.2
023800            "ERRORS ENCOUNTERED".                                 SM1014.2
023900 01  CCVS-E-3.                                                    SM1014.2
024000     02  FILLER                      PIC X(22)  VALUE             SM1014.2
024100            " FOR OFFICIAL USE ONLY".                             SM1014.2
024200     02  FILLER                      PIC X(12)  VALUE SPACE.      SM1014.2
024300     02  FILLER                      PIC X(58)  VALUE             SM1014.2
024400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1014.2
024500     02  FILLER                      PIC X(13)  VALUE SPACE.      SM1014.2
024600     02 FILLER                       PIC X(15)  VALUE             SM1014.2
024700             " COPYRIGHT 1985".                                   SM1014.2
024800 01  CCVS-E-4.                                                    SM1014.2
024900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM1014.2
025000     02 FILLER                       PIC X(4)   VALUE " OF ".     SM1014.2
025100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM1014.2
025200     02 FILLER                       PIC X(40)  VALUE             SM1014.2
025300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM1014.2
025400 01  XXINFO.                                                      SM1014.2
025500     02 FILLER                       PIC X(19)  VALUE             SM1014.2
025600            "*** INFORMATION ***".                                SM1014.2
025700     02 INFO-TEXT.                                                SM1014.2
025800       04 FILLER                     PIC X(8)   VALUE SPACE.      SM1014.2
025900       04 XXCOMPUTED                 PIC X(20).                   SM1014.2
026000       04 FILLER                     PIC X(5)   VALUE SPACE.      SM1014.2
026100       04 XXCORRECT                  PIC X(20).                   SM1014.2
026200     02 INF-ANSI-REFERENCE           PIC X(48).                   SM1014.2
026300 01  HYPHEN-LINE.                                                 SM1014.2
026400     02 FILLER  PIC IS X VALUE IS SPACE.                          SM1014.2
026500     02 FILLER  PIC IS X(65)    VALUE IS "************************SM1014.2
026600-    "*****************************************".                 SM1014.2
026700     02 FILLER  PIC IS X(54)    VALUE IS "************************SM1014.2
026800-    "******************************".                            SM1014.2
026900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM1014.2
027000     "SM101A".                                                    SM1014.2
027100 PROCEDURE DIVISION.                                              SM1014.2
027200 CCVS1 SECTION.                                                   SM1014.2
027300 OPEN-FILES.                                                      SM1014.2
027400     OPEN     OUTPUT PRINT-FILE.                                  SM1014.2
027500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM1014.2
027600     MOVE    SPACE TO TEST-RESULTS.                               SM1014.2
027700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM1014.2
027800     GO TO CCVS1-EXIT.                                            SM1014.2
027900 CLOSE-FILES.                                                     SM1014.2
028000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM1014.2
028100 TERMINATE-CCVS.                                                  SM1014.2
028200*    EXIT PROGRAM.                                                SM1014.2
028300*TERMINATE-CALL.                                                  SM1014.2
028400     STOP     RUN.                                                SM1014.2
028500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM1014.2
028600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM1014.2
028700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM1014.2
028800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM1014.2
028900     MOVE "****TEST DELETED****" TO RE-MARK.                      SM1014.2
029000 PRINT-DETAIL.                                                    SM1014.2
029100     IF REC-CT NOT EQUAL TO ZERO                                  SM1014.2
029200             MOVE "." TO PARDOT-X                                 SM1014.2
029300             MOVE REC-CT TO DOTVALUE.                             SM1014.2
029400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM1014.2
029500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM1014.2
029600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM1014.2
029700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM1014.2
029800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM1014.2
029900     MOVE SPACE TO CORRECT-X.                                     SM1014.2
030000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM1014.2
030100     MOVE     SPACE TO RE-MARK.                                   SM1014.2
030200 HEAD-ROUTINE.                                                    SM1014.2
030300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1014.2
030400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1014.2
030500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1014.2
030600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1014.2
030700 COLUMN-NAMES-ROUTINE.                                            SM1014.2
030800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1014.2
030900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1014.2
031000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM1014.2
031100 END-ROUTINE.                                                     SM1014.2
031200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM1014.2
031300 END-RTN-EXIT.                                                    SM1014.2
031400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1014.2
031500 END-ROUTINE-1.                                                   SM1014.2
031600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM1014.2
031700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM1014.2
031800      ADD PASS-COUNTER TO ERROR-HOLD.                             SM1014.2
031900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM1014.2
032000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM1014.2
032100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM1014.2
032200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM1014.2
032300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM1014.2
032400  END-ROUTINE-12.                                                 SM1014.2
032500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM1014.2
032600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM1014.2
032700         MOVE "NO " TO ERROR-TOTAL                                SM1014.2
032800         ELSE                                                     SM1014.2
032900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM1014.2
033000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM1014.2
033100     PERFORM WRITE-LINE.                                          SM1014.2
033200 END-ROUTINE-13.                                                  SM1014.2
033300     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM1014.2
033400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM1014.2
033500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM1014.2
033600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM1014.2
033700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1014.2
033800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM1014.2
033900          MOVE "NO " TO ERROR-TOTAL                               SM1014.2
034000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM1014.2
034100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM1014.2
034200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM1014.2
034300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1014.2
034400 WRITE-LINE.                                                      SM1014.2
034500     ADD 1 TO RECORD-COUNT.                                       SM1014.2
034600*    IF RECORD-COUNT GREATER 50                                   SM1014.2
034700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM1014.2
034800*        MOVE SPACE TO DUMMY-RECORD                               SM1014.2
034900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM1014.2
035000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM1014.2
035100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM1014.2
035200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM1014.2
035300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM1014.2
035400*        MOVE ZERO TO RECORD-COUNT.                               SM1014.2
035500     PERFORM WRT-LN.                                              SM1014.2
035600 WRT-LN.                                                          SM1014.2
035700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM1014.2
035800     MOVE SPACE TO DUMMY-RECORD.                                  SM1014.2
035900 BLANK-LINE-PRINT.                                                SM1014.2
036000     PERFORM WRT-LN.                                              SM1014.2
036100 FAIL-ROUTINE.                                                    SM1014.2
036200     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM1014.2
036300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM1014.2
036400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1014.2
036500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM1014.2
036600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1014.2
036700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1014.2
036800     GO TO  FAIL-ROUTINE-EX.                                      SM1014.2
036900 FAIL-ROUTINE-WRITE.                                              SM1014.2
037000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM1014.2
037100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM1014.2
037200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM1014.2
037300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM1014.2
037400 FAIL-ROUTINE-EX. EXIT.                                           SM1014.2
037500 BAIL-OUT.                                                        SM1014.2
037600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM1014.2
037700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM1014.2
037800 BAIL-OUT-WRITE.                                                  SM1014.2
037900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM1014.2
038000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1014.2
038100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1014.2
038200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1014.2
038300 BAIL-OUT-EX. EXIT.                                               SM1014.2
038400 CCVS1-EXIT.                                                      SM1014.2
038500     EXIT.                                                        SM1014.2
038600 INITIALIZATION SECTION.                                          SM1014.2
038700 SM101A-INIT.                                                     SM1014.2
038800     OPEN     OUTPUT TEST-FILE.                                   SM1014.2
038900     MOVE     "OUTPUT OF SM101A IS USED AS" TO RE-MARK.           SM1014.2
039000     PERFORM  PRINT-DETAIL.                                       SM1014.2
039100     MOVE     "INPUT FOR SM102A."           TO RE-MARK.           SM1014.2
039200     PERFORM  PRINT-DETAIL.                                       SM1014.2
039300     MOVE     "COPY ---" TO FEATURE.                              SM1014.2
039400     PERFORM  PRINT-DETAIL.                                       SM1014.2
039500 WORKING-STORAGE-TEST SECTION.                                    SM1014.2
039600 COPY-TEST-1.                                                     SM1014.2
039700     IF       WSTR-1 EQUAL TO WSTR-2                              SM1014.2
039800              PERFORM PASS GO TO COPY-WRITE-1.                    SM1014.2
039900*        NOTE TESTS COPYING OF WORKING-STORAGE ENTRIES.           SM1014.2
040000     GO       TO COPY-FAIL-1.                                     SM1014.2
040100 COPY-DELETE-1.                                                   SM1014.2
040200     PERFORM  DE-LETE.                                            SM1014.2
040300     GO       TO COPY-WRITE-1.                                    SM1014.2
040400 COPY-FAIL-1.                                                     SM1014.2
040500     MOVE     WSTR-2 TO COMPUTED-A.                               SM1014.2
040600     MOVE     "ABC" TO CORRECT-A                                  SM1014.2
040700     PERFORM  FAIL.                                               SM1014.2
040800 COPY-WRITE-1.                                                    SM1014.2
040900     MOVE     "  WKNG-STORAGE ENTRY" TO FEATURE                   SM1014.2
041000     MOVE     "COPY-TEST-1 " TO PAR-NAME.                         SM1014.2
041100     PERFORM  PRINT-DETAIL.                                       SM1014.2
041200 PARAGRAPH-TEST SECTION.                                          SM1014.2
041300 COPY-TEST-2.                                                     SM1014.2
041400                                                                  SM1014.2
041500                                                                  SM1014.2
041600                                                                  SM1014.2
041700                                                                  SM1014.2
041800                                                                  SM1014.2
041900*                                                                 SM1014.2
042000*********************** COPY STATEMENT USED **********************SM1014.2
042100*                                                                 SM1014.2
042200*                                                     COPY K1PRA. SM1014.2
042300*                                                                 SM1014.2
042400******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
042500                                                       COPY K1PRA.SM1014.2
042600*********************** END OF COPIED TEXT ***********************SM1014.2
042700 COPY-TESTT-2.                                                    SM1014.2
042800     IF       PROC-1 EQUAL TO PROC-2                              SM1014.2
042900              PERFORM PASS GO TO COPY-WRITE-2.                    SM1014.2
043000*        NOTE TESTS COPYING OF A PROCEDURE DIVISION STATEMENT.    SM1014.2
043100     GO       TO COPY-FAIL-2.                                     SM1014.2
043200 COPY-DELETE-2.                                                   SM1014.2
043300     PERFORM  DE-LETE.                                            SM1014.2
043400     GO       TO COPY-WRITE-2.                                    SM1014.2
043500 COPY-FAIL-2.                                                     SM1014.2
043600     MOVE     PROC-2 TO COMPUTED-N.                               SM1014.2
043700     MOVE     123 TO CORRECT-N.                                   SM1014.2
043800     PERFORM  FAIL.                                               SM1014.2
043900 COPY-WRITE-2.                                                    SM1014.2
044000     MOVE     "  PROCEDURE" TO FEATURE                            SM1014.2
044100     MOVE     "COPY-TEST-2 " TO PAR-NAME.                         SM1014.2
044200     PERFORM  PRINT-DETAIL.                                       SM1014.2
044300 SECTION-TEST SECTION.                                            SM1014.2
044400                                                                  SM1014.2
044500                                                                  SM1014.2
044600                                                                  SM1014.2
044700                                                                  SM1014.2
044800                                                                  SM1014.2
044900*                                                                 SM1014.2
045000*********************** COPY STATEMENT USED **********************SM1014.2
045100*                                                                 SM1014.2
045200*                                                     COPY K1SEA. SM1014.2
045300*                                                                 SM1014.2
045400******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
045500                                                       COPY K1SEA.SM1014.2
045600D                                                      COPY K1SEA.SM1014.2
045700*********************** END OF COPIED TEXT ***********************SM1014.2
045800 COPY-INIT-A.                                                     SM1014.2
045900     MOVE     "  SECTION" TO FEATURE.                             SM1014.2
046000 COPY-TEST-3.                                                     SM1014.2
046100     IF       COPYSECT-1 EQUAL TO 95427                           SM1014.2
046200              PERFORM PASS GO TO COPY-WRITE-3.                    SM1014.2
046300*        NOTE COPY-TEST-3, 4, 5, 6 TEST THE COPYING OF AN         SM1014.2
046400*             ENTIRE SECTION.                                     SM1014.2
046500     GO       TO COPY-FAIL-3.                                     SM1014.2
046600 COPY-DELETE-3.                                                   SM1014.2
046700     PERFORM  DE-LETE.                                            SM1014.2
046800     GO       TO COPY-WRITE-3.                                    SM1014.2
046900 COPY-FAIL-3.                                                     SM1014.2
047000     MOVE     COPYSECT-1 TO COMPUTED-N.                           SM1014.2
047100     MOVE     95427   TO CORRECT-N.                               SM1014.2
047200     PERFORM  FAIL.                                               SM1014.2
047300 COPY-WRITE-3.                                                    SM1014.2
047400     MOVE     "COPY-TEST-3 " TO PAR-NAME.                         SM1014.2
047500     PERFORM  PRINT-DETAIL.                                       SM1014.2
047600 COPY-TEST-4.                                                     SM1014.2
047700     IF       COPYSECT-2 EQUAL TO 23121                           SM1014.2
047800              PERFORM PASS GO TO COPY-WRITE-4.                    SM1014.2
047900     GO       TO COPY-FAIL-4.                                     SM1014.2
048000 COPY-DELETE-4.                                                   SM1014.2
048100     PERFORM  DE-LETE.                                            SM1014.2
048200     GO       TO COPY-WRITE-4.                                    SM1014.2
048300 COPY-FAIL-4.                                                     SM1014.2
048400     MOVE     COPYSECT-2 TO COMPUTED-N.                           SM1014.2
048500     MOVE     23121   TO CORRECT-N.                               SM1014.2
048600     PERFORM  FAIL.                                               SM1014.2
048700 COPY-WRITE-4.                                                    SM1014.2
048800     MOVE     "COPY-TEST-4 " TO PAR-NAME.                         SM1014.2
048900     PERFORM  PRINT-DETAIL.                                       SM1014.2
049000 COPY-TEST-5.                                                     SM1014.2
049100     IF       COPYSECT-3 EQUAL TO "LIBCO"                         SM1014.2
049200              PERFORM PASS GO TO COPY-WRITE-5.                    SM1014.2
049300     GO       TO COPY-FAIL-5.                                     SM1014.2
049400 COPY-DELETE-5.                                                   SM1014.2
049500     PERFORM  DE-LETE.                                            SM1014.2
049600     GO       TO COPY-WRITE-5.                                    SM1014.2
049700 COPY-FAIL-5.                                                     SM1014.2
049800     MOVE     COPYSECT-3 TO COMPUTED-A.                           SM1014.2
049900     MOVE     "LIBCO" TO CORRECT-A.                               SM1014.2
050000     PERFORM  FAIL.                                               SM1014.2
050100 COPY-WRITE-5.                                                    SM1014.2
050200     MOVE     "COPY-TEST-5 " TO PAR-NAME.                         SM1014.2
050300     PERFORM  PRINT-DETAIL.                                       SM1014.2
050400 COPY-TEST-6.                                                     SM1014.2
050500     IF       COPYSECT-4 EQUAL TO "PYTST"                         SM1014.2
050600              PERFORM PASS GO TO COPY-WRITE-6.                    SM1014.2
050700     GO       TO COPY-FAIL-6.                                     SM1014.2
050800 COPY-DELETE-6.                                                   SM1014.2
050900     PERFORM  DE-LETE.                                            SM1014.2
051000     GO       TO COPY-WRITE-6.                                    SM1014.2
051100 COPY-FAIL-6.                                                     SM1014.2
051200     MOVE     COPYSECT-4 TO COMPUTED-A.                           SM1014.2
051300     MOVE     "PYTST" TO CORRECT-A.                               SM1014.2
051400     PERFORM  FAIL.                                               SM1014.2
051500 COPY-WRITE-6.                                                    SM1014.2
051600     MOVE     "COPY-TEST-6 " TO PAR-NAME.                         SM1014.2
051700     PERFORM  PRINT-DETAIL.                                       SM1014.2
051800 BUILD SECTION.                                                   SM1014.2
051900 COPY-TEST-7.                                                     SM1014.2
052000     MOVE     RCD-1 TO TST-FLD-1.                                 SM1014.2
052100     WRITE    TST-TEST.                                           SM1014.2
052200     MOVE     RCD-2 TO TST-FLD-1.                                 SM1014.2
052300     WRITE    TST-TEST.                                           SM1014.2
052400     MOVE     RCD-3 TO TST-FLD-1.                                 SM1014.2
052500     WRITE    TST-TEST.                                           SM1014.2
052600     MOVE     RCD-4 TO TST-FLD-1.                                 SM1014.2
052700     WRITE    TST-TEST.                                           SM1014.2
052800     MOVE     RCD-5 TO TST-FLD-1.                                 SM1014.2
052900     WRITE    TST-TEST.                                           SM1014.2
053000     MOVE     RCD-6 TO TST-FLD-1.                                 SM1014.2
053100     WRITE    TST-TEST.                                           SM1014.2
053200     MOVE     RCD-7 TO TST-FLD-1.                                 SM1014.2
053300     WRITE    TST-TEST.                                           SM1014.2
053400     PERFORM  PASS.                                               SM1014.2
053500     GO       TO COPY-WRITE-7.                                    SM1014.2
053600 COPY-DELETE-7.                                                   SM1014.2
053700     PERFORM  DE-LETE.                                            SM1014.2
053800 COPY-WRITE-7.                                                    SM1014.2
053900     MOVE     "  FILE DESCRIPTION" TO FEATURE.                    SM1014.2
054000     MOVE     "COPY-TEST-7" TO PAR-NAME.                          SM1014.2
054100     MOVE     "OUTPUT CHECKED IN SM102A" TO RE-MARK.              SM1014.2
054200     PERFORM  PRINT-DETAIL.                                       SM1014.2
054300 COPY-TEST-8.                                                     SM1014.2
054400*                                                                 SM1014.2
054500*********************** COPY STATEMENT USED **********************SM1014.2
054600*                                                                 SM1014.2
054700*    ADD     COPY K1P01. TO WRK-DS-05V00.                         SM1014.2
054800*                                                                 SM1014.2
054900******************** COPIED TEXT BEGINS BELOW ********************SM1014.2
055000     ADD     COPY K1P01. TO WRK-DS-05V00.                         SM1014.2
055100*********************** END OF COPIED TEXT ***********************SM1014.2
055200     IF       WRK-DS-05V00 EQUAL TO 97523                         SM1014.2
055300             PERFORM PASS                                         SM1014.2
055400             GO TO COPY-WRITE-8.                                  SM1014.2
055500     GO TO    COPY-FAIL-8.                                        SM1014.2
055600 COPY-DELETE-8.                                                   SM1014.2
055700     PERFORM DE-LETE.                                             SM1014.2
055800     GO TO    COPY-WRITE-8.                                       SM1014.2
055900 COPY-FAIL-8.                                                     SM1014.2
056000     MOVE    WRK-DS-05V00 TO COMPUTED-N.                          SM1014.2
056100     MOVE    97523        TO CORRECT-N.                           SM1014.2
056200     PERFORM FAIL.                                                SM1014.2
056300 COPY-WRITE-8.                                                    SM1014.2
056400     MOVE     "COPY-TEST-8" TO PAR-NAME.                          SM1014.2
056500     PERFORM PRINT-DETAIL.                                        SM1014.2
056600     CLOSE    TEST-FILE.                                          SM1014.2
056700 CCVS-EXIT SECTION.                                               SM1014.2
056800 CCVS-999999.                                                     SM1014.2
056900     GO TO CLOSE-FILES.                                           SM1014.2
      *END-OF,SM101A                                                                  
