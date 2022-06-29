      *HEADER,COBOL,NC219A                                                            
000100 IDENTIFICATION DIVISION.                                         NC2194.2
000200 PROGRAM-ID.                                                      NC2194.2
000300     NC219A.                                                      NC2194.2
000400*                                                                 NC2194.2
000500****************************************************************  NC2194.2
000600*                                                              *  NC2194.2
000700*    VALIDATION FOR:-                                          *  NC2194.2
000800*                                                              *  NC2194.2
000900*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2194.2
001000*                                                              *  NC2194.2
001100*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2194.2
001200*                                                              *  NC2194.2
001300****************************************************************  NC2194.2
001400*                                                              *  NC2194.2
001500*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC2194.2
001600*                                                              *  NC2194.2
001700*        X-55  - SYSTEM PRINTER NAME.                          *  NC2194.2
001800*        X-82  - SOURCE COMPUTER NAME.                         *  NC2194.2
001900*        X-83  - OBJECT COMPUTER NAME.                         *  NC2194.2
002000*                                                              *  NC2194.2
002100****************************************************************  NC2194.2
002200*    PROGRAM NC219A TESTS THE USE OF "HIGH-VALUE" & "LOW-VALUE"   NC2194.2
002300*    IN THE LITERAL PHRASE OF THE "ALPHABET" CLAUSE OF THE        NC2194.2
002400*    "SPECIAL-NAMES" PARAGRAPH AND THE "PROGRAM COLLATING         NC2194.2
002500*    SEQUENCE" OF THE "OBJECT-COMPUTER PARAGRAPH.                 NC2194.2
002600*                                                                 NC2194.2
002700****************************************************************  NC2194.2
002800*                                                                 NC2194.2
002900*                                                                 NC2194.2
003000*        ACCORDING TO THE RULES FOR PROGRAM COLLATING SEQUENCE    NC2194.2
003100*    THE LOWEST CHARACTER SHOULD BE THE LETTER  F  FOLLOWED BY    NC2194.2
003200*    THE LETTER  U  FOLLOWED IN ASCENDING ORDER BY THE LETTER  N  NC2194.2
003300*    WHICH IS SET ON AN EVEN PAR WITH THE COMPUTER VALUES FOR     NC2194.2
003400*    HIGH-VALUE AND LOW-VALUE.  THE NEXT HIGHEST CHARACTER IS THE NC2194.2
003500*    LETTER  Y.  THE REMAINDER OF THE CHARACTERS IN THE COBOL     NC2194.2
003600*    CHARACTER SET THEN FOLLOW IN ASCENDING ORDER BUT EXCLUDE     NC2194.2
003700*    THE CHARACTERS AND VALUES PREVIOUSLY METIONED (F,U,N,HIGH-   NC2194.2
003800*    VALUE, LOW-VALUE, AND Y).  LOW-VALUE FOR THE NEW PROGRAM     NC2194.2
003900*    COLLATING SEQUENCE JUST EVALUATED SHOULD BE THE LETTER F.    NC2194.2
004000*    HIGH-VALUE SHOULD NOW BE EVALUATED AS THE HIGHEST ORDER      NC2194.2
004100*    CHARACTER FROM THE REMAINDER OF THE NATIVE COLLATING SEQUENCENC2194.2
004200*    NOT INCLUDING THE CHARACTERS F,U,N,**PREVIOUS** HIGH-VALUE   NC2194.2
004300*    OR **PREVIOUS** LOW-VALUE, AND THE LETTER  Y.                NC2194.2
004400*                                                                 NC2194.2
004500*        THE ALPHABET-NAME COLLATING-SEQ-2 IS NOT USED IN         NC2194.2
004600*    THE PROGRAM EXCEPT TO TEST WHETHER THE LETTER  Q  HAS BEEN   NC2194.2
004700*    SET TO AN EQUAL PAR WITH THE NEW HIGH-VALUE AND NEW LOW-VALUENC2194.2
004800*    FOR PURPOSES OF THE PROGRAM COLLATING SEQUENCE.  THIS WOULD  NC2194.2
004900*    BE TRUE IF THE ALPHABET-NAME COLLATING-SEQ-2 WERE REFERENCED NC2194.2
005000*    IN A SORT, MERGE, OR CODE-SET CLAUSE.                        NC2194.2
005100*                                                                 NC2194.2
005200*                                                                 NC2194.2
005300*                                                                 NC2194.2
005400 ENVIRONMENT DIVISION.                                            NC2194.2
005500 CONFIGURATION SECTION.                                           NC2194.2
005600 SOURCE-COMPUTER.                                                 NC2194.2
005700     XXXXX082.                                                    NC2194.2
005800 OBJECT-COMPUTER.                                                 NC2194.2
005900     XXXXX083                                                     NC2194.2
006000     PROGRAM COLLATING SEQUENCE IS COLLATING-SEQ-1.               NC2194.2
006100 SPECIAL-NAMES.                                                   NC2194.2
006200         ALPHABET                                                 NC2194.2
006300         COLLATING-SEQ-1 IS "F" "U" "N"                           NC2194.2
006400             ALSO HIGH-VALUE                                      NC2194.2
006500             ALSO LOW-VALUE                                       NC2194.2
006600             "Y"                                                  NC2194.2
006700         ALPHABET                                                 NC2194.2
006800         COLLATING-SEQ-2 IS "Q"                                   NC2194.2
006900             ALSO HIGH-VALUE                                      NC2194.2
007000             ALSO LOW-VALUE.                                      NC2194.2
007100 INPUT-OUTPUT SECTION.                                            NC2194.2
007200 FILE-CONTROL.                                                    NC2194.2
007300     SELECT PRINT-FILE ASSIGN TO                                  NC2194.2
007400     XXXXX055.                                                    NC2194.2
007500 DATA DIVISION.                                                   NC2194.2
007600 FILE SECTION.                                                    NC2194.2
007700 FD  PRINT-FILE.                                                  NC2194.2
007800 01  PRINT-REC PICTURE X(120).                                    NC2194.2
007900 01  DUMMY-RECORD PICTURE X(120).                                 NC2194.2
008000 WORKING-STORAGE SECTION.                                         NC2194.2
008100 01  F-AN-1                          PICTURE A VALUE "F".         NC2194.2
008200 01  U-AN-1                          PICTURE A VALUE "U".         NC2194.2
008300 01  N-AN-1                          PICTURE A VALUE "N".         NC2194.2
008400 01  Y-AN-1                          PICTURE A VALUE "Y".         NC2194.2
008500 01  Q-AN-1                          PICTURE A VALUE "Q".         NC2194.2
008600 01  NEW-LOW                         PICTURE X VALUE LOW-VALUE.   NC2194.2
008700 01  TEST-RESULTS.                                                NC2194.2
008800     02 FILLER                   PIC X      VALUE SPACE.          NC2194.2
008900     02 FEATURE                  PIC X(20)  VALUE SPACE.          NC2194.2
009000     02 FILLER                   PIC X      VALUE SPACE.          NC2194.2
009100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          NC2194.2
009200     02 FILLER                   PIC X      VALUE SPACE.          NC2194.2
009300     02  PAR-NAME.                                                NC2194.2
009400       03 FILLER                 PIC X(19)  VALUE SPACE.          NC2194.2
009500       03  PARDOT-X              PIC X      VALUE SPACE.          NC2194.2
009600       03 DOTVALUE               PIC 99     VALUE ZERO.           NC2194.2
009700     02 FILLER                   PIC X(8)   VALUE SPACE.          NC2194.2
009800     02 RE-MARK                  PIC X(61).                       NC2194.2
009900 01  TEST-COMPUTED.                                               NC2194.2
010000     02 FILLER                   PIC X(30)  VALUE SPACE.          NC2194.2
010100     02 FILLER                   PIC X(17)  VALUE                 NC2194.2
010200            "       COMPUTED=".                                   NC2194.2
010300     02 COMPUTED-X.                                               NC2194.2
010400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          NC2194.2
010500     03 COMPUTED-N               REDEFINES COMPUTED-A             NC2194.2
010600                                 PIC -9(9).9(9).                  NC2194.2
010700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         NC2194.2
010800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     NC2194.2
010900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     NC2194.2
011000     03       CM-18V0 REDEFINES COMPUTED-A.                       NC2194.2
011100         04 COMPUTED-18V0                    PIC -9(18).          NC2194.2
011200         04 FILLER                           PIC X.               NC2194.2
011300     03 FILLER PIC X(50) VALUE SPACE.                             NC2194.2
011400 01  TEST-CORRECT.                                                NC2194.2
011500     02 FILLER PIC X(30) VALUE SPACE.                             NC2194.2
011600     02 FILLER PIC X(17) VALUE "       CORRECT =".                NC2194.2
011700     02 CORRECT-X.                                                NC2194.2
011800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         NC2194.2
011900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      NC2194.2
012000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         NC2194.2
012100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     NC2194.2
012200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     NC2194.2
012300     03      CR-18V0 REDEFINES CORRECT-A.                         NC2194.2
012400         04 CORRECT-18V0                     PIC -9(18).          NC2194.2
012500         04 FILLER                           PIC X.               NC2194.2
012600     03 FILLER PIC X(2) VALUE SPACE.                              NC2194.2
012700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     NC2194.2
012800 01  CCVS-C-1.                                                    NC2194.2
012900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PANC2194.2
013000-    "SS  PARAGRAPH-NAME                                          NC2194.2
013100-    "       REMARKS".                                            NC2194.2
013200     02 FILLER                     PIC X(20)    VALUE SPACE.      NC2194.2
013300 01  CCVS-C-2.                                                    NC2194.2
013400     02 FILLER                     PIC X        VALUE SPACE.      NC2194.2
013500     02 FILLER                     PIC X(6)     VALUE "TESTED".   NC2194.2
013600     02 FILLER                     PIC X(15)    VALUE SPACE.      NC2194.2
013700     02 FILLER                     PIC X(4)     VALUE "FAIL".     NC2194.2
013800     02 FILLER                     PIC X(94)    VALUE SPACE.      NC2194.2
013900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       NC2194.2
014000 01  REC-CT                        PIC 99       VALUE ZERO.       NC2194.2
014100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       NC2194.2
014200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       NC2194.2
014300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       NC2194.2
014400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       NC2194.2
014500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       NC2194.2
014600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       NC2194.2
014700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      NC2194.2
014800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       NC2194.2
014900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     NC2194.2
015000 01  CCVS-H-1.                                                    NC2194.2
015100     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2194.2
015200     02  FILLER                    PIC X(42)    VALUE             NC2194.2
015300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 NC2194.2
015400     02  FILLER                    PIC X(39)    VALUE SPACES.     NC2194.2
015500 01  CCVS-H-2A.                                                   NC2194.2
015600   02  FILLER                        PIC X(40)  VALUE SPACE.      NC2194.2
015700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  NC2194.2
015800   02  FILLER                        PIC XXXX   VALUE             NC2194.2
015900     "4.2 ".                                                      NC2194.2
016000   02  FILLER                        PIC X(28)  VALUE             NC2194.2
016100            " COPY - NOT FOR DISTRIBUTION".                       NC2194.2
016200   02  FILLER                        PIC X(41)  VALUE SPACE.      NC2194.2
016300                                                                  NC2194.2
016400 01  CCVS-H-2B.                                                   NC2194.2
016500   02  FILLER                        PIC X(15)  VALUE             NC2194.2
016600            "TEST RESULT OF ".                                    NC2194.2
016700   02  TEST-ID                       PIC X(9).                    NC2194.2
016800   02  FILLER                        PIC X(4)   VALUE             NC2194.2
016900            " IN ".                                               NC2194.2
017000   02  FILLER                        PIC X(12)  VALUE             NC2194.2
017100     " HIGH       ".                                              NC2194.2
017200   02  FILLER                        PIC X(22)  VALUE             NC2194.2
017300            " LEVEL VALIDATION FOR ".                             NC2194.2
017400   02  FILLER                        PIC X(58)  VALUE             NC2194.2
017500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2194.2
017600 01  CCVS-H-3.                                                    NC2194.2
017700     02  FILLER                      PIC X(34)  VALUE             NC2194.2
017800            " FOR OFFICIAL USE ONLY    ".                         NC2194.2
017900     02  FILLER                      PIC X(58)  VALUE             NC2194.2
018000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC2194.2
018100     02  FILLER                      PIC X(28)  VALUE             NC2194.2
018200            "  COPYRIGHT   1985 ".                                NC2194.2
018300 01  CCVS-E-1.                                                    NC2194.2
018400     02 FILLER                       PIC X(52)  VALUE SPACE.      NC2194.2
018500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              NC2194.2
018600     02 ID-AGAIN                     PIC X(9).                    NC2194.2
018700     02 FILLER                       PIC X(45)  VALUE SPACES.     NC2194.2
018800 01  CCVS-E-2.                                                    NC2194.2
018900     02  FILLER                      PIC X(31)  VALUE SPACE.      NC2194.2
019000     02  FILLER                      PIC X(21)  VALUE SPACE.      NC2194.2
019100     02 CCVS-E-2-2.                                               NC2194.2
019200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      NC2194.2
019300         03 FILLER                   PIC X      VALUE SPACE.      NC2194.2
019400         03 ENDER-DESC               PIC X(44)  VALUE             NC2194.2
019500            "ERRORS ENCOUNTERED".                                 NC2194.2
019600 01  CCVS-E-3.                                                    NC2194.2
019700     02  FILLER                      PIC X(22)  VALUE             NC2194.2
019800            " FOR OFFICIAL USE ONLY".                             NC2194.2
019900     02  FILLER                      PIC X(12)  VALUE SPACE.      NC2194.2
020000     02  FILLER                      PIC X(58)  VALUE             NC2194.2
020100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC2194.2
020200     02  FILLER                      PIC X(13)  VALUE SPACE.      NC2194.2
020300     02 FILLER                       PIC X(15)  VALUE             NC2194.2
020400             " COPYRIGHT 1985".                                   NC2194.2
020500 01  CCVS-E-4.                                                    NC2194.2
020600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      NC2194.2
020700     02 FILLER                       PIC X(4)   VALUE " OF ".     NC2194.2
020800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      NC2194.2
020900     02 FILLER                       PIC X(40)  VALUE             NC2194.2
021000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       NC2194.2
021100 01  XXINFO.                                                      NC2194.2
021200     02 FILLER                       PIC X(19)  VALUE             NC2194.2
021300            "*** INFORMATION ***".                                NC2194.2
021400     02 INFO-TEXT.                                                NC2194.2
021500       04 FILLER                     PIC X(8)   VALUE SPACE.      NC2194.2
021600       04 XXCOMPUTED                 PIC X(20).                   NC2194.2
021700       04 FILLER                     PIC X(5)   VALUE SPACE.      NC2194.2
021800       04 XXCORRECT                  PIC X(20).                   NC2194.2
021900     02 INF-ANSI-REFERENCE           PIC X(48).                   NC2194.2
022000 01  HYPHEN-LINE.                                                 NC2194.2
022100     02 FILLER  PIC IS X VALUE IS SPACE.                          NC2194.2
022200     02 FILLER  PIC IS X(65)    VALUE IS "************************NC2194.2
022300-    "*****************************************".                 NC2194.2
022400     02 FILLER  PIC IS X(54)    VALUE IS "************************NC2194.2
022500-    "******************************".                            NC2194.2
022600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             NC2194.2
022700     "NC219A".                                                    NC2194.2
022800 PROCEDURE DIVISION.                                              NC2194.2
022900 CCVS1 SECTION.                                                   NC2194.2
023000 OPEN-FILES.                                                      NC2194.2
023100     OPEN     OUTPUT PRINT-FILE.                                  NC2194.2
023200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   NC2194.2
023300     MOVE    SPACE TO TEST-RESULTS.                               NC2194.2
023400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC2194.2
023500     GO TO CCVS1-EXIT.                                            NC2194.2
023600 CLOSE-FILES.                                                     NC2194.2
023700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   NC2194.2
023800 TERMINATE-CCVS.                                                  NC2194.2
023900*    EXIT PROGRAM.                                                NC2194.2
024000*TERMINATE-CALL.                                                  NC2194.2
024100     STOP     RUN.                                                NC2194.2
024200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         NC2194.2
024300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           NC2194.2
024400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          NC2194.2
024500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      NC2194.2
024600     MOVE "****TEST DELETED****" TO RE-MARK.                      NC2194.2
024700 PRINT-DETAIL.                                                    NC2194.2
024800     IF REC-CT NOT EQUAL TO ZERO                                  NC2194.2
024900             MOVE "." TO PARDOT-X                                 NC2194.2
025000             MOVE REC-CT TO DOTVALUE.                             NC2194.2
025100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      NC2194.2
025200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               NC2194.2
025300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 NC2194.2
025400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 NC2194.2
025500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              NC2194.2
025600     MOVE SPACE TO CORRECT-X.                                     NC2194.2
025700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         NC2194.2
025800     MOVE     SPACE TO RE-MARK.                                   NC2194.2
025900 HEAD-ROUTINE.                                                    NC2194.2
026000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2194.2
026100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  NC2194.2
026200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2194.2
026300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  NC2194.2
026400 COLUMN-NAMES-ROUTINE.                                            NC2194.2
026500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2194.2
026600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2194.2
026700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        NC2194.2
026800 END-ROUTINE.                                                     NC2194.2
026900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.NC2194.2
027000 END-RTN-EXIT.                                                    NC2194.2
027100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2194.2
027200 END-ROUTINE-1.                                                   NC2194.2
027300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      NC2194.2
027400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               NC2194.2
027500      ADD PASS-COUNTER TO ERROR-HOLD.                             NC2194.2
027600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   NC2194.2
027700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            NC2194.2
027800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              NC2194.2
027900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                NC2194.2
028000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           NC2194.2
028100  END-ROUTINE-12.                                                 NC2194.2
028200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        NC2194.2
028300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      NC2194.2
028400         MOVE "NO " TO ERROR-TOTAL                                NC2194.2
028500         ELSE                                                     NC2194.2
028600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       NC2194.2
028700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           NC2194.2
028800     PERFORM WRITE-LINE.                                          NC2194.2
028900 END-ROUTINE-13.                                                  NC2194.2
029000     IF DELETE-COUNTER IS EQUAL TO ZERO                           NC2194.2
029100         MOVE "NO " TO ERROR-TOTAL  ELSE                          NC2194.2
029200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      NC2194.2
029300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   NC2194.2
029400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2194.2
029500      IF   INSPECT-COUNTER EQUAL TO ZERO                          NC2194.2
029600          MOVE "NO " TO ERROR-TOTAL                               NC2194.2
029700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   NC2194.2
029800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            NC2194.2
029900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          NC2194.2
030000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           NC2194.2
030100 WRITE-LINE.                                                      NC2194.2
030200     ADD 1 TO RECORD-COUNT.                                       NC2194.2
030300*    IF RECORD-COUNT GREATER 50                                   NC2194.2
030400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          NC2194.2
030500*        MOVE SPACE TO DUMMY-RECORD                               NC2194.2
030600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  NC2194.2
030700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             NC2194.2
030800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     NC2194.2
030900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          NC2194.2
031000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          NC2194.2
031100*        MOVE ZERO TO RECORD-COUNT.                               NC2194.2
031200     PERFORM WRT-LN.                                              NC2194.2
031300 WRT-LN.                                                          NC2194.2
031400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               NC2194.2
031500     MOVE SPACE TO DUMMY-RECORD.                                  NC2194.2
031600 BLANK-LINE-PRINT.                                                NC2194.2
031700     PERFORM WRT-LN.                                              NC2194.2
031800 FAIL-ROUTINE.                                                    NC2194.2
031900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. NC2194.2
032000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.NC2194.2
032100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2194.2
032200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   NC2194.2
032300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2194.2
032400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2194.2
032500     GO TO  FAIL-ROUTINE-EX.                                      NC2194.2
032600 FAIL-ROUTINE-WRITE.                                              NC2194.2
032700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         NC2194.2
032800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 NC2194.2
032900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. NC2194.2
033000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         NC2194.2
033100 FAIL-ROUTINE-EX. EXIT.                                           NC2194.2
033200 BAIL-OUT.                                                        NC2194.2
033300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   NC2194.2
033400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           NC2194.2
033500 BAIL-OUT-WRITE.                                                  NC2194.2
033600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  NC2194.2
033700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 NC2194.2
033800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   NC2194.2
033900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         NC2194.2
034000 BAIL-OUT-EX. EXIT.                                               NC2194.2
034100 CCVS1-EXIT.                                                      NC2194.2
034200     EXIT.                                                        NC2194.2
034300 SECT-NC219A-001 SECTION.                                         NC2194.2
034400*                                                                 NC2194.2
034500*                                                                 NC2194.2
034600*        THE LETTER  F  IS THE LOWEST CHARACTER IN THE PROGRAM    NC2194.2
034700*    COLLATING SEQUENCE FOLLOWED IN ASCENDING ORDER BY THE        NC2194.2
034800*    LETTER  U.  THIS IS SHOWN IN THE ALPHABET-NAME               NC2194.2
034900*    COLLATING-SEQ-1.                                             NC2194.2
035000*                                                                 NC2194.2
035100*            F  SHOULD BE LESS THAN  U.                           NC2194.2
035200*                                                                 NC2194.2
035300*                                                                 NC2194.2
035400 SEQ-INIT-GF-1.                                                   NC2194.2
035500     MOVE    "SEQ-TEST-GF-1" TO PAR-NAME.                         NC2194.2
035600     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
035700     MOVE    "F < U" TO FEATURE.                                  NC2194.2
035800 SEQ-TEST-GF-1.                                                   NC2194.2
035900     IF      F-AN-1 IS LESS THAN U-AN-1                           NC2194.2
036000             PERFORM PASS                                         NC2194.2
036100         ELSE                                                     NC2194.2
036200             GO TO SEQ-FAIL-GF-1.                                 NC2194.2
036300     GO      TO SEQ-WRITE-GF-1.                                   NC2194.2
036400 SEQ-DELETE-GF-1.                                                 NC2194.2
036500     PERFORM DE-LETE.                                             NC2194.2
036600     GO      TO SEQ-WRITE-GF-1.                                   NC2194.2
036700 SEQ-FAIL-GF-1.                                                   NC2194.2
036800     PERFORM FAIL                                                 NC2194.2
036900     MOVE "F NOT < THAN U" TO COMPUTED-A.                         NC2194.2
037000 SEQ-WRITE-GF-1.                                                  NC2194.2
037100     PERFORM PRINT-DETAIL.                                        NC2194.2
037200*                                                                 NC2194.2
037300*                                                                 NC2194.2
037400*        THE LETTER  U  IS THE SECOND LOWEST CHARACTER IN THE     NC2194.2
037500*    PROGRAM COLLATING SEQUENCE FOLLOWED IN ASCENDING ORDER BY THENC2194.2
037600*    LETTER  N.  THIS IS SHOWN IN THE ALPHABET-NAME               NC2194.2
037700*    COLLATING-SEQ-1.                                             NC2194.2
037800*                                                                 NC2194.2
037900*            U  SHOULD BE LESS THAN  N.                           NC2194.2
038000*                                                                 NC2194.2
038100*                                                                 NC2194.2
038200 SEQ-INIT-GF-2.                                                   NC2194.2
038300     MOVE    "SEQ-TEST-GF-2" TO PAR-NAME.                         NC2194.2
038400     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
038500     MOVE    "U < N" TO FEATURE.                                  NC2194.2
038600 SEQ-TEST-GF-2.                                                   NC2194.2
038700*                                                                 NC2194.2
038800     IF      U-AN-1 IS LESS THAN N-AN-1                           NC2194.2
038900             PERFORM PASS                                         NC2194.2
039000         ELSE                                                     NC2194.2
039100             GO TO SEQ-FAIL-GF-2.                                 NC2194.2
039200     GO      TO SEQ-WRITE-GF-2.                                   NC2194.2
039300 SEQ-DELETE-GF-2.                                                 NC2194.2
039400     PERFORM DE-LETE.                                             NC2194.2
039500     GO      TO SEQ-WRITE-GF-2.                                   NC2194.2
039600 SEQ-FAIL-GF-2.                                                   NC2194.2
039700     PERFORM FAIL                                                 NC2194.2
039800     MOVE "U NOT < THAN N" TO COMPUTED-A.                         NC2194.2
039900 SEQ-WRITE-GF-2.                                                  NC2194.2
040000     PERFORM PRINT-DETAIL.                                        NC2194.2
040100*                                                                 NC2194.2
040200*                                                                 NC2194.2
040300*        THE LETTER  N  IS SET TO AN EVEN PAR WITH THE **OLD**    NC2194.2
040400*    HIGH-VALUE BUT NOT EQUAL TO THE **NEW** HIGH-VALUE.          NC2194.2
040500*                                                                 NC2194.2
040600*            N SHOULD NOT = HIGH-VALUE.                           NC2194.2
040700*                                                                 NC2194.2
040800*                                                                 NC2194.2
040900 SEQ-INIT-GF-3.                                                   NC2194.2
041000     MOVE    "SEQ-TEST-GF-3" TO PAR-NAME.                         NC2194.2
041100     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
041200     MOVE    "N = HIGH-VALUE" TO FEATURE.                         NC2194.2
041300 SEQ-TEST-GF-3.                                                   NC2194.2
041400*                                                                 NC2194.2
041500     IF      N-AN-1 IS EQUAL TO HIGH-VALUE                        NC2194.2
041600             GO TO SEQ-FAIL-GF-3                                  NC2194.2
041700         ELSE                                                     NC2194.2
041800             PERFORM PASS.                                        NC2194.2
041900     GO      TO SEQ-WRITE-GF-3.                                   NC2194.2
042000 SEQ-DELETE-GF-3.                                                 NC2194.2
042100     PERFORM DE-LETE.                                             NC2194.2
042200     GO      TO SEQ-WRITE-GF-3.                                   NC2194.2
042300 SEQ-FAIL-GF-3.                                                   NC2194.2
042400     PERFORM FAIL                                                 NC2194.2
042500     MOVE "N = HIGH-VALUE" TO COMPUTED-A.                         NC2194.2
042600 SEQ-WRITE-GF-3.                                                  NC2194.2
042700     PERFORM PRINT-DETAIL.                                        NC2194.2
042800*                                                                 NC2194.2
042900*                                                                 NC2194.2
043000*        LOW-VALUE SHOULD BE SET TO THE LETTER  F  SINCE IT       NC2194.2
043100*    IS THE LOWEST CHARACTER IN THE PROGRAM COLLATING SEQUENCE.   NC2194.2
043200*                                                                 NC2194.2
043300*            F SHOULD BE EQUAL TO LOW-VALUE.                      NC2194.2
043400*                                                                 NC2194.2
043500*                                                                 NC2194.2
043600 SEQ-INIT-GF-4.                                                   NC2194.2
043700     MOVE    "SEQ-TEST-GF-4" TO PAR-NAME.                         NC2194.2
043800     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
043900     MOVE    "F = LOW-VALUE" TO FEATURE.                          NC2194.2
044000 SEQ-TEST-GF-4.                                                   NC2194.2
044100*                                                                 NC2194.2
044200     IF      F-AN-1 IS EQUAL TO LOW-VALUE                         NC2194.2
044300             PERFORM PASS                                         NC2194.2
044400         ELSE                                                     NC2194.2
044500             GO TO SEQ-FAIL-GF-4.                                 NC2194.2
044600     GO      TO SEQ-WRITE-GF-4.                                   NC2194.2
044700 SEQ-DELETE-GF-4.                                                 NC2194.2
044800     PERFORM DE-LETE.                                             NC2194.2
044900     GO      TO SEQ-WRITE-GF-4.                                   NC2194.2
045000 SEQ-FAIL-GF-4.                                                   NC2194.2
045100     PERFORM FAIL                                                 NC2194.2
045200     MOVE "F NOT = LOW-VALUE" TO COMPUTED-A.                      NC2194.2
045300 SEQ-WRITE-GF-4.                                                  NC2194.2
045400     PERFORM PRINT-DETAIL.                                        NC2194.2
045500*                                                                 NC2194.2
045600*                                                                 NC2194.2
045700*        THE **NEW** LOW-VALUE SHOULD BE SET TO THE LETTER  F.    NC2194.2
045800*    THE **NEW** HIGH-VALUE SHOULD BE SET TO THE HIGHEST ORDER    NC2194.2
045900*    CHARACTER IN THE EVALUATED PROGRAM COLLATING SEQUENCE.       NC2194.2
046000*                                                                 NC2194.2
046100*            HIGH-VALUE SHOULD BE GREATER THAN LOW-VALUE.         NC2194.2
046200*                                                                 NC2194.2
046300*                                                                 NC2194.2
046400 SEQ-INIT-GF-5.                                                   NC2194.2
046500     MOVE    "SEQ-TEST-GF-5" TO PAR-NAME.                         NC2194.2
046600     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
046700     MOVE    "H-VALUE > L-VALUE" TO FEATURE.                      NC2194.2
046800 SEQ-TEST-GF-5.                                                   NC2194.2
046900*                                                                 NC2194.2
047000     IF      HIGH-VALUE IS GREATER THAN NEW-LOW                   NC2194.2
047100             PERFORM PASS                                         NC2194.2
047200         ELSE                                                     NC2194.2
047300             GO TO SEQ-FAIL-GF-5.                                 NC2194.2
047400     GO      TO SEQ-WRITE-GF-5.                                   NC2194.2
047500 SEQ-DELETE-GF-5.                                                 NC2194.2
047600     PERFORM DE-LETE.                                             NC2194.2
047700     GO      TO SEQ-WRITE-GF-5.                                   NC2194.2
047800 SEQ-FAIL-GF-5.                                                   NC2194.2
047900     PERFORM FAIL                                                 NC2194.2
048000     MOVE "H-VALU NOT > L-VALU" TO COMPUTED-A.                    NC2194.2
048100 SEQ-WRITE-GF-5.                                                  NC2194.2
048200     PERFORM PRINT-DETAIL.                                        NC2194.2
048300*                                                                 NC2194.2
048400*                                                                 NC2194.2
048500*            LOW-VALUE SHOULD BE LESS THAN HIGH-VALUE.            NC2194.2
048600*                                                                 NC2194.2
048700*                                                                 NC2194.2
048800 SEQ-INIT-GF-6.                                                   NC2194.2
048900     MOVE    "SEQ-TEST-GF-6" TO PAR-NAME.                         NC2194.2
049000     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
049100     MOVE    "L-VALUE < H-VALUE" TO FEATURE.                      NC2194.2
049200 SEQ-TEST-GF-6.                                                   NC2194.2
049300*                                                                 NC2194.2
049400     IF      NEW-LOW IS LESS THAN HIGH-VALUE                      NC2194.2
049500             PERFORM PASS                                         NC2194.2
049600         ELSE                                                     NC2194.2
049700             GO TO SEQ-FAIL-GF-6.                                 NC2194.2
049800     GO      TO SEQ-WRITE-GF-6.                                   NC2194.2
049900 SEQ-DELETE-GF-6.                                                 NC2194.2
050000     PERFORM DE-LETE.                                             NC2194.2
050100     GO      TO SEQ-WRITE-GF-6.                                   NC2194.2
050200 SEQ-FAIL-GF-6.                                                   NC2194.2
050300     PERFORM FAIL                                                 NC2194.2
050400     MOVE "L-VALU NOT < H-VALU" TO COMPUTED-A.                    NC2194.2
050500 SEQ-WRITE-GF-6.                                                  NC2194.2
050600     PERFORM PRINT-DETAIL.                                        NC2194.2
050700*                                                                 NC2194.2
050800*                                                                 NC2194.2
050900*            LOW-VALUE SHOULD NOT BE EQUAL TO HIGH-VALUE.         NC2194.2
051000*                                                                 NC2194.2
051100*                                                                 NC2194.2
051200 SEQ-INIT-GF-7.                                                   NC2194.2
051300     MOVE    "SEQ-TEST-GF-7" TO PAR-NAME.                         NC2194.2
051400     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
051500     MOVE    "H-VALUE = L-VALUE" TO FEATURE.                      NC2194.2
051600 SEQ-TEST-GF-7.                                                   NC2194.2
051700*                                                                 NC2194.2
051800     IF      HIGH-VALUE IS EQUAL TO NEW-LOW                       NC2194.2
051900             GO TO SEQ-FAIL-GF-7                                  NC2194.2
052000         ELSE                                                     NC2194.2
052100             PERFORM PASS.                                        NC2194.2
052200     GO      TO SEQ-WRITE-GF-7.                                   NC2194.2
052300 SEQ-DELETE-GF-7.                                                 NC2194.2
052400     PERFORM DE-LETE.                                             NC2194.2
052500     GO      TO SEQ-WRITE-GF-7.                                   NC2194.2
052600 SEQ-FAIL-GF-7.                                                   NC2194.2
052700     PERFORM FAIL                                                 NC2194.2
052800     MOVE "H-VALUE = L-VALUE" TO COMPUTED-A.                      NC2194.2
052900 SEQ-WRITE-GF-7.                                                  NC2194.2
053000     PERFORM PRINT-DETAIL.                                        NC2194.2
053100*                                                                 NC2194.2
053200*                                                                 NC2194.2
053300*        LOW-VALUE SHOULD BE SET TO THE LETTER  F.                NC2194.2
053400*                                                                 NC2194.2
053500*            THE LETTER  Y  SHOULD NOT BE EQUAL TO LOW-VALUE.     NC2194.2
053600*                                                                 NC2194.2
053700*                                                                 NC2194.2
053800 SEQ-INIT-GF-8.                                                   NC2194.2
053900     MOVE    "SEQ-TEST-GF-8" TO PAR-NAME.                         NC2194.2
054000     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
054100     MOVE    "Y = LOW-VALUE" TO FEATURE.                          NC2194.2
054200 SEQ-TEST-GF-8.                                                   NC2194.2
054300*                                                                 NC2194.2
054400     IF      Y-AN-1 IS EQUAL TO LOW-VALUE                         NC2194.2
054500             GO TO SEQ-FAIL-GF-8                                  NC2194.2
054600         ELSE                                                     NC2194.2
054700             PERFORM PASS.                                        NC2194.2
054800     GO      TO SEQ-WRITE-GF-8.                                   NC2194.2
054900 SEQ-DELETE-GF-8.                                                 NC2194.2
055000     PERFORM DE-LETE.                                             NC2194.2
055100     GO      TO SEQ-WRITE-GF-8.                                   NC2194.2
055200 SEQ-FAIL-GF-8.                                                   NC2194.2
055300     PERFORM FAIL                                                 NC2194.2
055400     MOVE "Y = LOW-VALUE" TO COMPUTED-A.                          NC2194.2
055500 SEQ-WRITE-GF-8.                                                  NC2194.2
055600     PERFORM PRINT-DETAIL.                                        NC2194.2
055700*                                                                 NC2194.2
055800*                                                                 NC2194.2
055900*        THE LETTER  Q  IS MENTIONED IN THE ALPHABET-NAME         NC2194.2
056000*    COLLATING-SEQ-2.  THIS ALPHABET-NAME CLAUSE SHOULD HAVE NO   NC2194.2
056100*    EFFECT ON THE PROGRAM COLLATING SEQUENCE.                    NC2194.2
056200*                                                                 NC2194.2
056300*            FOR OUR PROGRAM COLLATING SEQUENCE  Q  SHOULD NOT    NC2194.2
056400*            BE THE LOW ORDER CHARACTER THEREFORE IT SHOULD NOT   NC2194.2
056500*            BE EQUAL TO THE **NEW**  LOW-VALUE.                  NC2194.2
056600*                                                                 NC2194.2
056700*                                                                 NC2194.2
056800 SEQ-INIT-GF-9.                                                   NC2194.2
056900     MOVE    "SEQ-TEST-GF-9" TO PAR-NAME.                         NC2194.2
057000     MOVE    "VI-16 4.5.4 GR5/7" TO ANSI-REFERENCE.               NC2194.2
057100     MOVE    "Q = LOW-VALUE" TO FEATURE.                          NC2194.2
057200 SEQ-TEST-GF-9.                                                   NC2194.2
057300*                                                                 NC2194.2
057400     IF      Q-AN-1 IS EQUAL TO LOW-VALUE                         NC2194.2
057500             GO TO SEQ-FAIL-GF-9                                  NC2194.2
057600         ELSE                                                     NC2194.2
057700             PERFORM PASS.                                        NC2194.2
057800     GO      TO SEQ-WRITE-GF-9.                                   NC2194.2
057900 SEQ-DELETE-GF-9.                                                 NC2194.2
058000     PERFORM DE-LETE.                                             NC2194.2
058100     GO      TO SEQ-WRITE-GF-9.                                   NC2194.2
058200 SEQ-FAIL-GF-9.                                                   NC2194.2
058300     PERFORM FAIL                                                 NC2194.2
058400     MOVE "Q = LOW-VALUE" TO COMPUTED-A.                          NC2194.2
058500 SEQ-WRITE-GF-9.                                                  NC2194.2
058600     PERFORM PRINT-DETAIL.                                        NC2194.2
058700*                                                                 NC2194.2
058800*                                                                 NC2194.2
058900 CCVS-EXIT SECTION.                                               NC2194.2
059000 CCVS-999999.                                                     NC2194.2
059100     GO TO CLOSE-FILES.                                           NC2194.2
      *END-OF,NC219A                                                                  
