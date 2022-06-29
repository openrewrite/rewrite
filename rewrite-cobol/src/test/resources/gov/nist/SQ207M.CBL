      *HEADER,COBOL,SQ207M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2074.2
000200 PROGRAM-ID.                                                      SQ2074.2
000300     SQ207M.                                                      SQ2074.2
000400****************************************************************  SQ2074.2
000500*                                                              *  SQ2074.2
000600*    VALIDATION FOR:-                                          *  SQ2074.2
000700*    " HIGH       ".                                              SQ2074.2
000800*                                                              *  SQ2074.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2074.2
001000*    "4.2 ".                                                      SQ2074.2
001100*                                                              *  SQ2074.2
001200*        THE ROUTINE SQ207M TESTS THE USE OF THE LEVEL 2 WRITE    SQ2074.2
001300*    STATEMENT FOR A FILE DESIGNATED AS PRINTER OUTPUT.  THESE    SQ2074.2
001400*    WRITE STATEMENTS CONTROL THE VERTICAL POSITIONING OF EACH    SQ2074.2
001500*    LINE ON A PRINTED PAGE.  SQ207M TESTS ALL POSSIBLE LEVEL 2   SQ2074.2
001600*    COMBINATIONS OF THE FROM AND ADVANCING PHRASES USING MNEMONICSQ2074.2
001700*    NAME.  IT IS ASSUMED THAT ALL LEVEL 2 NUCLEUS OPTIONS ARE    SQ2074.2
001800*    AVAILABLE IN TESTING SQ207M. THE VARIABLES IN THE TESTS ARE  SQ2074.2
001900*    IDENTIFIER-1 AND MNEMONIC-NAME.  HOWEVER, BECAUSE ONLY ONE   SQ2074.2
002000*    MNEMONIC-NAME IS DEFINED IN THE SPECIAL-NAMES PARAGRAPH,     SQ2074.2
002100*    SEPARATE RUNS MUST BE MADE FOR EACH MNEMONIC-NAME TESTED.    SQ2074.2
002200*    IDENTIFIER-1 IS A 77, 01, OR SUBGROUP IDENTIFIER IN THE      SQ2074.2
002300*    WORKING-STORAGE SECTION.  THIS TEST MAY BE DELETED IF NO     SQ2074.2
002400*    MNEMONIC-NAMES EXIST FOR THE SYSTEM BEING VALIDATED.         SQ2074.2
002500*        BECAUSE OF THE NATURE OF THESE TESTS A "PASS" OR "FAIL"  SQ2074.2
002600*    CANNOT BE DETERMINED WITHIN THE PROGRAM.  THE USER MUST      SQ2074.2
002700*    VISUALLY CHECK THE POSITION OF EACH LINE TO DETERMINE THE    SQ2074.2
002800*    ACCURACY OF THE VARIOUS WRITE OPTIONS.                       SQ2074.2
002900 ENVIRONMENT DIVISION.                                            SQ2074.2
003000 CONFIGURATION SECTION.                                           SQ2074.2
003100 SOURCE-COMPUTER.                                                 SQ2074.2
003200     XXXXX082.                                                    SQ2074.2
003300 OBJECT-COMPUTER.                                                 SQ2074.2
003400     XXXXX083.                                                    SQ2074.2
003500 SPECIAL-NAMES.                                                   SQ2074.2
003600     XXXXX073                                                     SQ2074.2
003700             IS MNEMONIC-NAME.                                    SQ2074.2
003800 INPUT-OUTPUT SECTION.                                            SQ2074.2
003900 FILE-CONTROL.                                                    SQ2074.2
004000*    SELECT RAW-DATA   ASSIGN TO                                  SQ2074.2
004100*    XXXXX062                                                     SQ2074.2
004200*           ORGANIZATION IS INDEXED                               SQ2074.2
004300*           ACCESS MODE IS RANDOM                                 SQ2074.2
004400*           RECORD KEY IS RAW-DATA-KEY.                           SQ2074.2
004500     SELECT PRINT-FILE ASSIGN TO                                  SQ2074.2
004600     XXXXX055.                                                    SQ2074.2
004700 DATA DIVISION.                                                   SQ2074.2
004800 FILE SECTION.                                                    SQ2074.2
004900*                                                                 SQ2074.2
005000*FD  RAW-DATA.                                                    SQ2074.2
005100*                                                                 SQ2074.2
005200*01  RAW-DATA-SATZ.                                               SQ2074.2
005300*    05  RAW-DATA-KEY        PIC X(6).                            SQ2074.2
005400*    05  C-DATE              PIC 9(6).                            SQ2074.2
005500*    05  C-TIME              PIC 9(8).                            SQ2074.2
005600*    05  C-NO-OF-TESTS       PIC 99.                              SQ2074.2
005700*    05  C-OK                PIC 999.                             SQ2074.2
005800*    05  C-ALL               PIC 999.                             SQ2074.2
005900*    05  C-FAIL              PIC 999.                             SQ2074.2
006000*    05  C-DELETED           PIC 999.                             SQ2074.2
006100*    05  C-INSPECT           PIC 999.                             SQ2074.2
006200*    05  C-NOTE              PIC X(13).                           SQ2074.2
006300*    05  C-INDENT            PIC X.                               SQ2074.2
006400*    05  C-ABORT             PIC X(8).                            SQ2074.2
006500 FD  PRINT-FILE                                                   SQ2074.2
006600C    LABEL RECORDS                                                SQ2074.2
006700C    XXXXX084                                                     SQ2074.2
006800C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2074.2
006900               .                                                  SQ2074.2
007000 01  PRINT-REC PICTURE X(120).                                    SQ2074.2
007100 01  DUMMY-RECORD PICTURE X(120).                                 SQ2074.2
007200 WORKING-STORAGE SECTION.                                         SQ2074.2
007300 77  BEFORE-MSG-1       PIC X(120)   VALUE   "          THIS LINE SQ2074.2
007400-    "IS PRINTED BEFORE ADVANCING THE MNEMONIC-NAME SPACING.  IT SSQ2074.2
007500-    "HOULD BE 1 LINE BELOW THE WRT-TEST LINE.".                  SQ2074.2
007600 01  BEFORE-MSG-2      PIC X(120)   VALUE    "          THIS LINE SQ2074.2
007700-    "IS PRINTED BEFORE ADVANCING THE MNEMONIC-NAME SPACING.  IT SSQ2074.2
007800-    "HOULD BE 1 LINE BELOW THE WRT-TEST LINE.".                  SQ2074.2
007900 01  BEFORE-MSG-3      PIC X(120)   VALUE    "          THIS LINE SQ2074.2
008000-    "SHOULD BE WRITTEN ON THE LINE POSITIONED TO BY THE WRITE MNESQ2074.2
008100-    "MONIC-NAME OPTION BEING TESTED.".                           SQ2074.2
008200 01  LEVEL-ONE.                                                   SQ2074.2
008300     02 LEVEL-TWO.                                                SQ2074.2
008400        03 AFTER-MSG-1.                                           SQ2074.2
008500           04 FILLER    PIC X(10)    VALUE SPACES.                SQ2074.2
008600           04 FILLER    PIC X(110)   VALUE "THIS LINE IS PRINTED ASQ2074.2
008700-             "FTER ADVANCING THE MNEMONIC-NAME SPACING.  IT SHOULSQ2074.2
008800-             "D BE WRITTEN ON THE LINE POSITIONED".              SQ2074.2
008900        03 AFTER-MSG-2.                                           SQ2074.2
009000           04 FILLER    PIC X(10)    VALUE SPACE.                 SQ2074.2
009100           04 FILLER    PIC X(110)   VALUE "TO BY THE WRITE MNEMONSQ2074.2
009200-             "IC-NAME OPTION BEING TESTED.".                     SQ2074.2
009300 01  NOTE-1.                                                      SQ2074.2
009400     02 FILLER          PIC X(40)    VALUE "BECAUSE OF THE NATURE SQ2074.2
009500-    "OF THESE TESTS A ".                                         SQ2074.2
009600     02 FILLER          PIC X        VALUE QUOTE.                 SQ2074.2
009700     02 FILLER          PIC X(4)     VALUE "PASS".                SQ2074.2
009800     02 FILLER          PIC X        VALUE QUOTE.                 SQ2074.2
009900     02 FILLER          PIC X(4)     VALUE " OR ".                SQ2074.2
010000     02 FILLER          PIC X        VALUE QUOTE.                 SQ2074.2
010100     02 FILLER          PIC X(4)     VALUE "FAIL".                SQ2074.2
010200     02 FILLER          PIC X        VALUE QUOTE.                 SQ2074.2
010300     02 FILLER          PIC X(64)    VALUE " CANNOT BE DETERMINED SQ2074.2
010400-    "WITHIN THE PROGRAM. THE USER MUST VISUALLY".                SQ2074.2
010500 01  NOTE-2             PIC X(120)   VALUE "CHECK THE POSITION OF SQ2074.2
010600-    "EACH LINE TO DETERMINE THE ACCURACY OF THE VARIOUS WRITE OPTSQ2074.2
010700-    "IONS. VII-52 4.7.3 (3, 6, 7, 8, 9)".                        SQ2074.2
010800 01  TEST-RESULTS.                                                SQ2074.2
010900     02 FILLER                    PICTURE X VALUE SPACE.          SQ2074.2
011000     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2074.2
011100     02 FILLER                    PICTURE X VALUE SPACE.          SQ2074.2
011200     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2074.2
011300     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2074.2
011400     02  PAR-NAME.                                                SQ2074.2
011500       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2074.2
011600       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2074.2
011700       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2074.2
011800       03 FILLER PIC X(5) VALUE SPACE.                            SQ2074.2
011900     02 FILLER PIC X(10) VALUE SPACE.                             SQ2074.2
012000     02 RE-MARK PIC X(61).                                        SQ2074.2
012100 01  TEST-COMPUTED.                                               SQ2074.2
012200     02 FILLER PIC X(30) VALUE SPACE.                             SQ2074.2
012300     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2074.2
012400     02 COMPUTED-X.                                               SQ2074.2
012500     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2074.2
012600     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2074.2
012700     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2074.2
012800     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2074.2
012900     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2074.2
013000     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2074.2
013100         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2074.2
013200         04 FILLER                          PICTURE X.            SQ2074.2
013300     03 FILLER PIC X(50) VALUE SPACE.                             SQ2074.2
013400 01  TEST-CORRECT.                                                SQ2074.2
013500     02 FILLER PIC X(30) VALUE SPACE.                             SQ2074.2
013600     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2074.2
013700     02 CORRECT-X.                                                SQ2074.2
013800     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2074.2
013900     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2074.2
014000     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2074.2
014100     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2074.2
014200     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2074.2
014300     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2074.2
014400         04 CORRECT-18V0                    PICTURE -9(18).       SQ2074.2
014500         04 FILLER                          PICTURE X.            SQ2074.2
014600     03 FILLER PIC X(50) VALUE SPACE.                             SQ2074.2
014700 01  CCVS-C-1.                                                    SQ2074.2
014800     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2074.2
014900-    "SS  PARAGRAPH-NAME                                          SQ2074.2
015000-    "        REMARKS".                                           SQ2074.2
015100     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2074.2
015200 01  CCVS-C-2.                                                    SQ2074.2
015300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2074.2
015400     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2074.2
015500     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2074.2
015600     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2074.2
015700     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2074.2
015800 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2074.2
015900 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2074.2
016000 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2074.2
016100 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2074.2
016200 01  INSPECT-COUNTER PIC 999 VALUE 8.                             SQ2074.2
016300 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2074.2
016400 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2074.2
016500 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2074.2
016600 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2074.2
016700 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2074.2
016800 01  CCVS-H-1.                                                    SQ2074.2
016900     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2074.2
017000     02 FILLER PICTURE X(67) VALUE                                SQ2074.2
017100     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2074.2
017200-    " SYSTEM".                                                   SQ2074.2
017300     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2074.2
017400 01  CCVS-H-2.                                                    SQ2074.2
017500     02 FILLER PICTURE X(52) VALUE IS                             SQ2074.2
017600     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2074.2
017700     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2074.2
017800     02 TEST-ID PICTURE IS X(9).                                  SQ2074.2
017900     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2074.2
018000 01  CCVS-H-3.                                                    SQ2074.2
018100     02  FILLER PICTURE X(34) VALUE                               SQ2074.2
018200     " FOR OFFICIAL USE ONLY    ".                                SQ2074.2
018300     02  FILLER PICTURE X(58) VALUE                               SQ2074.2
018400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2074.2
018500     02  FILLER PICTURE X(28) VALUE                               SQ2074.2
018600     "  COPYRIGHT   1985 ".                                       SQ2074.2
018700 01  CCVS-E-1.                                                    SQ2074.2
018800     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2074.2
018900     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2074.2
019000     02 ID-AGAIN PICTURE IS X(9).                                 SQ2074.2
019100     02 FILLER PICTURE X(45) VALUE IS                             SQ2074.2
019200     " NTIS DISTRIBUTION COBOL 85".                               SQ2074.2
019300 01  CCVS-E-2.                                                    SQ2074.2
019400     02  FILLER                   PICTURE X(31)  VALUE            SQ2074.2
019500     SPACE.                                                       SQ2074.2
019600     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2074.2
019700     02 CCVS-E-2-2.                                               SQ2074.2
019800         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2074.2
019900         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2074.2
020000         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2074.2
020100 01  CCVS-E-3.                                                    SQ2074.2
020200     02  FILLER PICTURE X(22) VALUE                               SQ2074.2
020300     " FOR OFFICIAL USE ONLY".                                    SQ2074.2
020400     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2074.2
020500     02  FILLER PICTURE X(58) VALUE                               SQ2074.2
020600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2074.2
020700     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2074.2
020800     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2074.2
020900 01  CCVS-E-4.                                                    SQ2074.2
021000     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2074.2
021100     02 FILLER PIC XXXX VALUE " OF ".                             SQ2074.2
021200     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2074.2
021300     02 FILLER PIC X(40) VALUE                                    SQ2074.2
021400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2074.2
021500 01  XXINFO.                                                      SQ2074.2
021600     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2074.2
021700     02 INFO-TEXT.                                                SQ2074.2
021800     04 FILLER PIC X(20) VALUE SPACE.                             SQ2074.2
021900     04 XXCOMPUTED PIC X(20).                                     SQ2074.2
022000     04 FILLER PIC X(5) VALUE SPACE.                              SQ2074.2
022100     04 XXCORRECT PIC X(20).                                      SQ2074.2
022200 01  HYPHEN-LINE.                                                 SQ2074.2
022300     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2074.2
022400     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2074.2
022500-    "*****************************************".                 SQ2074.2
022600     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2074.2
022700-    "******************************".                            SQ2074.2
022800 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2074.2
022900     "SQ207M".                                                    SQ2074.2
023000 PROCEDURE DIVISION.                                              SQ2074.2
023100 CCVS1 SECTION.                                                   SQ2074.2
023200 OPEN-FILES.                                                      SQ2074.2
023300*    OPEN I-O RAW-DATA.                                           SQ2074.2
023400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2074.2
023500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2074.2
023600*    MOVE "ABORTED " TO C-ABORT.                                  SQ2074.2
023700*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2074.2
023800*    ACCEPT C-DATE  FROM DATE.                                    SQ2074.2
023900*    ACCEPT C-TIME  FROM TIME.                                    SQ2074.2
024000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2074.2
024100*END-E-1.                                                         SQ2074.2
024200*    CLOSE RAW-DATA.                                              SQ2074.2
024300     OPEN     OUTPUT PRINT-FILE.                                  SQ2074.2
024400     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2074.2
024500     MOVE    SPACE TO TEST-RESULTS.                               SQ2074.2
024600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2074.2
024700     GO TO CCVS1-EXIT.                                            SQ2074.2
024800 CLOSE-FILES.                                                     SQ2074.2
024900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2074.2
025000*    OPEN I-O RAW-DATA.                                           SQ2074.2
025100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2074.2
025200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2074.2
025300*    MOVE "OK.     " TO C-ABORT.                                  SQ2074.2
025400*    MOVE PASS-COUNTER TO C-OK.                                   SQ2074.2
025500*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2074.2
025600*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2074.2
025700*    MOVE DELETE-CNT TO C-DELETED.                                SQ2074.2
025800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2074.2
025900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2074.2
026000*END-E-2.                                                         SQ2074.2
026100*    CLOSE RAW-DATA.                                              SQ2074.2
026200 TERMINATE-CCVS.                                                  SQ2074.2
026300*    EXIT PROGRAM.                                                SQ2074.2
026400*TERMINATE-CALL.                                                  SQ2074.2
026500     STOP     RUN.                                                SQ2074.2
026600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2074.2
026700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2074.2
026800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2074.2
026900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2074.2
027000     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2074.2
027100 PRINT-DETAIL.                                                    SQ2074.2
027200     IF REC-CT NOT EQUAL TO ZERO                                  SQ2074.2
027300             MOVE "." TO PARDOT-X                                 SQ2074.2
027400             MOVE REC-CT TO DOTVALUE.                             SQ2074.2
027500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2074.2
027600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2074.2
027700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2074.2
027800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2074.2
027900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2074.2
028000     MOVE SPACE TO CORRECT-X.                                     SQ2074.2
028100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2074.2
028200     MOVE     SPACE TO RE-MARK.                                   SQ2074.2
028300 HEAD-ROUTINE.                                                    SQ2074.2
028400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2074.2
028500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2074.2
028600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2074.2
028700 COLUMN-NAMES-ROUTINE.                                            SQ2074.2
028800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2074.2
028900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2074.2
029000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2074.2
029100 END-ROUTINE.                                                     SQ2074.2
029200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2074.2
029300 END-RTN-EXIT.                                                    SQ2074.2
029400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2074.2
029500 END-ROUTINE-1.                                                   SQ2074.2
029600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2074.2
029700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2074.2
029800      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2074.2
029900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2074.2
030000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2074.2
030100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2074.2
030200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2074.2
030300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2074.2
030400  END-ROUTINE-12.                                                 SQ2074.2
030500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2074.2
030600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2074.2
030700         MOVE "NO " TO ERROR-TOTAL                                SQ2074.2
030800         ELSE                                                     SQ2074.2
030900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2074.2
031000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2074.2
031100     PERFORM WRITE-LINE.                                          SQ2074.2
031200 END-ROUTINE-13.                                                  SQ2074.2
031300     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2074.2
031400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2074.2
031500         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2074.2
031600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2074.2
031700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2074.2
031800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2074.2
031900          MOVE "NO " TO ERROR-TOTAL                               SQ2074.2
032000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2074.2
032100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2074.2
032200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2074.2
032300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2074.2
032400 WRITE-LINE.                                                      SQ2074.2
032500     ADD 1 TO RECORD-COUNT.                                       SQ2074.2
032600*    IF RECORD-COUNT GREATER 50                                   SQ2074.2
032700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2074.2
032800*        MOVE SPACE TO DUMMY-RECORD                               SQ2074.2
032900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2074.2
033000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2074.2
033100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2074.2
033200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2074.2
033300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2074.2
033400*        MOVE ZERO TO RECORD-COUNT.                               SQ2074.2
033500     PERFORM WRT-LN.                                              SQ2074.2
033600 WRT-LN.                                                          SQ2074.2
033700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2074.2
033800     MOVE SPACE TO DUMMY-RECORD.                                  SQ2074.2
033900 BLANK-LINE-PRINT.                                                SQ2074.2
034000     PERFORM WRT-LN.                                              SQ2074.2
034100 FAIL-ROUTINE.                                                    SQ2074.2
034200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2074.2
034300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2074.2
034400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2074.2
034500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2074.2
034600     GO TO FAIL-ROUTINE-EX.                                       SQ2074.2
034700 FAIL-ROUTINE-WRITE.                                              SQ2074.2
034800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2074.2
034900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2074.2
035000 FAIL-ROUTINE-EX. EXIT.                                           SQ2074.2
035100 BAIL-OUT.                                                        SQ2074.2
035200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2074.2
035300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2074.2
035400 BAIL-OUT-WRITE.                                                  SQ2074.2
035500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2074.2
035600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2074.2
035700 BAIL-OUT-EX. EXIT.                                               SQ2074.2
035800 CCVS1-EXIT.                                                      SQ2074.2
035900     EXIT.                                                        SQ2074.2
036000 SECT-SQ207M-0001 SECTION.                                        SQ2074.2
036100 WRITE-INIT-GF-01.                                                SQ2074.2
036200     MOVE NOTE-1 TO PRINT-REC.                                    SQ2074.2
036300     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2074.2
036400     MOVE NOTE-2 TO PRINT-REC.                                    SQ2074.2
036500     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2074.2
036600     MOVE SPACE TO DUMMY-RECORD.                                  SQ2074.2
036700     PERFORM BLANK-LINE-PRINT.                                    SQ2074.2
036800 WRITE-TEST-GF-01.                                                SQ2074.2
036900     MOVE "WRT FRM BFR ADV MNC" TO FEATURE.                       SQ2074.2
037000     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2074.2
037100     MOVE "FROM 77 LEVEL" TO RE-MARK.                             SQ2074.2
037200     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
037300     WRITE PRINT-REC FROM BEFORE-MSG-1 BEFORE ADVANCING           SQ2074.2
037400              MNEMONIC-NAME.                                      SQ2074.2
037500     WRITE PRINT-REC FROM BEFORE-MSG-3 BEFORE ADVANCING 0 LINE.   SQ2074.2
037600 WRITE-TEST-GF-02.                                                SQ2074.2
037700     MOVE "WRT FRM BFR MNC" TO FEATURE.                           SQ2074.2
037800     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         SQ2074.2
037900     MOVE "FROM 01 LEVEL" TO RE-MARK.                             SQ2074.2
038000     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
038100     WRITE PRINT-REC FROM BEFORE-MSG-2 BEFORE MNEMONIC-NAME.      SQ2074.2
038200     WRITE PRINT-REC FROM BEFORE-MSG-3 BEFORE ADVANCING 0 LINE.   SQ2074.2
038300 WRITE-TEST-GF-03.                                                SQ2074.2
038400     MOVE "WRT FRM AFT ADV MNC" TO FEATURE.                       SQ2074.2
038500     MOVE "WRITE-TEST-GF-03" TO PAR-NAME.                         SQ2074.2
038600     MOVE "FROM 03 LEVEL" TO RE-MARK.                             SQ2074.2
038700     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
038800     WRITE PRINT-REC FROM AFTER-MSG-1 AFTER ADVANCING             SQ2074.2
038900              MNEMONIC-NAME.                                      SQ2074.2
039000     WRITE PRINT-REC FROM AFTER-MSG-2 AFTER ADVANCING 1 LINE.     SQ2074.2
039100 WRITE-TEST-GF-04.                                                SQ2074.2
039200     MOVE "WRT FRM AFT MNC" TO FEATURE.                           SQ2074.2
039300     MOVE "WRITE-TEST-GF-04" TO PAR-NAME.                         SQ2074.2
039400     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
039500     WRITE PRINT-REC FROM AFTER-MSG-1 AFTER MNEMONIC-NAME.        SQ2074.2
039600     WRITE PRINT-REC FROM AFTER-MSG-2 AFTER ADVANCING 1 LINE.     SQ2074.2
039700 WRITE-TEST-GF-05.                                                SQ2074.2
039800     MOVE "WRT BFR ADV MNC" TO FEATURE.                           SQ2074.2
039900     MOVE "WRITE-TEST-GF-05" TO PAR-NAME.                         SQ2074.2
040000     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
040100     MOVE BEFORE-MSG-1 TO PRINT-REC.                              SQ2074.2
040200     WRITE PRINT-REC BEFORE ADVANCING MNEMONIC-NAME.              SQ2074.2
040300     MOVE BEFORE-MSG-3 TO PRINT-REC.                              SQ2074.2
040400     WRITE PRINT-REC BEFORE ADVANCING 0 LINE.                     SQ2074.2
040500 WRITE-TEST-GF-06.                                                SQ2074.2
040600     MOVE "WRT BFR MNC" TO FEATURE.                               SQ2074.2
040700     MOVE "WRITE-TEST-GF-06" TO PAR-NAME.                         SQ2074.2
040800     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
040900     MOVE BEFORE-MSG-2 TO PRINT-REC.                              SQ2074.2
041000     WRITE PRINT-REC BEFORE MNEMONIC-NAME.                        SQ2074.2
041100     MOVE BEFORE-MSG-3 TO PRINT-REC.                              SQ2074.2
041200     WRITE PRINT-REC BEFORE ADVANCING 0 LINE.                     SQ2074.2
041300 WRITE-TEST-GF-07.                                                SQ2074.2
041400     MOVE "WRT AFT ADV MNC" TO FEATURE.                           SQ2074.2
041500     MOVE "WRITE-TEST-GF-07" TO PAR-NAME.                         SQ2074.2
041600     MOVE "RECORD-NAME IS QUALIFIED (IN)" TO RE-MARK.             SQ2074.2
041700     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
041800     MOVE AFTER-MSG-1 TO PRINT-REC.                               SQ2074.2
041900     WRITE PRINT-REC IN PRINT-FILE AFTER ADVANCING MNEMONIC-NAME. SQ2074.2
042000     MOVE AFTER-MSG-2 TO PRINT-REC.                               SQ2074.2
042100     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2074.2
042200 WRITE-TEST-GF-08.                                                SQ2074.2
042300     MOVE "WRT AFT MNC" TO FEATURE.                               SQ2074.2
042400     MOVE "WRITE-TEST-GF-08" TO PAR-NAME.                         SQ2074.2
042500     MOVE "RECORD-NAME IS QUALIFIED (OF)" TO RE-MARK.             SQ2074.2
042600     PERFORM WRITE-TEST-LINE.                                     SQ2074.2
042700     MOVE AFTER-MSG-1 TO PRINT-REC.                               SQ2074.2
042800     WRITE PRINT-REC OF PRINT-FILE AFTER MNEMONIC-NAME.           SQ2074.2
042900     MOVE AFTER-MSG-2 TO PRINT-REC.                               SQ2074.2
043000     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2074.2
043100 SQ207M-END-ROUTINE.                                              SQ2074.2
043200     MOVE "END OF SQ207M VALIDATION TESTS" TO PRINT-REC.          SQ2074.2
043300     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2074.2
043400     GO TO CCVS-EXIT.                                             SQ2074.2
043500 WRITE-TEST-LINE.                                                 SQ2074.2
043600     PERFORM PRINT-DETAIL.                                        SQ2074.2
043700     PERFORM BLANK-LINE-PRINT.                                    SQ2074.2
043800 CCVS-EXIT SECTION.                                               SQ2074.2
043900 CCVS-999999.                                                     SQ2074.2
044000     GO TO CLOSE-FILES.                                           SQ2074.2
      *END-OF,SQ207M                                                                  
