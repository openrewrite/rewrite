      *HEADER,COBOL,SQ209M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2094.2
000200 PROGRAM-ID.                                                      SQ2094.2
000300     SQ209M.                                                      SQ2094.2
000400****************************************************************  SQ2094.2
000500*                                                              *  SQ2094.2
000600*    VALIDATION FOR:-                                          *  SQ2094.2
000700*    " HIGH       ".                                              SQ2094.2
000800*                                                              *  SQ2094.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2094.2
001000*    "4.2 ".                                                      SQ2094.2
001100*                                                              *  SQ2094.2
001200*        THE ROUTINE SQ209M TESTS THE USE OF THE LEVEL 2 WRITE    SQ2094.2
001300*    STATEMENT AND THE LINAGE CLAUSE FOR A FILE DESIGNATED AS     SQ2094.2
001400*    PRINTER OUTPUT.  THESE STATEMENTS CONTROL THE VERTICAL       SQ2094.2
001500*    POSITONING OF EACH LINE ON A PRINTED PAGE.  THE LINAGE       SQ2094.2
001600*    CLAUSE SPECIFICALLY CONTROLS THE VERTICAL FORMAT OF A LOGICALSQ2094.2
001700*    PRINT PAGE.  SQ209M IS DESIGNED TO TEST THE MINIMUM          SQ2094.2
001800*    CONFIGURATION OF THE LINAGE CLAUSE.  IT IS ASSUMED THAT ALL  SQ2094.2
001900*    LEVEL 2 NUCLEUS OPTIONS ARE AVAILABLE IN TESTING SQ210.      SQ2094.2
002000*        BECAUSE OF THE NATURE OF THESE TESTS A "PASS" OR "FAIL"  SQ2094.2
002100*    CANNOT BE DETERMINED WITHIN THE PROGRAM.  THE USER MUST      SQ2094.2
002200*    VISUALLY CHECK THE POSITION OF EACH LINE TO DETERMINE THE    SQ2094.2
002300*    ACCURACY OF THE VARIOUS WRITE OPTIONS.                       SQ2094.2
002400 ENVIRONMENT DIVISION.                                            SQ2094.2
002500 CONFIGURATION SECTION.                                           SQ2094.2
002600 SOURCE-COMPUTER.                                                 SQ2094.2
002700     XXXXX082.                                                    SQ2094.2
002800 OBJECT-COMPUTER.                                                 SQ2094.2
002900     XXXXX083.                                                    SQ2094.2
003000 INPUT-OUTPUT SECTION.                                            SQ2094.2
003100 FILE-CONTROL.                                                    SQ2094.2
003200*    SELECT RAW-DATA   ASSIGN TO                                  SQ2094.2
003300*    XXXXX062                                                     SQ2094.2
003400*           ORGANIZATION IS INDEXED                               SQ2094.2
003500*           ACCESS MODE IS RANDOM                                 SQ2094.2
003600*           RECORD KEY IS RAW-DATA-KEY.                           SQ2094.2
003700     SELECT PRINT-FILE ASSIGN TO                                  SQ2094.2
003800     XXXXX055.                                                    SQ2094.2
003900 DATA DIVISION.                                                   SQ2094.2
004000 FILE SECTION.                                                    SQ2094.2
004100*                                                                 SQ2094.2
004200*FD  RAW-DATA.                                                    SQ2094.2
004300*                                                                 SQ2094.2
004400*01  RAW-DATA-SATZ.                                               SQ2094.2
004500*    05  RAW-DATA-KEY        PIC X(6).                            SQ2094.2
004600*    05  C-DATE              PIC 9(6).                            SQ2094.2
004700*    05  C-TIME              PIC 9(8).                            SQ2094.2
004800*    05  C-NO-OF-TESTS       PIC 99.                              SQ2094.2
004900*    05  C-OK                PIC 999.                             SQ2094.2
005000*    05  C-ALL               PIC 999.                             SQ2094.2
005100*    05  C-FAIL              PIC 999.                             SQ2094.2
005200*    05  C-DELETED           PIC 999.                             SQ2094.2
005300*    05  C-INSPECT           PIC 999.                             SQ2094.2
005400*    05  C-NOTE              PIC X(13).                           SQ2094.2
005500*    05  C-INDENT            PIC X.                               SQ2094.2
005600*    05  C-ABORT             PIC X(8).                            SQ2094.2
005700 FD  PRINT-FILE                                                   SQ2094.2
005800C    LABEL RECORDS                                                SQ2094.2
005900C    XXXXX084                                                     SQ2094.2
006000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2094.2
006100     LINAGE 40                                                    SQ2094.2
006200              TOP 2.                                              SQ2094.2
006300 01  PRINT-REC PICTURE X(120).                                    SQ2094.2
006400 01  DUMMY-RECORD PICTURE X(120).                                 SQ2094.2
006500 WORKING-STORAGE SECTION.                                         SQ2094.2
006600 77  ONE                PIC 9        VALUE 1.                     SQ2094.2
006700 01  NOTE-1.                                                      SQ2094.2
006800     02 FILLER          PIC X(39)    VALUE "BECAUSE OF THE NATURE SQ2094.2
006900-    "OF THESE TESTS A ".                                         SQ2094.2
007000     02 FILLER          PIC X        VALUE QUOTE.                 SQ2094.2
007100     02 FILLER          PIC X(4)     VALUE "PASS".                SQ2094.2
007200     02 FILLER          PIC X        VALUE QUOTE.                 SQ2094.2
007300     02 FILLER          PIC X(4)     VALUE " OR ".                SQ2094.2
007400     02 FILLER          PIC X        VALUE QUOTE.                 SQ2094.2
007500     02 FILLER          PIC X(4)     VALUE "FAIL".                SQ2094.2
007600     02 FILLER          PIC X        VALUE QUOTE.                 SQ2094.2
007700     02 FILLER          PIC X(65)    VALUE " CANNOT BE DETERMINED SQ2094.2
007800-    "WITHIN THE PROGRAM. THE USER MUST VISUALLY".                SQ2094.2
007900 01  NOTE-2             PIC X(120)   VALUE "CHECK THE POSITION OF SQ2094.2
008000-    "EACH LINE TO DETERMINE THE ACCURACY OF THE VARIOUS WRITE OPTSQ2094.2
008100-    "IONS. VII-27 TO 29 LINAGE ".                                SQ2094.2
008200 01  LAST-LINE-1        PIC X(120)   VALUE "THIS LINE SHOULD PRINTSQ2094.2
008300-    " AS THE LAST LINE ON THIS LOGICAL PAGE. TWO BLANK LINES SHOUSQ2094.2
008400-    "LD FOLLOW.".                                                SQ2094.2
008500 01  FIRST-LINE-1       PIC X(120)   VALUE "THIS LINE SHOULD PRINTSQ2094.2
008600-    " AS THE FIRST LINE ON A NEW LOGICAL PAGE. IT SHOULD BE THREESQ2094.2
008700-    " LINES BELOW THE PREVIOUS LINE.".                           SQ2094.2
008800 01  DETAIL-LINE-3.                                               SQ2094.2
008900     02 FILLER          PIC X(13)    VALUE "THIS IS LINE ".       SQ2094.2
009000     02 DL3-LINE-NO     PIC 99.                                   SQ2094.2
009100     02 FILLER          PIC X(40)    VALUE " OF 60 DETAIL LINES.  SQ2094.2
009200-    "LINAGE-COUNTER IS ".                                        SQ2094.2
009300     02 DL3-LC          PIC 99.                                   SQ2094.2
009400     02 FILLER          PIC X(63)    VALUE ".".                   SQ2094.2
009500 01  FOOT-LINE-3.                                                 SQ2094.2
009600     02 FILLER          PIC X(103)   VALUE "THIS LINE WAS PRINTED SQ2094.2
009700-    "FROM AN EOP CLAUSE.  THE VALUE OF THE LINAGE-COUNTER PRIOR TSQ2094.2
009800-    "O THIS EXECUTION IS ".                                      SQ2094.2
009900     02 FL3-LC          PIC 99.                                   SQ2094.2
010000     02 FILLER          PIC X(15)    VALUE ".".                   SQ2094.2
010100 01  INFO-LINE-1        PIC X(120)   VALUE "THIS TEST IS DESIGNED SQ2094.2
010200-    "TO TEST THE OVERFLOW RESULTS OF A WRITE BEFORE ADVANCING OPESQ2094.2
010300-    "RATION CONTAINING AN EOP PHRASE.".                          SQ2094.2
010400 01  INFO-LINE-2        PIC X(120)   VALUE "THIS TEST IS DESIGNED SQ2094.2
010500-    "TO TEST THE OVERFLOW RESULTS OF A WRITE AFTER ADVANCING OPERSQ2094.2
010600-    "ATION CONTAINING AN EOP PHRASE.".                           SQ2094.2
010700 01  INFO-LINE-3        PIC X(120)   VALUE "FOR THIS TEST LINAGE VSQ2094.2
010800-    "ALUE IS 40.  NO FOOTING PHRASE IS SPECIFIED.".              SQ2094.2
010900 01  INFO-LINE-4        PIC X(120)   VALUE "39 DETAIL LINES SHOULDSQ2094.2
011000-    " PRINT ON THE 1ST LOGICAL PAGE AND THE REMAINING 21 DETAIL LSQ2094.2
011100-    "INES ON THE 2ND LOGICAL PAGE.".                             SQ2094.2
011200 01  INFO-LINE-5        PIC X(120)   VALUE "40 DETAIL LINES SHOULDSQ2094.2
011300-    " PRINT ON THE 1ST LOGICAL PAGE AND THE REMAINING 20 DETAIL LSQ2094.2
011400-    "INES ON THE 2ND LOGICAL PAGE.".                             SQ2094.2
011500 01  INFO-LINE-6        PIC X(120)   VALUE "THE EOP LINE SHOULD FOSQ2094.2
011600-    "LLOW DETAIL LINE 40 AND BE THE FIRST LINE ON THE 2ND LOGICALSQ2094.2
011700-    " PAGE.".                                                    SQ2094.2
011800 01  INFO-LINE-7        PIC X(120)   VALUE "THE EOP LINE SHOULD FOSQ2094.2
011900-    "LLOW DETAIL LINE 40 AND BE THE 2ND LINE ON THE 2ND LOGICAL PSQ2094.2
012000-    "AGE.".                                                      SQ2094.2
012100 01  INFO-LINE-8        PIC X(120)   VALUE "ALL LINAGE-COUNTER VALSQ2094.2
012200-    "UES REPRESENT VALUES PRIOR TO EXECUTION OF THE WRITE OPERATISQ2094.2
012300-    "ON.  NO MODIFICATIONS HAVE BEEN MADE.".                     SQ2094.2
012400 01  TEST-RESULTS.                                                SQ2094.2
012500     02 FILLER                    PICTURE X VALUE SPACE.          SQ2094.2
012600     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2094.2
012700     02 FILLER                    PICTURE X VALUE SPACE.          SQ2094.2
012800     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2094.2
012900     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2094.2
013000     02  PAR-NAME.                                                SQ2094.2
013100       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2094.2
013200       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2094.2
013300       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2094.2
013400       03 FILLER PIC X(5) VALUE SPACE.                            SQ2094.2
013500     02 FILLER PIC X(10) VALUE SPACE.                             SQ2094.2
013600     02 RE-MARK PIC X(61).                                        SQ2094.2
013700 01  TEST-COMPUTED.                                               SQ2094.2
013800     02 FILLER PIC X(30) VALUE SPACE.                             SQ2094.2
013900     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2094.2
014000     02 COMPUTED-X.                                               SQ2094.2
014100     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2094.2
014200     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2094.2
014300     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2094.2
014400     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2094.2
014500     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2094.2
014600     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2094.2
014700         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2094.2
014800         04 FILLER                          PICTURE X.            SQ2094.2
014900     03 FILLER PIC X(50) VALUE SPACE.                             SQ2094.2
015000 01  TEST-CORRECT.                                                SQ2094.2
015100     02 FILLER PIC X(30) VALUE SPACE.                             SQ2094.2
015200     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2094.2
015300     02 CORRECT-X.                                                SQ2094.2
015400     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2094.2
015500     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2094.2
015600     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2094.2
015700     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2094.2
015800     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2094.2
015900     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2094.2
016000         04 CORRECT-18V0                    PICTURE -9(18).       SQ2094.2
016100         04 FILLER                          PICTURE X.            SQ2094.2
016200     03 FILLER PIC X(50) VALUE SPACE.                             SQ2094.2
016300 01  CCVS-C-1.                                                    SQ2094.2
016400     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2094.2
016500-    "SS  PARAGRAPH-NAME                                          SQ2094.2
016600-    "        REMARKS".                                           SQ2094.2
016700     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2094.2
016800 01  CCVS-C-2.                                                    SQ2094.2
016900     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2094.2
017000     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2094.2
017100     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2094.2
017200     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2094.2
017300     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2094.2
017400 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2094.2
017500 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2094.2
017600 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2094.2
017700 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2094.2
017800 01  INSPECT-COUNTER PIC 999 VALUE 3.                             SQ2094.2
017900 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2094.2
018000 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2094.2
018100 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2094.2
018200 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2094.2
018300 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2094.2
018400 01  CCVS-H-1.                                                    SQ2094.2
018500     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2094.2
018600     02 FILLER PICTURE X(67) VALUE                                SQ2094.2
018700     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2094.2
018800-    " SYSTEM".                                                   SQ2094.2
018900     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2094.2
019000 01  CCVS-H-2.                                                    SQ2094.2
019100     02 FILLER PICTURE X(52) VALUE IS                             SQ2094.2
019200     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2094.2
019300     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2094.2
019400     02 TEST-ID PICTURE IS X(9).                                  SQ2094.2
019500     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2094.2
019600 01  CCVS-H-3.                                                    SQ2094.2
019700     02  FILLER PICTURE X(34) VALUE                               SQ2094.2
019800     " FOR OFFICIAL USE ONLY    ".                                SQ2094.2
019900     02  FILLER PICTURE X(58) VALUE                               SQ2094.2
020000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2094.2
020100     02  FILLER PICTURE X(28) VALUE                               SQ2094.2
020200     "  COPYRIGHT   1985 ".                                       SQ2094.2
020300 01  CCVS-E-1.                                                    SQ2094.2
020400     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2094.2
020500     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2094.2
020600     02 ID-AGAIN PICTURE IS X(9).                                 SQ2094.2
020700     02 FILLER PICTURE X(45) VALUE IS                             SQ2094.2
020800     " NTIS DISTRIBUTION COBOL 85".                               SQ2094.2
020900 01  CCVS-E-2.                                                    SQ2094.2
021000     02  FILLER                   PICTURE X(31)  VALUE            SQ2094.2
021100     SPACE.                                                       SQ2094.2
021200     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2094.2
021300     02 CCVS-E-2-2.                                               SQ2094.2
021400         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2094.2
021500         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2094.2
021600         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2094.2
021700 01  CCVS-E-3.                                                    SQ2094.2
021800     02  FILLER PICTURE X(22) VALUE                               SQ2094.2
021900     " FOR OFFICIAL USE ONLY".                                    SQ2094.2
022000     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2094.2
022100     02  FILLER PICTURE X(58) VALUE                               SQ2094.2
022200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2094.2
022300     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2094.2
022400     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2094.2
022500 01  CCVS-E-4.                                                    SQ2094.2
022600     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2094.2
022700     02 FILLER PIC XXXX VALUE " OF ".                             SQ2094.2
022800     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2094.2
022900     02 FILLER PIC X(40) VALUE                                    SQ2094.2
023000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2094.2
023100 01  XXINFO.                                                      SQ2094.2
023200     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2094.2
023300     02 INFO-TEXT.                                                SQ2094.2
023400     04 FILLER PIC X(20) VALUE SPACE.                             SQ2094.2
023500     04 XXCOMPUTED PIC X(20).                                     SQ2094.2
023600     04 FILLER PIC X(5) VALUE SPACE.                              SQ2094.2
023700     04 XXCORRECT PIC X(20).                                      SQ2094.2
023800 01  HYPHEN-LINE.                                                 SQ2094.2
023900     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2094.2
024000     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2094.2
024100-    "*****************************************".                 SQ2094.2
024200     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2094.2
024300-    "******************************".                            SQ2094.2
024400 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2094.2
024500     "SQ209M".                                                    SQ2094.2
024600 PROCEDURE DIVISION.                                              SQ2094.2
024700 CCVS1 SECTION.                                                   SQ2094.2
024800 OPEN-FILES.                                                      SQ2094.2
024900*    OPEN I-O RAW-DATA.                                           SQ2094.2
025000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2094.2
025100*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2094.2
025200*    MOVE "ABORTED " TO C-ABORT.                                  SQ2094.2
025300*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2094.2
025400*    ACCEPT C-DATE  FROM DATE.                                    SQ2094.2
025500*    ACCEPT C-TIME  FROM TIME.                                    SQ2094.2
025600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2094.2
025700*END-E-1.                                                         SQ2094.2
025800*    CLOSE RAW-DATA.                                              SQ2094.2
025900     OPEN     OUTPUT PRINT-FILE.                                  SQ2094.2
026000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2094.2
026100     MOVE    SPACE TO TEST-RESULTS.                               SQ2094.2
026200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2094.2
026300     GO TO CCVS1-EXIT.                                            SQ2094.2
026400 CLOSE-FILES.                                                     SQ2094.2
026500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2094.2
026600*    OPEN I-O RAW-DATA.                                           SQ2094.2
026700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2094.2
026800*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2094.2
026900*    MOVE "OK.     " TO C-ABORT.                                  SQ2094.2
027000*    MOVE PASS-COUNTER TO C-OK.                                   SQ2094.2
027100*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2094.2
027200*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2094.2
027300*    MOVE DELETE-CNT TO C-DELETED.                                SQ2094.2
027400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2094.2
027500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2094.2
027600*END-E-2.                                                         SQ2094.2
027700*    CLOSE RAW-DATA.                                              SQ2094.2
027800 TERMINATE-CCVS.                                                  SQ2094.2
027900*    EXIT PROGRAM.                                                SQ2094.2
028000*TERMINATE-CALL.                                                  SQ2094.2
028100     STOP     RUN.                                                SQ2094.2
028200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2094.2
028300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2094.2
028400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2094.2
028500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2094.2
028600     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2094.2
028700 PRINT-DETAIL.                                                    SQ2094.2
028800     IF REC-CT NOT EQUAL TO ZERO                                  SQ2094.2
028900             MOVE "." TO PARDOT-X                                 SQ2094.2
029000             MOVE REC-CT TO DOTVALUE.                             SQ2094.2
029100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2094.2
029200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2094.2
029300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2094.2
029400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2094.2
029500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2094.2
029600     MOVE SPACE TO CORRECT-X.                                     SQ2094.2
029700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2094.2
029800     MOVE     SPACE TO RE-MARK.                                   SQ2094.2
029900 HEAD-ROUTINE.                                                    SQ2094.2
030000     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2094.2
030100     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2094.2
030200     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2094.2
030300 COLUMN-NAMES-ROUTINE.                                            SQ2094.2
030400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2094.2
030500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2094.2
030600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2094.2
030700 END-ROUTINE.                                                     SQ2094.2
030800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2094.2
030900 END-RTN-EXIT.                                                    SQ2094.2
031000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2094.2
031100 END-ROUTINE-1.                                                   SQ2094.2
031200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2094.2
031300      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2094.2
031400      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2094.2
031500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2094.2
031600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2094.2
031700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2094.2
031800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2094.2
031900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2094.2
032000  END-ROUTINE-12.                                                 SQ2094.2
032100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2094.2
032200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2094.2
032300         MOVE "NO " TO ERROR-TOTAL                                SQ2094.2
032400         ELSE                                                     SQ2094.2
032500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2094.2
032600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2094.2
032700     PERFORM WRITE-LINE.                                          SQ2094.2
032800 END-ROUTINE-13.                                                  SQ2094.2
032900     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2094.2
033000         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2094.2
033100         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2094.2
033200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2094.2
033300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2094.2
033400      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2094.2
033500          MOVE "NO " TO ERROR-TOTAL                               SQ2094.2
033600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2094.2
033700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2094.2
033800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2094.2
033900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2094.2
034000 WRITE-LINE.                                                      SQ2094.2
034100     ADD 1 TO RECORD-COUNT.                                       SQ2094.2
034200*    IF RECORD-COUNT GREATER 50                                   SQ2094.2
034300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2094.2
034400*        MOVE SPACE TO DUMMY-RECORD                               SQ2094.2
034500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2094.2
034600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2094.2
034700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2094.2
034800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2094.2
034900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2094.2
035000*        MOVE ZERO TO RECORD-COUNT.                               SQ2094.2
035100     PERFORM WRT-LN.                                              SQ2094.2
035200 WRT-LN.                                                          SQ2094.2
035300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2094.2
035400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2094.2
035500 BLANK-LINE-PRINT.                                                SQ2094.2
035600     PERFORM WRT-LN.                                              SQ2094.2
035700 FAIL-ROUTINE.                                                    SQ2094.2
035800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2094.2
035900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2094.2
036000     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2094.2
036100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2094.2
036200     GO TO FAIL-ROUTINE-EX.                                       SQ2094.2
036300 FAIL-ROUTINE-WRITE.                                              SQ2094.2
036400     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2094.2
036500     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2094.2
036600 FAIL-ROUTINE-EX. EXIT.                                           SQ2094.2
036700 BAIL-OUT.                                                        SQ2094.2
036800     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2094.2
036900     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2094.2
037000 BAIL-OUT-WRITE.                                                  SQ2094.2
037100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2094.2
037200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2094.2
037300 BAIL-OUT-EX. EXIT.                                               SQ2094.2
037400 CCVS1-EXIT.                                                      SQ2094.2
037500     EXIT.                                                        SQ2094.2
037600 SECT-SQ209M-0001 SECTION.                                        SQ2094.2
037700 WRITE-INIT-001.                                                  SQ2094.2
037800     MOVE NOTE-1 TO PRINT-REC.                                    SQ2094.2
037900     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2094.2
038000     MOVE NOTE-2 TO PRINT-REC.                                    SQ2094.2
038100     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2094.2
038200     MOVE SPACE TO DUMMY-RECORD.                                  SQ2094.2
038300     PERFORM BLANK-LINE-PRINT.                                    SQ2094.2
038400 WRITE-TEST-GF-01.                                                SQ2094.2
038500*             THIS TEST CHECKS THE VERTICAL SPACING BETWEEN       SQ2094.2
038600*             LOGICAL PAGES.  BECAUSE ONLY THE TOP PHRASE IS      SQ2094.2
038700*             SPECIFIED THERE SHOULD BE TWO SPACES BETWEEN PAGES. SQ2094.2
038800     MOVE "SPACE BTWN LOG PAGES" TO FEATURE.                      SQ2094.2
038900     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2094.2
039000     PERFORM PRINT-DETAIL.                                        SQ2094.2
039100     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 39.      SQ2094.2
039200     MOVE LAST-LINE-1 TO PRINT-REC.                               SQ2094.2
039300     WRITE PRINT-REC AFTER ONE LINE.                              SQ2094.2
039400     WRITE PRINT-REC FROM FIRST-LINE-1 AFTER ADVANCING PAGE.      SQ2094.2
039500 WRITE-TEST-GF-02.                                                SQ2094.2
039600*             THIS TEST SHOWS THE RESULTS OF A WRITE BEFORE       SQ2094.2
039700*             OPERATION WITH AN EOP PHRASE. ONLY LINAGE IS        SQ2094.2
039800*             SPECIFIED.                                          SQ2094.2
039900     MOVE "WRITE BEFORE" TO FEATURE.                              SQ2094.2
040000     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         SQ2094.2
040100     PERFORM PRINT-DETAIL.                                        SQ2094.2
040200     MOVE INFO-LINE-1 TO PRINT-REC.                               SQ2094.2
040300     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2094.2
040400     MOVE INFO-LINE-3 TO PRINT-REC.                               SQ2094.2
040500     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2094.2
040600     MOVE INFO-LINE-5 TO PRINT-REC.                               SQ2094.2
040700     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2094.2
040800     MOVE INFO-LINE-6 TO PRINT-REC.                               SQ2094.2
040900     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2094.2
041000     MOVE INFO-LINE-8 TO PRINT-REC.                               SQ2094.2
041100     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2094.2
041200     MOVE SPACES TO PRINT-REC.                                    SQ2094.2
041300     WRITE PRINT-REC BEFORE ADVANCING PAGE.                       SQ2094.2
041400     MOVE 1 TO DL3-LINE-NO.                                       SQ2094.2
041500     PERFORM WRITE-BEFORE 60 TIMES.                               SQ2094.2
041600 WRITE-TEST-GF-03.                                                SQ2094.2
041700*             THIS TEST SHOWS THE RESULTS OF A WRITE AFTER        SQ2094.2
041800*             OPERATION WITH AN EOP PHRASE. ONLY LINAGE IS        SQ2094.2
041900*             SPECIFIED.                                          SQ2094.2
042000     MOVE "WRITE AFTER" TO FEATURE.                               SQ2094.2
042100     MOVE "WRITE-TEST-GF-03" TO PAR-NAME.                         SQ2094.2
042200     PERFORM PRINT-DETAIL.                                        SQ2094.2
042300     MOVE INFO-LINE-2 TO PRINT-REC.                               SQ2094.2
042400     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2094.2
042500     MOVE INFO-LINE-3 TO PRINT-REC.                               SQ2094.2
042600     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2094.2
042700     MOVE INFO-LINE-4 TO PRINT-REC.                               SQ2094.2
042800     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2094.2
042900     MOVE INFO-LINE-7 TO PRINT-REC.                               SQ2094.2
043000     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2094.2
043100     MOVE INFO-LINE-8 TO PRINT-REC.                               SQ2094.2
043200     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2094.2
043300     MOVE SPACES TO PRINT-REC.                                    SQ2094.2
043400     WRITE PRINT-REC BEFORE ADVANCING PAGE.                       SQ2094.2
043500     MOVE 1 TO DL3-LINE-NO.                                       SQ2094.2
043600     PERFORM WRITE-AFTER 60 TIMES.                                SQ2094.2
043700 SQ209M-END-ROUTINE.                                              SQ2094.2
043800     MOVE "END OF SQ209M VALIDATION TESTS" TO PRINT-REC.          SQ2094.2
043900     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2094.2
044000     GO TO CCVS-EXIT.                                             SQ2094.2
044100 WRITE-BEFORE.                                                    SQ2094.2
044200     MOVE LINAGE-COUNTER TO DL3-LC.                               SQ2094.2
044300     MOVE DETAIL-LINE-3 TO PRINT-REC.                             SQ2094.2
044400     WRITE PRINT-REC BEFORE ADVANCING 1 LINE AT EOP               SQ2094.2
044500              MOVE LINAGE-COUNTER TO FL3-LC                       SQ2094.2
044600              MOVE FOOT-LINE-3 TO PRINT-REC                       SQ2094.2
044700              WRITE PRINT-REC BEFORE ADVANCING 1 LINE.            SQ2094.2
044800     ADD 1 TO DL3-LINE-NO.                                        SQ2094.2
044900 WRITE-AFTER.                                                     SQ2094.2
045000     MOVE LINAGE-COUNTER TO DL3-LC.                               SQ2094.2
045100     MOVE DETAIL-LINE-3 TO PRINT-REC.                             SQ2094.2
045200     WRITE PRINT-REC AFTER ADVANCING 1 LINE AT EOP                SQ2094.2
045300              MOVE LINAGE-COUNTER TO FL3-LC                       SQ2094.2
045400              MOVE FOOT-LINE-3 TO PRINT-REC                       SQ2094.2
045500              WRITE PRINT-REC AFTER ADVANCING 1 LINE.             SQ2094.2
045600     ADD 1 TO DL3-LINE-NO.                                        SQ2094.2
045700 CCVS-EXIT SECTION.                                               SQ2094.2
045800 CCVS-999999.                                                     SQ2094.2
045900     GO TO CLOSE-FILES.                                           SQ2094.2
      *END-OF,SQ209M                                                                  
