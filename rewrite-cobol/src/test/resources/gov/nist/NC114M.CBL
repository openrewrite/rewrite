      *HEADER,COBOL,NC114M                                                            
000100 IDENTIFICATION DIVISION.                                         NC1144.2
000200 PROGRAM-ID.                                                      NC1144.2
000300     NC114M.                                                      NC1144.2
000400****************************************************************  NC1144.2
000500*                                                              *  NC1144.2
000600*    VALIDATION FOR:-                                          *  NC1144.2
000700*                                                              *  NC1144.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1144.2
000900*                                                              *  NC1144.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1144.2
001100*                                                              *  NC1144.2
001200****************************************************************  NC1144.2
001300*                                                              *  NC1144.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1144.2
001500*                                                              *  NC1144.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1144.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1144.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1144.2
001900*                                                              *  NC1144.2
002000****************************************************************  NC1144.2
002100*                                                                 NC1144.2
002200*    PROGRAM NC114M TESTS:                                        NC1144.2
002300*                                                                 NC1144.2
002400*        ALPHA-NUMERIC EDITING                                    NC1144.2
002500*        COMMENT LINES                                            NC1144.2
002600*        UNIQUENESS OF REFERENCE (DATA, PARAGRAPH & SECTION NAME) NC1144.2
002700*        SEQUENCE NUMBERING                                       NC1144.2
002800*                                                                 NC1144.2
002900******************************************************************NC1144.2
003000*                                                                 NC1144.2
003100 ENVIRONMENT DIVISION.                                            NC1144.2
003200 CONFIGURATION SECTION.                                           NC1144.2
003300 SOURCE-COMPUTER.                                                 NC1144.2
003400     XXXXX082.                                                    NC1144.2
003500 OBJECT-COMPUTER.                                                 NC1144.2
003600     XXXXX083                                                     NC1144.2
003700      PROGRAM COLLATING SEQUENCE IS AMERICAN-INDIAN.              NC1144.2
003800 SPECIAL-NAMES.                                                   NC1144.2
003900     ALPHABET AMERICAN-INDIAN IS NATIVE.                          NC1144.2
004000 INPUT-OUTPUT SECTION.                                            NC1144.2
004100 FILE-CONTROL.                                                    NC1144.2
004200     SELECT PRINT-FILE ASSIGN TO                                  NC1144.2
004300     XXXXX055.                                                    NC1144.2
004400 DATA DIVISION.                                                   NC1144.2
004500 FILE SECTION.                                                    NC1144.2
004600 FD  PRINT-FILE.                                                  NC1144.2
004700 01  PRINT-REC PICTURE X(120).                                    NC1144.2
004800 01  DUMMY-RECORD PICTURE X(120).                                 NC1144.2
004900 WORKING-STORAGE SECTION.                                         NC1144.2
005000 01  NINE-DU-9  PIC 9 VALUE 9.                                    NC1144.2
005100 01  WRK-DU-99-1  PIC 99.                                         NC1144.2
005200 01  WRK-DU-99-2  PIC 99.                                         NC1144.2
005300 01  WRK-AE-1 PIC ABA VALUE "ABC".                                NC1144.2
005400 01  WRK-AE-2 PIC A/AA.                                           NC1144.2
005500 01  WRK-AE-3  PIC XBXXX/XXX/XXX/XXX/XXXBXX.                      NC1144.2
005600 01  WRK-NE-1 PIC 9/99   .                                        NC1144.2
005700/ STROKE-COMMENT-TEST-7   SHOULD PAGE EJECT AND PRINT ON TOP LINE NC1144.2
005800/ STROKE-COMMENT-TEST-7   SHOULD PAGE EJECT AND PRINT ON TOP LINE NC1144.2
005900* ASTERISK COMMENT SHOULD NOT BE SYNTAX CHECKED "(,$*.)-/+> <,....NC1144.2
006000*SIGN-LEADING-TEST-9     ) ,  >   ;   <                           NC1144.2
006100 01  WRK-DS-L-18V0-1 PIC S9(18)    SIGN IS LEADING      .         NC1144.2
006200* SIGN-TRAILING-TEST-10                                           NC1144.2
006300 01  WRK-DS-T-18V0-1 PIC S9(18)  SIGN TRAILING .                  NC1144.2
006400* SIGN-SEPARATE-TEST-11                                           NC1144.2
006500 01  WRK-DS-S-18V0-1 PIC S9(18) SIGN TRAILING SEPARATE CHARACTER. NC1144.2
006600* REDEFINITION-TEST-12                                            NC1144.2
006700 01  WRK-XN-18-1 PIC X(18).                                       NC1144.2
006800 01  WRK-AN-18-X-1 REDEFINES WRK-XN-18-1 PIC A(18).               NC1144.2
006900 01  GRP-X-1 REDEFINES WRK-XN-18-1.                               NC1144.2
007000 02  WRK-DU-9V0-1 PIC 9(9).                                       NC1144.2
007100  02 WRK-DU-9V0-2 PIC 9(9).                                       NC1144.2
007200 01  WRK-DS-18V0-1 PIC S9(18)                                     NC1144.2
007300     VALUE  -123456789012345678.                                  NC1144.2
007400 01  WRK-XN-18-2 PIC X(18) VALUE "123456789012345678".            NC1144.2
007500 01  WRK-DS-LS-1P17-1 PIC S9P(17) SIGN LEADING SEPARATE           NC1144.2
007600     CHARACTER VALUE  -100000000000000000.                        NC1144.2
007700/ STROKE-COMMENT-TEST-7   SHOULD PAGE EJECT AND PRINT ON TOP LINE NC1144.2
007800 01  PIC-SYNTAX-TEST-19  PIC AB9.                                 NC1144.2
007900/ STROKE-COMMENT-TEST-7   SHOULD PAGE EJECT AND PRINT ON TOP LINE NC1144.2
008000 01  TEST-RESULTS.                                                NC1144.2
008100     02 FILLER                   PIC X      VALUE SPACE.          NC1144.2
008200     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC1144.2
008300     02 FILLER                   PIC X      VALUE SPACE.          NC1144.2
008400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC1144.2
008500     02 FILLER                   PIC X      VALUE SPACE.          NC1144.2
008600     02  PAR-NAME.                                                NC1144.2
008700       03 FILLER                 PIC X(19)  VALUE SPACE.          NC1144.2
008800       03  PARDOT-X              PIC X      VALUE SPACE.          NC1144.2
008900       03 DOTVALUE               PIC 99     VALUE ZERO.           NC1144.2
009000     02 FILLER                   PIC X(8)   VALUE SPACE.          NC1144.2
009100     02 RE-MARK                  PIC X(61).                       NC1144.2
009200 01  TEST-COMPUTED.                                               NC1144.2
009300     02 FILLER                   PIC X(30)  VALUE SPACE.          NC1144.2
009400     02 FILLER                   PIC X(17)  VALUE                 NC1144.2
009500            "       COMPUTED=".                                   NC1144.2
009600     02 COMPUTED-X.                                               NC1144.2
009700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC1144.2
009800     03 COMPUTED-N               REDEFINES COMPUTED-A             NC1144.2
009900                                 PIC -9(9).9(9).                  NC1144.2
010000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC1144.2
010100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC1144.2
010200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC1144.2
010300     03       CM-18V0 REDEFINES COMPUTED-A.                       NC1144.2
010400         04 COMPUTED-18V0                    PIC -9(18).          NC1144.2
010500         04 FILLER                           PIC X.               NC1144.2
010600     03 FILLER PIC X(50) VALUE SPACE.                             NC1144.2
010700 01  TEST-CORRECT.                                                NC1144.2
010800     02 FILLER PIC X(30) VALUE SPACE.                             NC1144.2
010900     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC1144.2
011000     02 CORRECT-X.                                                NC1144.2
011100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC1144.2
011200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC1144.2
011300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC1144.2
011400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC1144.2
011500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC1144.2
011600     03      CR-18V0 REDEFINES CORRECT-A.                         NC1144.2
011700         04 CORRECT-18V0                     PIC -9(18).          NC1144.2
011800         04 FILLER                           PIC X.               NC1144.2
011900     03 FILLER PIC X(2) VALUE SPACE.                              NC1144.2
012000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC1144.2
012100 01  CCVS-C-1.                                                    NC1144.2
012200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC1144.2
012300-    "SS  PARAGRAPH-NAME                                          NC1144.2
012400-    "       REMARKS".                                            NC1144.2
012500     02 FILLER                     PIC X(20)    VALUE SPACE.      NC1144.2
012600 01  CCVS-C-2.                                                    NC1144.2
012700     02 FILLER                     PIC X        VALUE SPACE.      NC1144.2
012800     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC1144.2
012900     02 FILLER                     PIC X(15)    VALUE SPACE.      NC1144.2
013000     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC1144.2
013100     02 FILLER                     PIC X(94)    VALUE SPACE.      NC1144.2
013200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC1144.2
013300 01  REC-CT                        PIC 99       VALUE ZERO.       NC1144.2
013400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC1144.2
013500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC1144.2
013600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC1144.2
013700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC1144.2
013800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC1144.2
013900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC1144.2
014000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC1144.2
014100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC1144.2
014200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC1144.2
014300 01  CCVS-H-1.                                                    NC1144.2
014400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1144.2
014500     02  FILLER                    PIC X(42)    VALUE             NC1144.2
014600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC1144.2
014700     02  FILLER                    PIC X(39)    VALUE SPACES.     NC1144.2
014800 01  CCVS-H-2A.                                                   NC1144.2
014900   02  FILLER                        PIC X(40)  VALUE SPACE.      NC1144.2
015000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC1144.2
015100   02  FILLER                        PIC XXXX   VALUE             NC1144.2
015200     "4.2 ".                                                      NC1144.2
015300   02  FILLER                        PIC X(28)  VALUE             NC1144.2
015400            " COPY - NOT FOR DISTRIBUTION".                       NC1144.2
015500   02  FILLER                        PIC X(41)  VALUE SPACE.      NC1144.2
015600                                                                  NC1144.2
015700 01  CCVS-H-2B.                                                   NC1144.2
015800   02  FILLER                        PIC X(15)  VALUE             NC1144.2
015900            "TEST RESULT OF ".                                    NC1144.2
016000   02  TEST-ID                       PIC X(9).                    NC1144.2
016100   02  FILLER                        PIC X(4)   VALUE             NC1144.2
016200            " IN ".                                               NC1144.2
016300   02  FILLER                        PIC X(12)  VALUE             NC1144.2
016400     " HIGH       ".                                              NC1144.2
016500   02  FILLER                        PIC X(22)  VALUE             NC1144.2
016600            " LEVEL VALIDATION FOR ".                             NC1144.2
016700   02  FILLER                        PIC X(58)  VALUE             NC1144.2
016800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1144.2
016900 01  CCVS-H-3.                                                    NC1144.2
017000     02  FILLER                      PIC X(34)  VALUE             NC1144.2
017100            " FOR OFFICIAL USE ONLY    ".                         NC1144.2
017200     02  FILLER                      PIC X(58)  VALUE             NC1144.2
017300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1144.2
017400     02  FILLER                      PIC X(28)  VALUE             NC1144.2
017500            "  COPYRIGHT   1985 ".                                NC1144.2
017600 01  CCVS-E-1.                                                    NC1144.2
017700     02 FILLER                       PIC X(52)  VALUE SPACE.      NC1144.2
017800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC1144.2
017900     02 ID-AGAIN                     PIC X(9).                    NC1144.2
018000     02 FILLER                       PIC X(45)  VALUE SPACES.     NC1144.2
018100 01  CCVS-E-2.                                                    NC1144.2
018200     02  FILLER                      PIC X(31)  VALUE SPACE.      NC1144.2
018300     02  FILLER                      PIC X(21)  VALUE SPACE.      NC1144.2
018400     02 CCVS-E-2-2.                                               NC1144.2
018500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC1144.2
018600         03 FILLER                   PIC X      VALUE SPACE.      NC1144.2
018700         03 ENDER-DESC               PIC X(44)  VALUE             NC1144.2
018800            "ERRORS ENCOUNTERED".                                 NC1144.2
018900 01  CCVS-E-3.                                                    NC1144.2
019000     02  FILLER                      PIC X(22)  VALUE             NC1144.2
019100            " FOR OFFICIAL USE ONLY".                             NC1144.2
019200     02  FILLER                      PIC X(12)  VALUE SPACE.      NC1144.2
019300     02  FILLER                      PIC X(58)  VALUE             NC1144.2
019400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1144.2
019500     02  FILLER                      PIC X(13)  VALUE SPACE.      NC1144.2
019600     02 FILLER                       PIC X(15)  VALUE             NC1144.2
019700             " COPYRIGHT 1985".                                   NC1144.2
019800 01  CCVS-E-4.                                                    NC1144.2
019900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC1144.2
020000     02 FILLER                       PIC X(4)   VALUE " OF ".     NC1144.2
020100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC1144.2
020200     02 FILLER                       PIC X(40)  VALUE             NC1144.2
020300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC1144.2
020400 01  XXINFO.                                                      NC1144.2
020500     02 FILLER                       PIC X(19)  VALUE             NC1144.2
020600            "*** INFORMATION ***".                                NC1144.2
020700     02 INFO-TEXT.                                                NC1144.2
020800       04 FILLER                     PIC X(8)   VALUE SPACE.      NC1144.2
020900       04 XXCOMPUTED                 PIC X(20).                   NC1144.2
021000       04 FILLER                     PIC X(5)   VALUE SPACE.      NC1144.2
021100       04 XXCORRECT                  PIC X(20).                   NC1144.2
021200     02 INF-ANSI-REFERENCE           PIC X(48).                   NC1144.2
021300 01  HYPHEN-LINE.                                                 NC1144.2
021400     02 FILLER  PIC IS X VALUE IS SPACE.                          NC1144.2
021500     02 FILLER  PIC IS X(65)    VALUE IS "************************NC1144.2
021600-    "*****************************************".                 NC1144.2
021700     02 FILLER  PIC IS X(54)    VALUE IS "************************NC1144.2
021800-    "******************************".                            NC1144.2
021900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC1144.2
022000     "NC114M".                                                    NC1144.2
022100 PROCEDURE DIVISION.                                              NC1144.2
022200 CCVS1 SECTION.                                                   NC1144.2
022300 OPEN-FILES.                                                      NC1144.2
022400     OPEN     OUTPUT PRINT-FILE.                                  NC1144.2
022500     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC1144.2
022600     MOVE    SPACE TO TEST-RESULTS.                               NC1144.2
022700     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1144.2
022800     GO TO CCVS1-EXIT.                                            NC1144.2
022900 CLOSE-FILES.                                                     NC1144.2
023000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC1144.2
023100 TERMINATE-CCVS.                                                  NC1144.2
023200*    EXIT PROGRAM.                                                NC1144.2
023300*TERMINATE-CALL.                                                  NC1144.2
023400     STOP     RUN.                                                NC1144.2
023500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC1144.2
023600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC1144.2
023700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC1144.2
023800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC1144.2
023900     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1144.2
024000 PRINT-DETAIL.                                                    NC1144.2
024100     IF REC-CT NOT EQUAL TO ZERO                                  NC1144.2
024200             MOVE "." TO PARDOT-X                                 NC1144.2
024300             MOVE REC-CT TO DOTVALUE.                             NC1144.2
024400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC1144.2
024500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC1144.2
024600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC1144.2
024700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC1144.2
024800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC1144.2
024900     MOVE SPACE TO CORRECT-X.                                     NC1144.2
025000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC1144.2
025100     MOVE     SPACE TO RE-MARK.                                   NC1144.2
025200 HEAD-ROUTINE.                                                    NC1144.2
025300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1144.2
025400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC1144.2
025500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1144.2
025600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC1144.2
025700 COLUMN-NAMES-ROUTINE.                                            NC1144.2
025800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1144.2
025900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1144.2
026000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC1144.2
026100 END-ROUTINE.                                                     NC1144.2
026200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC1144.2
026300 END-RTN-EXIT.                                                    NC1144.2
026400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1144.2
026500 END-ROUTINE-1.                                                   NC1144.2
026600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC1144.2
026700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC1144.2
026800      ADD PASS-COUNTER TO ERROR-HOLD.                             NC1144.2
026900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC1144.2
027000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC1144.2
027100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC1144.2
027200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC1144.2
027300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC1144.2
027400  END-ROUTINE-12.                                                 NC1144.2
027500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC1144.2
027600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC1144.2
027700         MOVE "NO " TO ERROR-TOTAL                                NC1144.2
027800         ELSE                                                     NC1144.2
027900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC1144.2
028000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC1144.2
028100     PERFORM WRITE-LINE.                                          NC1144.2
028200 END-ROUTINE-13.                                                  NC1144.2
028300     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC1144.2
028400         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC1144.2
028500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC1144.2
028600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC1144.2
028700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1144.2
028800      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC1144.2
028900          MOVE "NO " TO ERROR-TOTAL                               NC1144.2
029000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC1144.2
029100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC1144.2
029200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC1144.2
029300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC1144.2
029400 WRITE-LINE.                                                      NC1144.2
029500     ADD 1 TO RECORD-COUNT.                                       NC1144.2
029600*    IF RECORD-COUNT GREATER 42                                   NC1144.2
029700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC1144.2
029800*        MOVE SPACE TO DUMMY-RECORD                               NC1144.2
029900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC1144.2
030000*        MOVE CCVS-H-1  TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1144.2
030100*        MOVE CCVS-H-2A TO DUMMY-RECORD  PERFORM WRT-LN 2 TIMES   NC1144.2
030200*        MOVE CCVS-H-2B TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1144.2
030300*        MOVE CCVS-H-3  TO DUMMY-RECORD  PERFORM WRT-LN 3 TIMES   NC1144.2
030400*        MOVE CCVS-C-1  TO DUMMY-RECORD  PERFORM WRT-LN           NC1144.2
030500*        MOVE CCVS-C-2  TO DUMMY-RECORD  PERFORM WRT-LN           NC1144.2
030600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC1144.2
030700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC1144.2
030800*        MOVE ZERO TO RECORD-COUNT.                               NC1144.2
030900     PERFORM WRT-LN.                                              NC1144.2
031000 WRT-LN.                                                          NC1144.2
031100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC1144.2
031200     MOVE SPACE TO DUMMY-RECORD.                                  NC1144.2
031300 BLANK-LINE-PRINT.                                                NC1144.2
031400     PERFORM WRT-LN.                                              NC1144.2
031500 FAIL-ROUTINE.                                                    NC1144.2
031600     IF     COMPUTED-X NOT EQUAL TO SPACE                         NC1144.2
031700            GO TO FAIL-ROUTINE-WRITE.                             NC1144.2
031800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC1144.2
031900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1144.2
032000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC1144.2
032100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1144.2
032200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1144.2
032300     GO TO  FAIL-ROUTINE-EX.                                      NC1144.2
032400 FAIL-ROUTINE-WRITE.                                              NC1144.2
032500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC1144.2
032600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC1144.2
032700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC1144.2
032800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC1144.2
032900 FAIL-ROUTINE-EX. EXIT.                                           NC1144.2
033000 BAIL-OUT.                                                        NC1144.2
033100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC1144.2
033200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC1144.2
033300 BAIL-OUT-WRITE.                                                  NC1144.2
033400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC1144.2
033500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC1144.2
033600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC1144.2
033700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC1144.2
033800 BAIL-OUT-EX. EXIT.                                               NC1144.2
033900 CCVS1-EXIT.                                                      NC1144.2
034000     EXIT.                                                        NC1144.2
034100/ STROKE-COMMENT-TEST-7   SHOULD PAGE EJECT AND PRINT ON TOP LINE NC1144.2
034200* NULL1-NU-L-TEST-13                                              NC1144.2
034300 0 SECTION.                                                       NC1144.2
034400 NULL1 SECTION.                                                   NC1144.2
034500 NU-L               SECTION                                      .NC1144.2
034600 A.                                                               NC1144.2
034700 B .                                                              NC1144.2
034800 C  .                                                             NC1144.2
034900 D   .                                                            NC1144.2
035000 THE-END.                                                         NC1144.2
035100     EXIT.                                                        NC1144.2
035200/ STROKE-COMMENT-TEST-7   SHOULD PAGE EJECT AND PRINT ON TOP LINE NC1144.2
035300* ASTERISK COMMENT SHOULD NOT BE SYNTAX CHECKED "(,l*.)-/+> <,....NC1144.2
035400 SECT-NC114-1 SECTION.                                            NC1144.2
035500 ALPHA-EDIT-TEST-4.                                               NC1144.2
035600     IF WRK-AE-1 EQUAL TO "ABC" NEXT SENTENCE ELSE MOVE "ABC" TO  NC1144.2
035700         CORRECT-A GO TO ALPHA-EDIT-FAIL-4.                       NC1144.2
035800     MOVE "DEF" TO WRK-AE-1.                                      NC1144.2
035900     IF WRK-AE-1 EQUAL TO "D E" PERFORM PASS                      NC1144.2
036000         GO TO ALPHA-EDIT-WRITE-4 ELSE MOVE "D E" TO CORRECT-A    NC1144.2
036100         GO TO ALPHA-EDIT-FAIL-4.                                 NC1144.2
036200 ALPHA-EDIT-DELETE-4.                                             NC1144.2
036300     PERFORM DE-LETE.                                             NC1144.2
036400     GO TO ALPHA-EDIT-WRITE-4.                                    NC1144.2
036500 ALPHA-EDIT-FAIL-4.                                               NC1144.2
036600     PERFORM FAIL.                                                NC1144.2
036700     MOVE WRK-AE-1 TO COMPUTED-A.                                 NC1144.2
036800 ALPHA-EDIT-WRITE-4.                                              NC1144.2
036900     MOVE "ALPHA-ED-TEST-4" TO PAR-NAME.                          NC1144.2
037000     MOVE "B AS EDIT CHARACTER" TO FEATURE.                       NC1144.2
037100     PERFORM PRINT-DETAIL.                                        NC1144.2
037200* ASTERISK COMMENT SHOULD NOT BE SYNTAX CHECKED "(,l*.)-/+> <,....NC1144.2
037300 ALPHA-EDIT-TEST-5.                                               NC1144.2
037400     MOVE "ABC" TO WRK-AE-2.                                      NC1144.2
037500     IF WRK-AE-2 EQUAL TO "A/BC" PERFORM PASS                     NC1144.2
037600         GO TO ALPHA-EDIT-WRITE-5 ELSE GO TO ALPHA-EDIT-FAIL-5.   NC1144.2
037700 ALPHA-EDIT-DELETE-5.                                             NC1144.2
037800     PERFORM DE-LETE.                                             NC1144.2
037900     GO TO ALPHA-EDIT-WRITE-5.                                    NC1144.2
038000 ALPHA-EDIT-FAIL-5.                                               NC1144.2
038100     PERFORM FAIL.                                                NC1144.2
038200     MOVE WRK-AE-2 TO COMPUTED-A.                                 NC1144.2
038300     MOVE "A/BC" TO CORRECT-A.                                    NC1144.2
038400 ALPHA-EDIT-WRITE-5.                                              NC1144.2
038500     MOVE "ALPHA-ED-TEST-5" TO PAR-NAME.                          NC1144.2
038600     MOVE "/ AS EDIT CHARACTER" TO FEATURE.                       NC1144.2
038700     PERFORM PRINT-DETAIL.                                        NC1144.2
038800 NUM-EDIT-TEST-6.                                                 NC1144.2
038900     MOVE 123 TO WRK-NE-1.                                        NC1144.2
039000     IF WRK-NE-1 EQUAL TO "1/23" PERFORM PASS                     NC1144.2
039100         GO TO NUM-EDIT-WRITE-6 ELSE GO TO NUM-EDIT-FAIL-6.       NC1144.2
039200 NUM-EDIT-DELETE-6.                                               NC1144.2
039300     PERFORM DE-LETE.                                             NC1144.2
039400     GO TO NUM-EDIT-WRITE-6.                                      NC1144.2
039500 NUM-EDIT-FAIL-6.                                                 NC1144.2
039600     PERFORM FAIL.                                                NC1144.2
039700     MOVE WRK-NE-1 TO COMPUTED-A.                                 NC1144.2
039800     MOVE "1/23" TO CORRECT-A.                                    NC1144.2
039900 NUM-EDIT-WRITE-6.                                                NC1144.2
040000     MOVE "NUM-EDIT-TEST-6" TO PAR-NAME.                          NC1144.2
040100     MOVE "/ AS EDIT CHARACTER" TO FEATURE.                       NC1144.2
040200     PERFORM PRINT-DETAIL.                                        NC1144.2
040300* ASTERISK COMMENT SHOULD NOT BE SYNTAX CHECKED "(,l*.)-/+> <,....NC1144.2
040400 ELEM-MOVE-TEST-16.                                               NC1144.2
040500     MOVE WRK-DS-LS-1P17-1 TO WRK-XN-18-2.                        NC1144.2
040600     IF WRK-XN-18-2 EQUAL TO "100000000000000000" PERFORM PASS    NC1144.2
040700         GO TO ELEM-MOVE-WRITE-16.                                NC1144.2
040800     GO TO ELEM-MOVE-FAIL-16.                                     NC1144.2
040900 ELEM-MOVE-DELETE-16.                                             NC1144.2
041000     PERFORM DE-LETE.                                             NC1144.2
041100     GO TO ELEM-MOVE-WRITE-16.                                    NC1144.2
041200 ELEM-MOVE-FAIL-16.                                               NC1144.2
041300     PERFORM FAIL.                                                NC1144.2
041400     MOVE WRK-XN-18-2 TO COMPUTED-A.                              NC1144.2
041500     MOVE "100000000000000000" TO CORRECT-A.                      NC1144.2
041600 ELEM-MOVE-WRITE-16.                                              NC1144.2
041700     MOVE "MOVE-TEST-16" TO PAR-NAME.                             NC1144.2
041800     MOVE "STRIP MINUS SIGN" TO FEATURE.                          NC1144.2
041900     PERFORM PRINT-DETAIL.                                        NC1144.2
042000 ELEM-MOVE-TEST-17.                                               NC1144.2
042100     MOVE WRK-DS-LS-1P17-1 TO WRK-AE-3.                           NC1144.2
042200     IF WRK-AE-3 EQUAL TO "1 000/000/000/000/000 00"              NC1144.2
042300         PERFORM PASS GO TO ELEM-MOVE-WRITE-17.                   NC1144.2
042400     GO TO ELEM-MOVE-FAIL-17.                                     NC1144.2
042500 ELEM-MOVE-DELETE-17.                                             NC1144.2
042600     PERFORM DE-LETE.                                             NC1144.2
042700     GO TO ELEM-MOVE-WRITE-17.                                    NC1144.2
042800 ELEM-MOVE-FAIL-17.                                               NC1144.2
042900     PERFORM FAIL.                                                NC1144.2
043000     MOVE WRK-AE-3 TO COMPUTED-A.                                 NC1144.2
043100     MOVE "1 000/000/000/000/000 00" TO CORRECT-A.                NC1144.2
043200 ELEM-MOVE-WRITE-17.                                              NC1144.2
043300     MOVE "MOVE-TEST-17" TO PAR-NAME.                             NC1144.2
043400     MOVE "/ AND B EDITS" TO FEATURE.                             NC1144.2
043500     PERFORM PRINT-DETAIL.                                        NC1144.2
043600 TEST-19-SYNTAX.                                                  NC1144.2
043700     PERFORM END-ROUTINE.                                         NC1144.2
043800     MOVE    ZERO TO REC-CT.                                      NC1144.2
043900     MOVE    SPACE TO TEST-RESULTS.                               NC1144.2
044000     MOVE    " PICTURE AB9  **  CHECK DATA DIV." TO TEST-RESULTS. NC1144.2
044100     PERFORM PRINT-DETAIL.                                        NC1144.2
044200*                                                                 NC1144.2
044300*    THE FOLLOWING LINES HAVE SPECIAL CHARACTERS IN THE           NC1144.2
044400*    SEQUENCE AREA (COLS 1-6) AND MANUAL VERIFICATION OF          NC1144.2
044500*    THEIR POSITION ON THE COMPILATION LISTING IS REQUESTED       NC1144.2
044600*    IN THE REPORT.                                               NC1144.2
044700*                                                                 NC1144.2
044800 SEQ-NUM-TEST-1.                                                  NC1144.2
044900     MOVE   "IV-44  7.2.1"         TO ANSI-REFERENCE.             NC1144.2
045000     MOVE    SPACES                TO TEST-RESULTS.               NC1144.2
045100     MOVE   "SEQUENCE NUMBER AREA" TO FEATURE.                    NC1144.2
045200     MOVE   "SEQ-NUM-TEST-1"       TO PAR-NAME.                   NC1144.2
045300     PERFORM PRINT-DETAIL.                                        NC1144.2
045400     MOVE   "   PLEASE VERIFY THAT THE FOLLOWING ENTRIES"         NC1144.2
045500          TO RE-MARK.                                             NC1144.2
045600     PERFORM PRINT-DETAIL.                                        NC1144.2
045700     MOVE   "   ARE PRINTED IN THE SEQUENCE NUMBER AREA"          NC1144.2
045800          TO RE-MARK.                                             NC1144.2
045900     PERFORM PRINT-DETAIL.                                        NC1144.2
046000     MOVE   "   (COLUMNS 1-6) NEAR THE END OF THE      "          NC1144.2
046100          TO RE-MARK.                                             NC1144.2
046200     PERFORM PRINT-DETAIL.                                        NC1144.2
046300     MOVE   "   COMPILATION LISTING FOR NC114M:        "          NC1144.2
046400          TO RE-MARK.                                             NC1144.2
046500     PERFORM PRINT-DETAIL.                                        NC1144.2
046600     MOVE    SPACES TO TEST-RESULTS.                              NC1144.2
046700     MOVE   "   COLUMNS:  123456" TO RE-MARK.                     NC1144.2
046800     PERFORM PRINT-DETAIL.                                        NC1144.2
046900     MOVE   "   =======:  ======" TO RE-MARK.                     NC1144.2
047000     PERFORM PRINT-DETAIL.                                        NC1144.2
047100     MOVE   "   ENTRY-1:  ABCDEF" TO RE-MARK.                     NC1144.2
047200     PERFORM PRINT-DETAIL.                                        NC1144.2
047300     MOVE   "   ENTRY-2:  */+(>'" TO RE-MARK.                     NC1144.2
047400     PERFORM PRINT-DETAIL.                                        NC1144.2
047500     MOVE   "   ENTRY-3:  999-99" TO RE-MARK.                     NC1144.2
047600     PERFORM PRINT-DETAIL.                                        NC1144.2
047700     MOVE   "   ENTRY-4:  Z=.,;<" TO RE-MARK.                     NC1144.2
047800     PERFORM PRINT-DETAIL.                                        NC1144.2
047900     MOVE   "   ENTRY-5:  )14$ X" TO RE-MARK.                     NC1144.2
048000     PERFORM PRINT-DETAIL.                                        NC1144.2
048100     MOVE   "   ENTRY-6:    23 4" TO RE-MARK.                     NC1144.2
048200     PERFORM PRINT-DETAIL.                                        NC1144.2
048300     PERFORM INSPT.                                               NC1144.2
048400*                                                                 NC1144.2
048500                                                                  NC1144.2
ABCDEF                                                                          
*/+(>'                                                                          
999-99                                                                          
Z=.,;<                                                                          
)14$ X                                                                          
  23 4                                                                          
049200                                                                  NC1144.2
049300                                                                  NC1144.2
049400                                                                  NC1144.2
049500                                                                  NC1144.2
049600*                                                                 NC1144.2
049700 CCVS-EXIT SECTION.                                               NC1144.2
049800 CCVS-999999.                                                     NC1144.2
049900     GO TO CLOSE-FILES.                                           NC1144.2
050000* ASTERISK COMMENT AS THE LAST LINE IN THE SOURCE PROGRAM LISTING NC1144.2
      *END-OF,NC114M                                                                  
