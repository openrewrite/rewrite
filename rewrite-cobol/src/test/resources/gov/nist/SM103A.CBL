      *HEADER,COBOL,SM103A                                                            
000100 IDENTIFICATION DIVISION.                                         SM1034.2
000200 PROGRAM-ID.                                                      SM1034.2
000300     SM103A.                                                      SM1034.2
000400****************************************************************  SM1034.2
000500*                                                              *  SM1034.2
000600*    VALIDATION FOR:-                                          *  SM1034.2
000700*                                                              *  SM1034.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1034.2
000900*                                                              *  SM1034.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1034.2
001100*                                                              *  SM1034.2
001200****************************************************************  SM1034.2
001300*                                                              *  SM1034.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM1034.2
001500*                                                              *  SM1034.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM1034.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM1034.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM1034.2
001900*                                                              *  SM1034.2
002000****************************************************************  SM1034.2
002100*                                                              *  SM1034.2
002200*    PROGRAM SM103A TESTS THE USE OF THE "COPY" STATEMENT IN   *  SM1034.2
002300*    THE IDENTIFICATION DIVISION AND ENVIRONMENT DIVISION      *  SM1034.2
002400*    (SOURCE-COMPUTER, OBJECT-COMPUTER, SPECIAL-NAMES,         *  SM1034.2
002500*    FILE-CONTROL AND I-O-CONTROL ENTRIES).                    *  SM1034.2
002600*    A SEQUENTIAL FILE IS PRODUCED WHICH IS READ AND CHECKED   *  SM1034.2
002700*    BY SM104A.                                                *  SM1034.2
002800*    THE MAXIMUM AND MINIMUM LENGTHS OF A LIBRARY TEXT WORD    *  SM1034.2
002900*    ARE ALSO TESTED.                                          *  SM1034.2
003000*                                                              *  SM1034.2
003100****************************************************************  SM1034.2
003200 SECURITY.                                                        SM1034.2
003300     COPY K3SNA.                                                  SM1034.2
003400 ENVIRONMENT DIVISION.                                            SM1034.2
003500 CONFIGURATION SECTION.                                           SM1034.2
003600                                                                  SM1034.2
003700                                                                  SM1034.2
003800                                                                  SM1034.2
003900                                                                  SM1034.2
004000                                                                  SM1034.2
004100*                                                                 SM1034.2
004200*********************** COPY STATEMENT USED **********************SM1034.2
004300*                                                                 SM1034.2
004400*SOURCE-COMPUTER.                                     COPY K3SCA  SM1034.2
004500*                                                                 SM1034.2
004600******************** COPIED TEXT BEGINS BELOW ********************SM1034.2
004700 SOURCE-COMPUTER.                                      COPY K3SCA.SM1034.2
004800*********************** END OF COPIED TEXT ***********************SM1034.2
004900                                                                  SM1034.2
005000                                                                  SM1034.2
005100                                                                  SM1034.2
005200                                                                  SM1034.2
005300                                                                  SM1034.2
005400*                                                                 SM1034.2
005500*********************** COPY STATEMENT USED **********************SM1034.2
005600*                                                                 SM1034.2
005700*OBJECT-COMPUTER.                                     COPY K3OCA  SM1034.2
005800*                                                                 SM1034.2
005900******************** COPIED TEXT BEGINS BELOW ********************SM1034.2
006000 OBJECT-COMPUTER.                                      COPY K3OCA.SM1034.2
006100*********************** END OF COPIED TEXT ***********************SM1034.2
006200                                                                  SM1034.2
006300                                                                  SM1034.2
006400                                                                  SM1034.2
006500                                                                  SM1034.2
006600                                                                  SM1034.2
006700*                                                                 SM1034.2
006800*********************** COPY STATEMENT USED **********************SM1034.2
006900*                                                                 SM1034.2
007000*SPECIAL-NAMES.                                       COPY K3SNA. SM1034.2
007100*                                                                 SM1034.2
007200******************** COPIED TEXT BEGINS BELOW ********************SM1034.2
007300 SPECIAL-NAMES.                                        COPY K3SNA.SM1034.2
007400*********************** END OF COPIED TEXT ***********************SM1034.2
007500 INPUT-OUTPUT SECTION.                                            SM1034.2
007600                                                                  SM1034.2
007700                                                                  SM1034.2
007800                                                                  SM1034.2
007900                                                                  SM1034.2
008000                                                                  SM1034.2
008100*                                                                 SM1034.2
008200*********************** COPY STATEMENT USED **********************SM1034.2
008300*                                                                 SM1034.2
008400*FILE-CONTROL.                                        COPY K3FCA. SM1034.2
008500*                                                                 SM1034.2
008600******************** COPIED TEXT BEGINS BELOW ********************SM1034.2
008700 FILE-CONTROL.                                         COPY K3FCA.SM1034.2
008800*********************** END OF COPIED TEXT ***********************SM1034.2
008900                                                                  SM1034.2
009000                                                                  SM1034.2
009100                                                                  SM1034.2
009200                                                                  SM1034.2
009300                                                                  SM1034.2
009400*                                                                 SM1034.2
009500*********************** COPY STATEMENT USED **********************SM1034.2
009600*                                                                 SM1034.2
009700*I-O-CONTROL                                          COPY K3IOA  SM1034.2
009800*                                                                 SM1034.2
009900******************** COPIED TEXT BEGINS BELOW ********************SM1034.2
010000 I-O-CONTROL.                                          COPY K3IOA.SM1034.2
010100*********************** END OF COPIED TEXT ***********************SM1034.2
010200 DATA DIVISION.                                                   SM1034.2
010300 FILE SECTION.                                                    SM1034.2
010400 FD  PRINT-FILE.                                                  SM1034.2
010500 01  PRINT-REC PICTURE X(120).                                    SM1034.2
010600 01  DUMMY-RECORD PICTURE X(120).                                 SM1034.2
010700 FD  TEST-FILE                                                    SM1034.2
010800     LABEL RECORD STANDARD                                        SM1034.2
010900C    VALUE OF                                                     SM1034.2
011000C    XXXXX074                                                     SM1034.2
011100C    IS                                                           SM1034.2
011200C    XXXXX075                                                     SM1034.2
011300*    XXXXX069                                                     SM1034.2
011400     DATA RECORD TEST-REC.                                        SM1034.2
011500 01  TEST-REC.                                                    SM1034.2
011600     02  TST-FLD-1 PICTURE 9(5).                                  SM1034.2
011700     02  TST-FLD-2 PICTURE X(13).                                 SM1034.2
011800     02  FILLER PICTURE X(102).                                   SM1034.2
011900 FD  TEST-FILE2                                                   SM1034.2
012000     LABEL RECORD STANDARD                                        SM1034.2
012100C    VALUE OF                                                     SM1034.2
012200C    XXXXX074                                                     SM1034.2
012300C    IS                                                           SM1034.2
012400C    XXXXX076                                                     SM1034.2
012500*    XXXXX069                                                     SM1034.2
012600     DATA RECORD TEST-REC2.                                       SM1034.2
012700 01  TEST-REC2.                                                   SM1034.2
012800     02  TST-FLD-3 PICTURE 9(5).                                  SM1034.2
012900     02  TST-FLD-4 PICTURE X(13).                                 SM1034.2
013000     02  FILLER PICTURE X(102).                                   SM1034.2
013100 WORKING-STORAGE SECTION.                                         SM1034.2
013200 77  RCD-1 PICTURE 9(5) VALUE 97532.                              SM1034.2
013300 77  RCD-2 PICTURE 9(5) VALUE 23479.                              SM1034.2
013400 77  RCD-3 PICTURE 9(5) VALUE 10901.                              SM1034.2
013500 77  RCD-4 PICTURE 9(5) VALUE 02734.                              SM1034.2
013600 77  RCD-5 PICTURE 9(5) VALUE 14003.                              SM1034.2
013700 77  RCD-6 PICTURE 9(5) VALUE 19922.                              SM1034.2
013800 77  RCD-7 PICTURE 9(5) VALUE 03543.                              SM1034.2
013900 01  S-N-1 PICTURE 9(8)V99 VALUE IS 12345678,91.                  SM1034.2
014000 01  S-N-2 PICTURE ZZ.ZZZ.ZZZ,99.                                 SM1034.2
014100 01  WRK-DU-9                    PIC 9          VALUE ZERO.       SM1034.2
014200 01  WRK-DU-99                   PIC 99         VALUE ZERO.       SM1034.2
014300 01  WRK-DU-99-LONGER            PIC 99         VALUE ZERO.       SM1034.2
014400 01  WRK-DU-00001                PIC 9.                           SM1034.2
014500 01  WRK-XN-00322                PIC X(322).                      SM1034.2
014600 01  FILLER REDEFINES WRK-XN-00322.                               SM1034.2
014700   03  WRK-XN-00322-1         PIC X.                              SM1034.2
014800   03  WRK-XN-00322-2-322.                                        SM1034.2
014900     05  WRK-XN-00322-2-3     PIC X.                              SM1034.2
015000     05  WRK-XN-00322-20      PIC X(20)                           SM1034.2
015100                              OCCURS 16                           SM1034.2
015200                              INDEXED BY X1.                      SM1034.2
015300 01  TEST-RESULTS.                                                SM1034.2
015400     02 FILLER                   PIC X      VALUE SPACE.          SM1034.2
015500     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM1034.2
015600     02 FILLER                   PIC X      VALUE SPACE.          SM1034.2
015700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM1034.2
015800     02 FILLER                   PIC X      VALUE SPACE.          SM1034.2
015900     02  PAR-NAME.                                                SM1034.2
016000       03 FILLER                 PIC X(19)  VALUE SPACE.          SM1034.2
016100       03  PARDOT-X              PIC X      VALUE SPACE.          SM1034.2
016200       03 DOTVALUE               PIC 99     VALUE ZERO.           SM1034.2
016300     02 FILLER                   PIC X(8)   VALUE SPACE.          SM1034.2
016400     02 RE-MARK                  PIC X(61).                       SM1034.2
016500 01  TEST-COMPUTED.                                               SM1034.2
016600     02 FILLER                   PIC X(30)  VALUE SPACE.          SM1034.2
016700     02 FILLER                   PIC X(17)  VALUE                 SM1034.2
016800            "       COMPUTED=".                                   SM1034.2
016900     02 COMPUTED-X.                                               SM1034.2
017000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM1034.2
017100     03 COMPUTED-N               REDEFINES COMPUTED-A             SM1034.2
017200                                 PIC -9(9).9(9).                  SM1034.2
017300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM1034.2
017400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM1034.2
017500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM1034.2
017600     03       CM-18V0 REDEFINES COMPUTED-A.                       SM1034.2
017700         04 COMPUTED-18V0                    PIC -9(18).          SM1034.2
017800         04 FILLER                           PIC X.               SM1034.2
017900     03 FILLER PIC X(50) VALUE SPACE.                             SM1034.2
018000 01  TEST-CORRECT.                                                SM1034.2
018100     02 FILLER PIC X(30) VALUE SPACE.                             SM1034.2
018200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM1034.2
018300     02 CORRECT-X.                                                SM1034.2
018400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM1034.2
018500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM1034.2
018600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM1034.2
018700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM1034.2
018800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM1034.2
018900     03      CR-18V0 REDEFINES CORRECT-A.                         SM1034.2
019000         04 CORRECT-18V0                     PIC -9(18).          SM1034.2
019100         04 FILLER                           PIC X.               SM1034.2
019200     03 FILLER PIC X(2) VALUE SPACE.                              SM1034.2
019300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM1034.2
019400 01  CCVS-C-1.                                                    SM1034.2
019500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM1034.2
019600-    "SS  PARAGRAPH-NAME                                          SM1034.2
019700-    "       REMARKS".                                            SM1034.2
019800     02 FILLER                     PIC X(20)    VALUE SPACE.      SM1034.2
019900 01  CCVS-C-2.                                                    SM1034.2
020000     02 FILLER                     PIC X        VALUE SPACE.      SM1034.2
020100     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM1034.2
020200     02 FILLER                     PIC X(15)    VALUE SPACE.      SM1034.2
020300     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM1034.2
020400     02 FILLER                     PIC X(94)    VALUE SPACE.      SM1034.2
020500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM1034.2
020600 01  REC-CT                        PIC 99       VALUE ZERO.       SM1034.2
020700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM1034.2
020800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM1034.2
020900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM1034.2
021000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM1034.2
021100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM1034.2
021200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM1034.2
021300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM1034.2
021400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM1034.2
021500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM1034.2
021600 01  CCVS-H-1.                                                    SM1034.2
021700     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1034.2
021800     02  FILLER                    PIC X(42)    VALUE             SM1034.2
021900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM1034.2
022000     02  FILLER                    PIC X(39)    VALUE SPACES.     SM1034.2
022100 01  CCVS-H-2A.                                                   SM1034.2
022200   02  FILLER                        PIC X(40)  VALUE SPACE.      SM1034.2
022300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM1034.2
022400   02  FILLER                        PIC XXXX   VALUE             SM1034.2
022500     "4.2 ".                                                      SM1034.2
022600   02  FILLER                        PIC X(28)  VALUE             SM1034.2
022700            " COPY - NOT FOR DISTRIBUTION".                       SM1034.2
022800   02  FILLER                        PIC X(41)  VALUE SPACE.      SM1034.2
022900                                                                  SM1034.2
023000 01  CCVS-H-2B.                                                   SM1034.2
023100   02  FILLER                        PIC X(15)  VALUE             SM1034.2
023200            "TEST RESULT OF ".                                    SM1034.2
023300   02  TEST-ID                       PIC X(9).                    SM1034.2
023400   02  FILLER                        PIC X(4)   VALUE             SM1034.2
023500            " IN ".                                               SM1034.2
023600   02  FILLER                        PIC X(12)  VALUE             SM1034.2
023700     " HIGH       ".                                              SM1034.2
023800   02  FILLER                        PIC X(22)  VALUE             SM1034.2
023900            " LEVEL VALIDATION FOR ".                             SM1034.2
024000   02  FILLER                        PIC X(58)  VALUE             SM1034.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1034.2
024200 01  CCVS-H-3.                                                    SM1034.2
024300     02  FILLER                      PIC X(34)  VALUE             SM1034.2
024400            " FOR OFFICIAL USE ONLY    ".                         SM1034.2
024500     02  FILLER                      PIC X(58)  VALUE             SM1034.2
024600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM1034.2
024700     02  FILLER                      PIC X(28)  VALUE             SM1034.2
024800            "  COPYRIGHT   1985 ".                                SM1034.2
024900 01  CCVS-E-1.                                                    SM1034.2
025000     02 FILLER                       PIC X(52)  VALUE SPACE.      SM1034.2
025100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM1034.2
025200     02 ID-AGAIN                     PIC X(9).                    SM1034.2
025300     02 FILLER                       PIC X(45)  VALUE SPACES.     SM1034.2
025400 01  CCVS-E-2.                                                    SM1034.2
025500     02  FILLER                      PIC X(31)  VALUE SPACE.      SM1034.2
025600     02  FILLER                      PIC X(21)  VALUE SPACE.      SM1034.2
025700     02 CCVS-E-2-2.                                               SM1034.2
025800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM1034.2
025900         03 FILLER                   PIC X      VALUE SPACE.      SM1034.2
026000         03 ENDER-DESC               PIC X(44)  VALUE             SM1034.2
026100            "ERRORS ENCOUNTERED".                                 SM1034.2
026200 01  CCVS-E-3.                                                    SM1034.2
026300     02  FILLER                      PIC X(22)  VALUE             SM1034.2
026400            " FOR OFFICIAL USE ONLY".                             SM1034.2
026500     02  FILLER                      PIC X(12)  VALUE SPACE.      SM1034.2
026600     02  FILLER                      PIC X(58)  VALUE             SM1034.2
026700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM1034.2
026800     02  FILLER                      PIC X(13)  VALUE SPACE.      SM1034.2
026900     02 FILLER                       PIC X(15)  VALUE             SM1034.2
027000             " COPYRIGHT 1985".                                   SM1034.2
027100 01  CCVS-E-4.                                                    SM1034.2
027200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM1034.2
027300     02 FILLER                       PIC X(4)   VALUE " OF ".     SM1034.2
027400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM1034.2
027500     02 FILLER                       PIC X(40)  VALUE             SM1034.2
027600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM1034.2
027700 01  XXINFO.                                                      SM1034.2
027800     02 FILLER                       PIC X(19)  VALUE             SM1034.2
027900            "*** INFORMATION ***".                                SM1034.2
028000     02 INFO-TEXT.                                                SM1034.2
028100       04 FILLER                     PIC X(8)   VALUE SPACE.      SM1034.2
028200       04 XXCOMPUTED                 PIC X(20).                   SM1034.2
028300       04 FILLER                     PIC X(5)   VALUE SPACE.      SM1034.2
028400       04 XXCORRECT                  PIC X(20).                   SM1034.2
028500     02 INF-ANSI-REFERENCE           PIC X(48).                   SM1034.2
028600 01  HYPHEN-LINE.                                                 SM1034.2
028700     02 FILLER  PIC IS X VALUE IS SPACE.                          SM1034.2
028800     02 FILLER  PIC IS X(65)    VALUE IS "************************SM1034.2
028900-    "*****************************************".                 SM1034.2
029000     02 FILLER  PIC IS X(54)    VALUE IS "************************SM1034.2
029100-    "******************************".                            SM1034.2
029200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM1034.2
029300     "SM103A".                                                    SM1034.2
029400 PROCEDURE DIVISION.                                              SM1034.2
029500 CCVS1 SECTION.                                                   SM1034.2
029600 OPEN-FILES.                                                      SM1034.2
029700     OPEN     OUTPUT PRINT-FILE.                                  SM1034.2
029800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM1034.2
029900     MOVE    SPACE TO TEST-RESULTS.                               SM1034.2
030000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM1034.2
030100     GO TO CCVS1-EXIT.                                            SM1034.2
030200 CLOSE-FILES.                                                     SM1034.2
030300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM1034.2
030400 TERMINATE-CCVS.                                                  SM1034.2
030500*    EXIT PROGRAM.                                                SM1034.2
030600*TERMINATE-CALL.                                                  SM1034.2
030700     STOP     RUN.                                                SM1034.2
030800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM1034.2
030900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM1034.2
031000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM1034.2
031100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM1034.2
031200     MOVE "****TEST DELETED****" TO RE-MARK.                      SM1034.2
031300 PRINT-DETAIL.                                                    SM1034.2
031400     IF REC-CT NOT EQUAL TO ZERO                                  SM1034.2
031500             MOVE "." TO PARDOT-X                                 SM1034.2
031600             MOVE REC-CT TO DOTVALUE.                             SM1034.2
031700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM1034.2
031800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM1034.2
031900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM1034.2
032000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM1034.2
032100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM1034.2
032200     MOVE SPACE TO CORRECT-X.                                     SM1034.2
032300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM1034.2
032400     MOVE     SPACE TO RE-MARK.                                   SM1034.2
032500 HEAD-ROUTINE.                                                    SM1034.2
032600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1034.2
032700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM1034.2
032800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1034.2
032900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM1034.2
033000 COLUMN-NAMES-ROUTINE.                                            SM1034.2
033100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1034.2
033200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1034.2
033300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM1034.2
033400 END-ROUTINE.                                                     SM1034.2
033500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM1034.2
033600 END-RTN-EXIT.                                                    SM1034.2
033700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1034.2
033800 END-ROUTINE-1.                                                   SM1034.2
033900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM1034.2
034000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM1034.2
034100      ADD PASS-COUNTER TO ERROR-HOLD.                             SM1034.2
034200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM1034.2
034300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM1034.2
034400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM1034.2
034500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM1034.2
034600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM1034.2
034700  END-ROUTINE-12.                                                 SM1034.2
034800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM1034.2
034900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM1034.2
035000         MOVE "NO " TO ERROR-TOTAL                                SM1034.2
035100         ELSE                                                     SM1034.2
035200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM1034.2
035300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM1034.2
035400     PERFORM WRITE-LINE.                                          SM1034.2
035500 END-ROUTINE-13.                                                  SM1034.2
035600     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM1034.2
035700         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM1034.2
035800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM1034.2
035900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM1034.2
036000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1034.2
036100      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM1034.2
036200          MOVE "NO " TO ERROR-TOTAL                               SM1034.2
036300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM1034.2
036400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM1034.2
036500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM1034.2
036600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM1034.2
036700 WRITE-LINE.                                                      SM1034.2
036800     ADD 1 TO RECORD-COUNT.                                       SM1034.2
036900*    IF RECORD-COUNT GREATER 50                                   SM1034.2
037000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM1034.2
037100*        MOVE SPACE TO DUMMY-RECORD                               SM1034.2
037200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM1034.2
037300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM1034.2
037400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM1034.2
037500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM1034.2
037600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM1034.2
037700*        MOVE ZERO TO RECORD-COUNT.                               SM1034.2
037800     PERFORM WRT-LN.                                              SM1034.2
037900 WRT-LN.                                                          SM1034.2
038000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM1034.2
038100     MOVE SPACE TO DUMMY-RECORD.                                  SM1034.2
038200 BLANK-LINE-PRINT.                                                SM1034.2
038300     PERFORM WRT-LN.                                              SM1034.2
038400 FAIL-ROUTINE.                                                    SM1034.2
038500     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM1034.2
038600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM1034.2
038700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1034.2
038800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM1034.2
038900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1034.2
039000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1034.2
039100     GO TO  FAIL-ROUTINE-EX.                                      SM1034.2
039200 FAIL-ROUTINE-WRITE.                                              SM1034.2
039300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM1034.2
039400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM1034.2
039500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM1034.2
039600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM1034.2
039700 FAIL-ROUTINE-EX. EXIT.                                           SM1034.2
039800 BAIL-OUT.                                                        SM1034.2
039900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM1034.2
040000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM1034.2
040100 BAIL-OUT-WRITE.                                                  SM1034.2
040200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM1034.2
040300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM1034.2
040400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM1034.2
040500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM1034.2
040600 BAIL-OUT-EX. EXIT.                                               SM1034.2
040700 CCVS1-EXIT.                                                      SM1034.2
040800     EXIT.                                                        SM1034.2
040900 INITIALIZATION SECTION.                                          SM1034.2
041000 SM103A-INIT.                                                     SM1034.2
041100     MOVE     "ALL TESTS IN SM103A PRODUCE" TO RE-MARK.           SM1034.2
041200     PERFORM  PRINT-DETAIL.                                       SM1034.2
041300     MOVE     "OUTPUT CHECKED IN SM104A."   TO RE-MARK.           SM1034.2
041400     PERFORM  PRINT-DETAIL.                                       SM1034.2
041500     MOVE     "COPY ---" TO FEATURE.                              SM1034.2
041600     PERFORM  PRINT-DETAIL.                                       SM1034.2
041700 SPECIAL-NAMES-TEST SECTION.                                      SM1034.2
041800 COPY-TEST-1.                                                     SM1034.2
041900     MOVE     S-N-1 TO S-N-2.                                     SM1034.2
042000*    NOTE     THIS ROUTINE USES A COPIED DECIMAL-POINT IS COMMA   SM1034.2
042100*             CLAUSE IN SPECIAL-NAMES --- THE EDITING IN S-N-2    SM1034.2
042200*             WOULD NOT BE VALID WITHOUT THIS CLAUSE.             SM1034.2
042300     PERFORM  PASS.                                               SM1034.2
042400     GO       TO COPY-WRITE-1.                                    SM1034.2
042500 COPY-DELETE-1.                                                   SM1034.2
042600     PERFORM  DE-LETE.                                            SM1034.2
042700 COPY-WRITE-1.                                                    SM1034.2
042800     MOVE     "  DEC POINT IS COMMA" TO FEATURE.                  SM1034.2
042900     MOVE     "COPY-TEST-1 " TO PAR-NAME.                         SM1034.2
043000     PERFORM  PRINT-DETAIL.                                       SM1034.2
043100 BUILD SECTION.                                                   SM1034.2
043200 COPY-TEST-2.                                                     SM1034.2
043300     OPEN     OUTPUT TEST-FILE.                                   SM1034.2
043400     MOVE     S-N-2 TO TST-FLD-2.                                 SM1034.2
043500     MOVE     RCD-1 TO TST-FLD-1.                                 SM1034.2
043600     WRITE    TEST-REC.                                           SM1034.2
043700     MOVE     RCD-2 TO TST-FLD-1.                                 SM1034.2
043800     WRITE    TEST-REC.                                           SM1034.2
043900     MOVE     RCD-3 TO TST-FLD-1.                                 SM1034.2
044000     WRITE    TEST-REC.                                           SM1034.2
044100     MOVE     RCD-4 TO TST-FLD-1.                                 SM1034.2
044200     WRITE    TEST-REC.                                           SM1034.2
044300     MOVE     RCD-5 TO TST-FLD-1.                                 SM1034.2
044400     WRITE    TEST-REC.                                           SM1034.2
044500     MOVE     RCD-6 TO TST-FLD-1.                                 SM1034.2
044600     WRITE    TEST-REC.                                           SM1034.2
044700     MOVE     RCD-7 TO TST-FLD-1.                                 SM1034.2
044800     WRITE    TEST-REC.                                           SM1034.2
044900     CLOSE    TEST-FILE.                                          SM1034.2
045000     OPEN OUTPUT TEST-FILE2.                                      SM1034.2
045100     MOVE ZERO TO TST-FLD-3.                                      SM1034.2
045200     MOVE "DDDDD" TO TST-FLD-4.                                   SM1034.2
045300     WRITE TEST-REC2.                                             SM1034.2
045400     CLOSE TEST-FILE2.                                            SM1034.2
045500     PERFORM  PASS.                                               SM1034.2
045600     GO       TO COPY-WRITE-2.                                    SM1034.2
045700 COPY-DELETE-2.                                                   SM1034.2
045800     PERFORM DE-LETE.                                             SM1034.2
045900 COPY-WRITE-2.                                                    SM1034.2
046000     MOVE   "  ENVIR DIV ENTRIES" TO FEATURE.                     SM1034.2
046100     MOVE   "COPY-TEST-2 " TO PAR-NAME.                           SM1034.2
046200     PERFORM PRINT-DETAIL.                                        SM1034.2
046300*                                                                 SM1034.2
046400 COPY-TEST-3.                                                     SM1034.2
046500*    ===-->  MINIMUM LENGTH TEXT WORD  <--===                     SM1034.2
046600     MOVE   "XII-2 2.3 SR8" TO ANSI-REFERENCE.                    SM1034.2
046700     MOVE   "COPY-TEST-3"   TO PAR-NAME.                          SM1034.2
046800     MOVE    8  TO WRK-DU-00001.                                  SM1034.2
046900     GO TO   COPY-TEST-3-0.                                       SM1034.2
047000 COPY-DELETE-3.                                                   SM1034.2
047100     PERFORM DE-LETE.                                             SM1034.2
047200     PERFORM PRINT-DETAIL.                                        SM1034.2
047300     GO TO   COPY-INIT-4.                                         SM1034.2
047400 COPY-TEST-3-0.                                                   SM1034.2
047500********************* COPY TEXT USED ***************************  SM1034.2
047600*                            8                                 *  SM1034.2
047700*********************END OF COPY TEXT***************************  SM1034.2
047800     IF      WRK-DU-00001 =                                       SM1034.2
047900     COPY    K3SML.                                               SM1034.2
048000             PERFORM PASS                                         SM1034.2
048100             PERFORM PRINT-DETAIL                                 SM1034.2
048200     ELSE                                                         SM1034.2
048300             MOVE   "COPYING SINGLE CHARACTER FAILED"             SM1034.2
048400                  TO RE-MARK                                      SM1034.2
048500             MOVE    8   TO CORRECT-N                             SM1034.2
048600             MOVE    WRK-DU-00001 TO COMPUTED-N                   SM1034.2
048700             PERFORM FAIL                                         SM1034.2
048800             PERFORM PRINT-DETAIL.                                SM1034.2
048900*                                                                 SM1034.2
049000 COPY-INIT-4.                                                     SM1034.2
049100*    ===-->  MAXIMUM LENGTH TEXT WORD  <--===                     SM1034.2
049200     MOVE   "XII-2 2.3 (SR8) AND XII-5 2.4(GR11)"                 SM1034.2
049300          TO ANSI-REFERENCE.                                      SM1034.2
049400     MOVE   "COPY-TEST-4" TO PAR-NAME.                            SM1034.2
049500     MOVE    SPACES      TO WRK-XN-00322.                         SM1034.2
049600     MOVE    1 TO REC-CT.                                         SM1034.2
049700     GO TO   COPY-TEST-4-0.                                       SM1034.2
049800 COPY-DELETE-4.                                                   SM1034.2
049900     PERFORM DE-LETE.                                             SM1034.2
050000     PERFORM PRINT-DETAIL.                                        SM1034.2
050100     GO TO   CCVS-EXIT.                                           SM1034.2
050200 COPY-TEST-4-0.                                                   SM1034.2
050300********************* COPY TEXT USED ***************************  SM1034.2
050400*    MOVE 1 TO WRK-DU-9, ADD 1 TO WRK-DU-9, ADD 1 TO WRK-DU-9, ADDSM1034.2
050500*     1 TO WRK-DU-99, ADD 1 TO WRK-DU-9, ADD 1 TO WRK-DU-99, ADD 1SM1034.2
050600*     TO WRK-DU-99, ADD 1 TO WRK-DU-99, ADD 1 TO WRK-DU-99, ADD 1 SM1034.2
050700*    TO WRK-DU-9, ADD 1 TO WRK-DU-9, ADD 1 TO WRK-DU-99, ADD 1 TO SM1034.2
050800*    WRK-DU-99, ADD 1 TO WRK-DU-99, ADD 1 TO WRK-DU-99, ADD 10 TO SM1034.2
050900*    WRK-DU-99-LONGER.                                            SM1034.2
051000*********************END OF COPY TEXT***************************  SM1034.2
051100*                                                                 SM1034.2
051200     COPY    K3LGE.                                               SM1034.2
051300*                                                                 SM1034.2
051400 COPY-TEST-4-1.                                                   SM1034.2
051500     MOVE   "COPY-TEST-4-1" TO PAR-NAME.                          SM1034.2
051600     IF      WRK-DU-9 = 6                                         SM1034.2
051700             PERFORM PASS                                         SM1034.2
051800             PERFORM PRINT-DETAIL                                 SM1034.2
051900     ELSE                                                         SM1034.2
052000             MOVE   "COPYING ALL 322 CHARACTERS FAILED"           SM1034.2
052100                  TO RE-MARK                                      SM1034.2
052200             MOVE    6   TO CORRECT-N                             SM1034.2
052300             MOVE    WRK-DU-9 TO COMPUTED-N                       SM1034.2
052400             PERFORM FAIL                                         SM1034.2
052500             PERFORM PRINT-DETAIL.                                SM1034.2
052600     ADD     1 TO REC-CT.                                         SM1034.2
052700 COPY-TEST-4-2.                                                   SM1034.2
052800     MOVE   "COPY-TEST-4-2" TO PAR-NAME.                          SM1034.2
052900     IF      WRK-DU-99 = 9                                        SM1034.2
053000             PERFORM PASS                                         SM1034.2
053100             PERFORM PRINT-DETAIL                                 SM1034.2
053200     ELSE                                                         SM1034.2
053300             MOVE   "COPYING ALL 322 CHARACTERS FAILED"           SM1034.2
053400                  TO RE-MARK                                      SM1034.2
053500             MOVE    9   TO CORRECT-N                             SM1034.2
053600             MOVE    WRK-DU-99 TO COMPUTED-N                      SM1034.2
053700             PERFORM FAIL                                         SM1034.2
053800             PERFORM PRINT-DETAIL.                                SM1034.2
053900     ADD     1 TO REC-CT.                                         SM1034.2
054000 COPY-TEST-4-3.                                                   SM1034.2
054100     MOVE   "COPY-TEST-4-3" TO PAR-NAME.                          SM1034.2
054200     IF      WRK-DU-99-LONGER = 10                                SM1034.2
054300             PERFORM PASS                                         SM1034.2
054400             PERFORM PRINT-DETAIL                                 SM1034.2
054500     ELSE                                                         SM1034.2
054600             MOVE   "COPYING ALL 322 CHARACTERS FAILED"           SM1034.2
054700                  TO RE-MARK                                      SM1034.2
054800             MOVE    10  TO CORRECT-N                             SM1034.2
054900             MOVE    WRK-DU-99-LONGER TO COMPUTED-N               SM1034.2
055000             PERFORM FAIL                                         SM1034.2
055100             PERFORM PRINT-DETAIL.                                SM1034.2
055200*                                                                 SM1034.2
055300 CCVS-EXIT SECTION.                                               SM1034.2
055400 CCVS-999999.                                                     SM1034.2
055500     GO TO CLOSE-FILES.                                           SM1034.2
      *END-OF,SM103A                                                                  
