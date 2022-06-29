      *HEADER,COBOL,SQ208M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ2084.2
000200 PROGRAM-ID.                                                      SQ2084.2
000300     SQ208M.                                                      SQ2084.2
000400****************************************************************  SQ2084.2
000500*                                                              *  SQ2084.2
000600*    VALIDATION FOR:-                                          *  SQ2084.2
000700*    " HIGH       ".                                              SQ2084.2
000800*                                                              *  SQ2084.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2084.2
001000*    "4.2 ".                                                      SQ2084.2
001100*                                                              *  SQ2084.2
001200*        THE ROUTINE SQ208M TESTS THE USE OF THE LEVEL 2 WRITE    SQ2084.2
001300*    STATEMENT AND THE LINAGE CLAUSE FOR A FILE DESIGNATED AS     SQ2084.2
001400*    PRINTER OUTPUT.  THESE STATEMENTS CONTROL THE VERTICAL       SQ2084.2
001500*    POSITIONING OF EACH LINE ON A PRINTED PAGE.  THE LINAGE      SQ2084.2
001600*    CLAUSE SPECIFICALLY CONTROLS THE VERTICAL FORMAT OF A LOGICALSQ2084.2
001700*    PRINT PAGE. SQ208M TESTS THE USE OF DATA-NAMES IN THE LINAGE,SQ2084.2
001800*    FOOTING, TOP, AND BOTTOM PHRASES.  VALUES OF DATA-NAMES ARE  SQ2084.2
001900*    CHANGED IN ORDER TO CHECK REDEFINITION OF LOGICAL PAGE       SQ2084.2
002000*    FORMATS AFTER OVERFLOW OR WRITE ADVANCING PAGE OPERATIONS.   SQ2084.2
002100*    IT IS ASSUMED THAT ALL LEVEL 2 NUCLEUS OPTIONS ARE AVAILABLE SQ2084.2
002200*    IN TESTING SQ208M.                                           SQ2084.2
002300*        BECAUSE OF THE NATURE OF THESE TESTS A "PASS" OR "FAIL"  SQ2084.2
002400*    CANNOT BE DETERMINED WITHIN THE PROGRAM.  THE USER MUST      SQ2084.2
002500*    VISUALLY CHECK THE POSITION OF EACH LINE TO DETERMINE THE    SQ2084.2
002600*    ACCURACY OF THE VARIOUS WRITE AND LINAGE OPTIONS.            SQ2084.2
002700 ENVIRONMENT DIVISION.                                            SQ2084.2
002800 CONFIGURATION SECTION.                                           SQ2084.2
002900 SOURCE-COMPUTER.                                                 SQ2084.2
003000     XXXXX082.                                                    SQ2084.2
003100 OBJECT-COMPUTER.                                                 SQ2084.2
003200     XXXXX083.                                                    SQ2084.2
003300 INPUT-OUTPUT SECTION.                                            SQ2084.2
003400 FILE-CONTROL.                                                    SQ2084.2
003500*    SELECT RAW-DATA   ASSIGN TO                                  SQ2084.2
003600*    XXXXX062                                                     SQ2084.2
003700*           ORGANIZATION IS INDEXED                               SQ2084.2
003800*           ACCESS MODE IS RANDOM                                 SQ2084.2
003900*           RECORD KEY IS RAW-DATA-KEY.                           SQ2084.2
004000     SELECT PRINT-FILE ASSIGN TO                                  SQ2084.2
004100     XXXXX055.                                                    SQ2084.2
004200 DATA DIVISION.                                                   SQ2084.2
004300 FILE SECTION.                                                    SQ2084.2
004400*                                                                 SQ2084.2
004500*FD  RAW-DATA.                                                    SQ2084.2
004600*                                                                 SQ2084.2
004700*01  RAW-DATA-SATZ.                                               SQ2084.2
004800*    05  RAW-DATA-KEY        PIC X(6).                            SQ2084.2
004900*    05  C-DATE              PIC 9(6).                            SQ2084.2
005000*    05  C-TIME              PIC 9(8).                            SQ2084.2
005100*    05  C-NO-OF-TESTS       PIC 99.                              SQ2084.2
005200*    05  C-OK                PIC 999.                             SQ2084.2
005300*    05  C-ALL               PIC 999.                             SQ2084.2
005400*    05  C-FAIL              PIC 999.                             SQ2084.2
005500*    05  C-DELETED           PIC 999.                             SQ2084.2
005600*    05  C-INSPECT           PIC 999.                             SQ2084.2
005700*    05  C-NOTE              PIC X(13).                           SQ2084.2
005800*    05  C-INDENT            PIC X.                               SQ2084.2
005900*    05  C-ABORT             PIC X(8).                            SQ2084.2
006000 FD  PRINT-FILE                                                   SQ2084.2
006100C    LABEL RECORDS                                                SQ2084.2
006200C    XXXXX084                                                     SQ2084.2
006300C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2084.2
006400     LINAGE LINAGE-CTR                                            SQ2084.2
006500              FOOTING FOOT-CTR                                    SQ2084.2
006600              TOP TOP-CTR                                         SQ2084.2
006700              BOTTOM BOTTOM-CTR.                                  SQ2084.2
006800 01  PRINT-REC PICTURE X(120).                                    SQ2084.2
006900 01  DUMMY-RECORD PICTURE X(120).                                 SQ2084.2
007000 WORKING-STORAGE SECTION.                                         SQ2084.2
007100 77  LINAGE-CTR         PIC 999      VALUE 66.                    SQ2084.2
007200 01  FOOT-CTR           PIC 999      VALUE 66.                    SQ2084.2
007300 01  SPACING-CTR.                                                 SQ2084.2
007400     02 TOP-CTR         PIC 999      VALUE 0.                     SQ2084.2
007500     02 BOTTOM-CTR      PIC 999      VALUE 0.                     SQ2084.2
007600 01  DETAIL-LINE-1.                                               SQ2084.2
007700     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ2084.2
007800     02 FILLER          PIC X(13)    VALUE "THIS IS LINE ".       SQ2084.2
007900     02 DL1-LINE-NO     PIC 999.                                  SQ2084.2
008000     02 FILLER          PIC X(4)     VALUE " OF ".                SQ2084.2
008100     02 DL1-TOTAL-LINES PIC 999.                                  SQ2084.2
008200     02 FILLER          PIC X(34)    VALUE " DETAIL LINES.  LINAGESQ2084.2
008300-                                          "-COUNTER IS ".        SQ2084.2
008400     02 DL1-LC          PIC 999.                                  SQ2084.2
008500     02 FILLER          PIC X(40)    VALUE ".".                   SQ2084.2
008600 01  DETAIL-LINE-2.                                               SQ2084.2
008700     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ2084.2
008800     02 FILLER          PIC X(13)    VALUE "THIS IS LINE ".       SQ2084.2
008900     02 DL2-LINE-NO     PIC 999.                                  SQ2084.2
009000     02 FILLER          PIC X(41)    VALUE " OF 010 DETAIL LINES. SQ2084.2
009100-    " LINAGE-COUNTER IS ".                                       SQ2084.2
009200     02 DL2-LC          PIC 999.                                  SQ2084.2
009300     02 FILLER          PIC X(40)    VALUE ".".                   SQ2084.2
009400 01  DETAIL-LINE-3.                                               SQ2084.2
009500     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ2084.2
009600     02 FILLER          PIC X(13)    VALUE "THIS IS LINE ".       SQ2084.2
009700     02 DL3-LINE-NO     PIC 99.                                   SQ2084.2
009800     02 FILLER          PIC X(40)    VALUE " OF 60 DETAIL LINES.  SQ2084.2
009900-    "LINAGE-COUNTER IS ".                                        SQ2084.2
010000     02 DL3-LC          PIC 999.                                  SQ2084.2
010100     02 FILLER          PIC X(42)    VALUE ".".                   SQ2084.2
010200 01  FOOT-LINE-1.                                                 SQ2084.2
010300     02 FILLER          PIC X(20)    VALUE SPACE.                 SQ2084.2
010400     02 FILLER          PIC X(57)    VALUE "THIS IS LINE 001 OF 00SQ2084.2
010500-    "1 FOOTING LINES. LINAGE-COUNTER IS ".                       SQ2084.2
010600     02 FL1-LC          PIC 999.                                  SQ2084.2
010700     02 FILLER          PIC X(40)    VALUE ".".                   SQ2084.2
010800 01  FOOT-LINE-3.                                                 SQ2084.2
010900     02 FILLER          PIC X(103)   VALUE "THIS LINE WAS PRINTED SQ2084.2
011000-    "FROM AN EOP CLAUSE.  THE VALUE OF THE LINAGE-COUNTER PRIOR TSQ2084.2
011100-    "O THIS EXECUTION IS ".                                      SQ2084.2
011200     02 FL3-LC          PIC 999.                                  SQ2084.2
011300     02 FILLER          PIC X(14)    VALUE ".".                   SQ2084.2
011400 01  INFO-LINE-1        PIC X(120)   VALUE "THIS TEST IS DESIGNED SQ2084.2
011500-    "TO TEST THE OVERFLOW RESULTS OF A WRITE BEFORE ADVANCING OPESQ2084.2
011600-    "RATION CONTAINING AN EOP PHRASE.".                          SQ2084.2
011700 01  INFO-LINE-2        PIC X(120)   VALUE "THIS TEST IS DESIGNED SQ2084.2
011800-    "TO TEST THE OVERFLOW RESULTS OF A WRITE AFTER ADVANCING OPERSQ2084.2
011900-    "ATION CONTAINING AN EOP PHRASE.".                           SQ2084.2
012000 01  INFO-LINE-3        PIC X(120)   VALUE "FOR THIS TEST LINAGE ASQ2084.2
012100-    "ND FOOTING VALUES ARE 40.".                                 SQ2084.2
012200 01  INFO-LINE-4        PIC X(120)   VALUE "39 DETAIL LINES SHOULDSQ2084.2
012300-    " PRINT ON THE 1ST LOGICAL PAGE AND THE REMAINING 21 DETAIL LSQ2084.2
012400-    "INES ON THE 2ND LOGICAL PAGE.".                             SQ2084.2
012500 01  INFO-LINE-6        PIC X(120)   VALUE "THE EOP LINE SHOULD FOSQ2084.2
012600-    "LLOW DETAIL LINE 39 AND BE THE LAST LINE ON THE 1ST LOGICAL SQ2084.2
012700-    "PAGE.".                                                     SQ2084.2
012800 01  INFO-LINE-7        PIC X(120)   VALUE "THE EOP LINE SHOULD FOSQ2084.2
012900-    "LLOW DETAIL LINE 39 AND BE THE FIRST LINE ON THE 2ND LOGICALSQ2084.2
013000-    " PAGE.".                                                    SQ2084.2
013100 01  INFO-LINE-8        PIC X(120)   VALUE "ALL LINAGE-COUNTER VALSQ2084.2
013200-    "UES REPRESENT VALUES PRIOR TO EXECUTION OF THE WRITE OPERATISQ2084.2
013300-    "ON.  NO MODIFICATIONS HAVE BEEN MADE.".                     SQ2084.2
013400 01  COMMENT-LINE-1     PIC X(120)   VALUE "BECAUSE OF THE NATURE SQ2084.2
013500-    "OF THESE TESTS A PASS OR FAIL CANNOT BE DETERMINED WITHIN THSQ2084.2
013600-    "E PROGRAM.  THE USER MUST VISUALLY".                        SQ2084.2
013700 01  COMMENT-LINE-2     PIC X(120)   VALUE "CHECK THE POSITION OF SQ2084.2
013800-    "EACH LINE TO DETERMINE THE ACCURACY OF THE VARIOUS WRITE OPTSQ2084.2
013900-    "IONS. VII-22, 3.2.2 LINAGE, VII-27-29".                     SQ2084.2
014000 01  COMMENT-LINE-3     PIC X(120)   VALUE "IN THIS TEST THE FIRSTSQ2084.2
014100-    " LOGICAL PAGE SHOULD CONTAIN 65 DETAIL LINES.".             SQ2084.2
014200 01  COMMENT-LINE-4     PIC X(120)   VALUE "1 FOOTING LINE AND BE SQ2084.2
014300-    "FOLLOWED BY 1 BLANK LINE.  ALL SUCCEEDING LOGICAL PAGES SHOUSQ2084.2
014400-    "LD CONTAIN 29 DETAIL LINES, 1 FOOTING".                     SQ2084.2
014500 01  COMMENT-LINE-5     PIC X(120)   VALUE "LINE AND BE SEPARATED SQ2084.2
014600-    "BY 3 BLANK LINES".                                          SQ2084.2
014700 01  COMMENT-LINE-6     PIC X(120)   VALUE "IN THIS TEST THE FIRSTSQ2084.2
014800-    " LOGICAL PAGE SHOULD CONTAIN 19 DETAIL LINES, 1 FOOTING LINESQ2084.2
014900-    ", BE PRECEDED BY 2 BLANK LINES, AND".                       SQ2084.2
015000 01  COMMENT-LINE-7     PIC X(120)   VALUE "BE FOLLOWED BY 4 BLANKSQ2084.2
015100-    " LINES.  ALL SUCCEEDING LOGICAL PAGES SHOULD CONTAIN 39 DETASQ2084.2
015200-    "IL LINES, 1 FOOTING LINE, AND BE".                          SQ2084.2
015300 01  COMMENT-LINE-8     PIC X(120)   VALUE "SEPARATED BY 3 BLANK LSQ2084.2
015400-    "INES.".                                                     SQ2084.2
015500 01  COMMENT-LINE-9     PIC X(120)   VALUE "IN THIS TEST THE FIRSTSQ2084.2
015600-    " LOGICAL PAGE SHOULD CONTAIN 15 DETAIL LINES, BE PRECEDED BYSQ2084.2
015700-    " 2 BLANK LINES, AND BE FOLLOWED BY".                        SQ2084.2
015800 01  COMMENT-LINE-10    PIC X(120)   VALUE "9 BLANK LINES.  ALL SUSQ2084.2
015900-    "CCEEDING LOGICAL PAGES SHOULD CONTAIN 40 DETAIL LINES AND BESQ2084.2
016000-    " SEPARATED BY 3 BLANK LINES.".                              SQ2084.2
016100 01  COMMENT-LINE-11    PIC X(120)   VALUE "THIS TEST IS DESIGNED SQ2084.2
016200-    "TO TEST THE MINIMUM LINAGE VALUE OF 1 AND THE MINIMUM TOP VASQ2084.2
016300-    "LUE OF ZERO.  EACH LOGICAL PAGE SHOULD".                    SQ2084.2
016400 01  COMMENT-LINE-12    PIC X(120)   VALUE "CONTAIN 1 DETAIL LINE SQ2084.2
016500-    "AND BE SEPARATED BY 2 BLANK LINES.  THE FIRST PAGE SHOULD NOSQ2084.2
016600-    "T BE PRECEDED BY ANY BLANK LINES.".                         SQ2084.2
016700 01  COMMENT-LINE-13    PIC X(120)   VALUE "THIS TEST IS DESIGNED SQ2084.2
016800-    "TO TEST THE MINIMUM FOOTING LINE VALUE OF 1 AND MINIMUM BOTTSQ2084.2
016900-    "OM VALUE OF ZERO.  EACH LOGICAL PAGE".                      SQ2084.2
017000 01  COMMENT-LINE-14    PIC X(120)   VALUE "SHOULD CONTAIN 1 DETAISQ2084.2
017100-    "L LINE, 1 FOOTING LINE, AND BE SEPARATED BY 1 BLANK LINE.". SQ2084.2
017200 01  LAST-LINE          PIC X(120)   VALUE "THIS IS THE LAST LINE SQ2084.2
017300-    "IN THE PAGE BODY OF THIS LOGICAL PAGE.  USE IT AS A REFERENCSQ2084.2
017400-    "E POINT FOR THE FOLLOWING TEST PAGES.".                     SQ2084.2
017500 01  TEST-RESULTS.                                                SQ2084.2
017600     02 FILLER                    PICTURE X VALUE SPACE.          SQ2084.2
017700     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2084.2
017800     02 FILLER                    PICTURE X VALUE SPACE.          SQ2084.2
017900     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2084.2
018000     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2084.2
018100     02  PAR-NAME.                                                SQ2084.2
018200       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2084.2
018300       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2084.2
018400       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2084.2
018500       03 FILLER PIC X(5) VALUE SPACE.                            SQ2084.2
018600     02 FILLER PIC X(10) VALUE SPACE.                             SQ2084.2
018700     02 RE-MARK PIC X(61).                                        SQ2084.2
018800 01  TEST-COMPUTED.                                               SQ2084.2
018900     02 FILLER PIC X(30) VALUE SPACE.                             SQ2084.2
019000     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2084.2
019100     02 COMPUTED-X.                                               SQ2084.2
019200     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2084.2
019300     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2084.2
019400     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2084.2
019500     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2084.2
019600     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2084.2
019700     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2084.2
019800         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2084.2
019900         04 FILLER                          PICTURE X.            SQ2084.2
020000     03 FILLER PIC X(50) VALUE SPACE.                             SQ2084.2
020100 01  TEST-CORRECT.                                                SQ2084.2
020200     02 FILLER PIC X(30) VALUE SPACE.                             SQ2084.2
020300     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2084.2
020400     02 CORRECT-X.                                                SQ2084.2
020500     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2084.2
020600     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2084.2
020700     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2084.2
020800     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2084.2
020900     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2084.2
021000     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2084.2
021100         04 CORRECT-18V0                    PICTURE -9(18).       SQ2084.2
021200         04 FILLER                          PICTURE X.            SQ2084.2
021300     03 FILLER PIC X(50) VALUE SPACE.                             SQ2084.2
021400 01  CCVS-C-1.                                                    SQ2084.2
021500     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2084.2
021600-    "SS  PARAGRAPH-NAME                                          SQ2084.2
021700-    "        REMARKS".                                           SQ2084.2
021800     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2084.2
021900 01  CCVS-C-2.                                                    SQ2084.2
022000     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2084.2
022100     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2084.2
022200     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2084.2
022300     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2084.2
022400     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2084.2
022500 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2084.2
022600 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2084.2
022700 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2084.2
022800 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2084.2
022900 01  INSPECT-COUNTER PIC 999 VALUE 7.                             SQ2084.2
023000 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2084.2
023100 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2084.2
023200 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2084.2
023300 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2084.2
023400 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2084.2
023500 01  CCVS-H-1.                                                    SQ2084.2
023600     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2084.2
023700     02 FILLER PICTURE X(67) VALUE                                SQ2084.2
023800     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2084.2
023900-    " SYSTEM".                                                   SQ2084.2
024000     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2084.2
024100 01  CCVS-H-2.                                                    SQ2084.2
024200     02 FILLER PICTURE X(52) VALUE IS                             SQ2084.2
024300     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2084.2
024400     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2084.2
024500     02 TEST-ID PICTURE IS X(9).                                  SQ2084.2
024600     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2084.2
024700 01  CCVS-H-3.                                                    SQ2084.2
024800     02  FILLER PICTURE X(34) VALUE                               SQ2084.2
024900     " FOR OFFICIAL USE ONLY    ".                                SQ2084.2
025000     02  FILLER PICTURE X(58) VALUE                               SQ2084.2
025100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2084.2
025200     02  FILLER PICTURE X(28) VALUE                               SQ2084.2
025300     "  COPYRIGHT   1985 ".                                       SQ2084.2
025400 01  CCVS-E-1.                                                    SQ2084.2
025500     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2084.2
025600     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2084.2
025700     02 ID-AGAIN PICTURE IS X(9).                                 SQ2084.2
025800     02 FILLER PICTURE X(45) VALUE IS                             SQ2084.2
025900     " NTIS DISTRIBUTION COBOL 85".                               SQ2084.2
026000 01  CCVS-E-2.                                                    SQ2084.2
026100     02  FILLER                   PICTURE X(31)  VALUE            SQ2084.2
026200     SPACE.                                                       SQ2084.2
026300     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2084.2
026400     02 CCVS-E-2-2.                                               SQ2084.2
026500         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2084.2
026600         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2084.2
026700         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2084.2
026800 01  CCVS-E-3.                                                    SQ2084.2
026900     02  FILLER PICTURE X(22) VALUE                               SQ2084.2
027000     " FOR OFFICIAL USE ONLY".                                    SQ2084.2
027100     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2084.2
027200     02  FILLER PICTURE X(58) VALUE                               SQ2084.2
027300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2084.2
027400     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2084.2
027500     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2084.2
027600 01  CCVS-E-4.                                                    SQ2084.2
027700     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2084.2
027800     02 FILLER PIC XXXX VALUE " OF ".                             SQ2084.2
027900     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2084.2
028000     02 FILLER PIC X(40) VALUE                                    SQ2084.2
028100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2084.2
028200 01  XXINFO.                                                      SQ2084.2
028300     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2084.2
028400     02 INFO-TEXT.                                                SQ2084.2
028500     04 FILLER PIC X(20) VALUE SPACE.                             SQ2084.2
028600     04 XXCOMPUTED PIC X(20).                                     SQ2084.2
028700     04 FILLER PIC X(5) VALUE SPACE.                              SQ2084.2
028800     04 XXCORRECT PIC X(20).                                      SQ2084.2
028900 01  HYPHEN-LINE.                                                 SQ2084.2
029000     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2084.2
029100     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2084.2
029200-    "*****************************************".                 SQ2084.2
029300     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2084.2
029400-    "******************************".                            SQ2084.2
029500 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2084.2
029600     "SQ208M".                                                    SQ2084.2
029700 PROCEDURE DIVISION.                                              SQ2084.2
029800 CCVS1 SECTION.                                                   SQ2084.2
029900 OPEN-FILES.                                                      SQ2084.2
030000*    OPEN I-O RAW-DATA.                                           SQ2084.2
030100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2084.2
030200*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2084.2
030300*    MOVE "ABORTED " TO C-ABORT.                                  SQ2084.2
030400*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2084.2
030500*    ACCEPT C-DATE  FROM DATE.                                    SQ2084.2
030600*    ACCEPT C-TIME  FROM TIME.                                    SQ2084.2
030700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2084.2
030800*END-E-1.                                                         SQ2084.2
030900*    CLOSE RAW-DATA.                                              SQ2084.2
031000     OPEN     OUTPUT PRINT-FILE.                                  SQ2084.2
031100     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2084.2
031200     MOVE    SPACE TO TEST-RESULTS.                               SQ2084.2
031300     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2084.2
031400     GO TO CCVS1-EXIT.                                            SQ2084.2
031500 CLOSE-FILES.                                                     SQ2084.2
031600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2084.2
031700*    OPEN I-O RAW-DATA.                                           SQ2084.2
031800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2084.2
031900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2084.2
032000*    MOVE "OK.     " TO C-ABORT.                                  SQ2084.2
032100*    MOVE PASS-COUNTER TO C-OK.                                   SQ2084.2
032200*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2084.2
032300*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2084.2
032400*    MOVE DELETE-CNT TO C-DELETED.                                SQ2084.2
032500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2084.2
032600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2084.2
032700*END-E-2.                                                         SQ2084.2
032800*    CLOSE RAW-DATA.                                              SQ2084.2
032900 TERMINATE-CCVS.                                                  SQ2084.2
033000*    EXIT PROGRAM.                                                SQ2084.2
033100*TERMINATE-CALL.                                                  SQ2084.2
033200     STOP     RUN.                                                SQ2084.2
033300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2084.2
033400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2084.2
033500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2084.2
033600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2084.2
033700     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2084.2
033800 PRINT-DETAIL.                                                    SQ2084.2
033900     IF REC-CT NOT EQUAL TO ZERO                                  SQ2084.2
034000             MOVE "." TO PARDOT-X                                 SQ2084.2
034100             MOVE REC-CT TO DOTVALUE.                             SQ2084.2
034200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2084.2
034300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2084.2
034400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2084.2
034500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2084.2
034600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2084.2
034700     MOVE SPACE TO CORRECT-X.                                     SQ2084.2
034800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2084.2
034900     MOVE     SPACE TO RE-MARK.                                   SQ2084.2
035000 HEAD-ROUTINE.                                                    SQ2084.2
035100     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2084.2
035200     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2084.2
035300     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2084.2
035400 COLUMN-NAMES-ROUTINE.                                            SQ2084.2
035500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2084.2
035600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2084.2
035700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2084.2
035800 END-ROUTINE.                                                     SQ2084.2
035900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2084.2
036000 END-RTN-EXIT.                                                    SQ2084.2
036100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2084.2
036200 END-ROUTINE-1.                                                   SQ2084.2
036300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2084.2
036400      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2084.2
036500      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2084.2
036600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2084.2
036700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2084.2
036800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2084.2
036900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2084.2
037000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2084.2
037100  END-ROUTINE-12.                                                 SQ2084.2
037200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2084.2
037300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2084.2
037400         MOVE "NO " TO ERROR-TOTAL                                SQ2084.2
037500         ELSE                                                     SQ2084.2
037600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2084.2
037700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2084.2
037800     PERFORM WRITE-LINE.                                          SQ2084.2
037900 END-ROUTINE-13.                                                  SQ2084.2
038000     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2084.2
038100         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2084.2
038200         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2084.2
038300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2084.2
038400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2084.2
038500      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2084.2
038600          MOVE "NO " TO ERROR-TOTAL                               SQ2084.2
038700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2084.2
038800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2084.2
038900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2084.2
039000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2084.2
039100 WRITE-LINE.                                                      SQ2084.2
039200     ADD 1 TO RECORD-COUNT.                                       SQ2084.2
039300*    IF RECORD-COUNT GREATER 50                                   SQ2084.2
039400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2084.2
039500*        MOVE SPACE TO DUMMY-RECORD                               SQ2084.2
039600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2084.2
039700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2084.2
039800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2084.2
039900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2084.2
040000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2084.2
040100*        MOVE ZERO TO RECORD-COUNT.                               SQ2084.2
040200     PERFORM WRT-LN.                                              SQ2084.2
040300 WRT-LN.                                                          SQ2084.2
040400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2084.2
040500     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
040600 BLANK-LINE-PRINT.                                                SQ2084.2
040700     PERFORM WRT-LN.                                              SQ2084.2
040800 FAIL-ROUTINE.                                                    SQ2084.2
040900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2084.2
041000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2084.2
041100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2084.2
041200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2084.2
041300     GO TO FAIL-ROUTINE-EX.                                       SQ2084.2
041400 FAIL-ROUTINE-WRITE.                                              SQ2084.2
041500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2084.2
041600     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2084.2
041700 FAIL-ROUTINE-EX. EXIT.                                           SQ2084.2
041800 BAIL-OUT.                                                        SQ2084.2
041900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2084.2
042000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2084.2
042100 BAIL-OUT-WRITE.                                                  SQ2084.2
042200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2084.2
042300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2084.2
042400 BAIL-OUT-EX. EXIT.                                               SQ2084.2
042500 CCVS1-EXIT.                                                      SQ2084.2
042600     EXIT.                                                        SQ2084.2
042700 SECT-SQ208M-0001 SECTION.                                        SQ2084.2
042800 WRITE-INIT-GF-01.                                                SQ2084.2
042900     MOVE COMMENT-LINE-1 TO PRINT-REC.                            SQ2084.2
043000     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
043100     MOVE COMMENT-LINE-2 TO PRINT-REC.                            SQ2084.2
043200     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
043300     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
043400     PERFORM BLANK-LINE-PRINT.                                    SQ2084.2
043500 WRITE-TEST-GF-01.                                                SQ2084.2
043600*             THIS TEST CHECKS THE RESULTS OF CHANGING THE VALUES SQ2084.2
043700*             OF THE DATA-NAMES IN THE LINAGE CLAUSE AFTER AN     SQ2084.2
043800*             OPEN OUTPUT PRINT-FILE.                             SQ2084.2
043900     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
044000     PERFORM INITIALIZE-PAGE.                                     SQ2084.2
044100     MOVE "LINAGE AFTER OPEN" TO FEATURE.                         SQ2084.2
044200     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         SQ2084.2
044300     PERFORM PRINT-DETAIL.                                        SQ2084.2
044400     MOVE COMMENT-LINE-3 TO PRINT-REC.                            SQ2084.2
044500     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
044600     MOVE COMMENT-LINE-4 TO PRINT-REC.                            SQ2084.2
044700     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
044800     MOVE COMMENT-LINE-5 TO PRINT-REC.                            SQ2084.2
044900     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2084.2
045000     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
045100     PERFORM PRINT-LAST-LINE.                                     SQ2084.2
045200     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
045300     PERFORM BLANK-LINE-PRINT.                                    SQ2084.2
045400     MOVE 30 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
045500     MOVE 1 TO TOP-CTR.                                           SQ2084.2
045600     MOVE 2 TO BOTTOM-CTR.                                        SQ2084.2
045700     MOVE 1 TO DL1-LINE-NO.                                       SQ2084.2
045800     MOVE 123 TO DL1-TOTAL-LINES.                                 SQ2084.2
045900     PERFORM PRINT-DETAIL-1 123 TIMES.                            SQ2084.2
046000 WRITE-TEST-GF-02.                                                SQ2084.2
046100*             THIS TEST CHECKS THE RESULTS OF CHANGING THE VALUES SQ2084.2
046200*             OF THE DATA-NAMES IN THE LINAGE CLAUSE PRIOR TO A   SQ2084.2
046300*             PAGE OVERFLOW.                                      SQ2084.2
046400     PERFORM INITIALIZE-PAGE.                                     SQ2084.2
046500     MOVE "LINAGE AFT OVERFLOW" TO FEATURE.                       SQ2084.2
046600     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         SQ2084.2
046700     PERFORM PRINT-DETAIL.                                        SQ2084.2
046800     MOVE COMMENT-LINE-6 TO PRINT-REC.                            SQ2084.2
046900     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
047000     MOVE COMMENT-LINE-7 TO PRINT-REC.                            SQ2084.2
047100     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
047200     MOVE COMMENT-LINE-8 TO PRINT-REC.                            SQ2084.2
047300     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
047400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
047500     PERFORM PRINT-LAST-LINE.                                     SQ2084.2
047600     MOVE 20 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
047700     MOVE 2 TO TOP-CTR.                                           SQ2084.2
047800     MOVE 3 TO BOTTOM-CTR.                                        SQ2084.2
047900     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
048000     PERFORM BLANK-LINE-PRINT.                                    SQ2084.2
048100     MOVE 40 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
048200     MOVE 1 TO TOP-CTR.                                           SQ2084.2
048300     MOVE 2 TO BOTTOM-CTR.                                        SQ2084.2
048400     MOVE 1 TO DL1-LINE-NO.                                       SQ2084.2
048500     MOVE 136 TO DL1-TOTAL-LINES.                                 SQ2084.2
048600     PERFORM PRINT-DETAIL-1 136 TIMES.                            SQ2084.2
048700 WRITE-TEST-GF-03.                                                SQ2084.2
048800*             THIS TEST CHECKS THE RESULTS OF CHANGING THE VALUES SQ2084.2
048900*             OF THE DATA-NAMES IN THE LINAGE-CLAUSE PRIOR TO A   SQ2084.2
049000*             WRITE ADVANCING PAGE OPERATION.                     SQ2084.2
049100     PERFORM INITIALIZE-PAGE.                                     SQ2084.2
049200     MOVE "LINAGE AFT WRT PAGE" TO FEATURE.                       SQ2084.2
049300     MOVE "WRITE-TEST-GF-03" TO PAR-NAME.                         SQ2084.2
049400     PERFORM PRINT-DETAIL.                                        SQ2084.2
049500     MOVE COMMENT-LINE-9 TO PRINT-REC.                            SQ2084.2
049600     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
049700     MOVE COMMENT-LINE-10 TO PRINT-REC.                           SQ2084.2
049800     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2084.2
049900     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
050000     PERFORM PRINT-LAST-LINE.                                     SQ2084.2
050100     MOVE 20 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
050200     MOVE 2 TO TOP-CTR.                                           SQ2084.2
050300     MOVE 3 TO BOTTOM-CTR.                                        SQ2084.2
050400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
050500     PERFORM BLANK-LINE-PRINT.                                    SQ2084.2
050600     MOVE 1 TO DL1-LINE-NO.                                       SQ2084.2
050700     MOVE 135 TO DL1-TOTAL-LINES.                                 SQ2084.2
050800     PERFORM PRINT-DETAIL-2 15 TIMES.                             SQ2084.2
050900     MOVE 40 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
051000     MOVE 1 TO TOP-CTR.                                           SQ2084.2
051100     MOVE 2 TO BOTTOM-CTR.                                        SQ2084.2
051200     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
051300     WRITE PRINT-REC BEFORE ADVANCING PAGE.                       SQ2084.2
051400     MOVE 16 TO DL1-LINE-NO.                                      SQ2084.2
051500     PERFORM PRINT-DETAIL-2 120 TIMES.                            SQ2084.2
051600 WRITE-TEST-GF-04.                                                SQ2084.2
051700*             THIS TEST CHECKS THE MINIMUM LINAGE VALUE OF 1      SQ2084.2
051800*             AND THE MINIMUM TOP VALUE OF ZERO.                  SQ2084.2
051900     PERFORM INITIALIZE-PAGE.                                     SQ2084.2
052000     MOVE "MIN LINAGE / 0 TOP" TO FEATURE.                        SQ2084.2
052100     MOVE "WRITE-TEST-GF-04" TO PAR-NAME.                         SQ2084.2
052200     PERFORM PRINT-DETAIL.                                        SQ2084.2
052300     MOVE COMMENT-LINE-11 TO PRINT-REC.                           SQ2084.2
052400     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
052500     MOVE COMMENT-LINE-12 TO PRINT-REC.                           SQ2084.2
052600     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
052700     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
052800     PERFORM PRINT-LAST-LINE.                                     SQ2084.2
052900     MOVE 1 TO LINAGE-CTR, FOOT-CTR.                              SQ2084.2
053000     MOVE 0 TO TOP-CTR.                                           SQ2084.2
053100     MOVE 2 TO BOTTOM-CTR.                                        SQ2084.2
053200     MOVE 1 TO DL2-LINE-NO.                                       SQ2084.2
053300     PERFORM PRINT-DETAIL-3 10 TIMES.                             SQ2084.2
053400 WRITE-TEST-GF-05.                                                SQ2084.2
053500*             THIS TEST CHECKS THE MINIMUM FOOTING VALUE OF 1     SQ2084.2
053600*             AND THE MINIMUM BOTTOM VALUE OF ZERO.               SQ2084.2
053700     PERFORM INITIALIZE-PAGE.                                     SQ2084.2
053800     MOVE "MIN FOOTING / 0 BOTM" TO FEATURE.                      SQ2084.2
053900     MOVE "WRITE-TEST-GF-05" TO PAR-NAME.                         SQ2084.2
054000     PERFORM PRINT-DETAIL.                                        SQ2084.2
054100     MOVE COMMENT-LINE-13 TO PRINT-REC.                           SQ2084.2
054200     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
054300     MOVE COMMENT-LINE-14 TO PRINT-REC.                           SQ2084.2
054400     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
054500     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
054600     PERFORM PRINT-LAST-LINE.                                     SQ2084.2
054700     MOVE 2 TO LINAGE-CTR.                                        SQ2084.2
054800     MOVE 1 TO FOOT-CTR, TOP-CTR.                                 SQ2084.2
054900     MOVE 0 TO BOTTOM-CTR.                                        SQ2084.2
055000     MOVE 1 TO DL2-LINE-NO.                                       SQ2084.2
055100     PERFORM PRINT-DETAIL-4 10 TIMES.                             SQ2084.2
055200 WRITE-TEST-GF-06.                                                SQ2084.2
055300*             THIS TEST SHOWS THE RESULTS OF A WRITE BEFORE       SQ2084.2
055400*             OPERATION WITH AN EOP PHRASE.  LINAGE AND FOOTING   SQ2084.2
055500*             VALUES ARE SPECIFIED AND EQUAL.                     SQ2084.2
055600     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
055700     PERFORM INITIALIZE-PAGE.                                     SQ2084.2
055800     MOVE "WRITE BEFORE" TO FEATURE.                              SQ2084.2
055900     MOVE "WRITE-TEST-GF-06" TO PAR-NAME.                         SQ2084.2
056000     PERFORM PRINT-DETAIL.                                        SQ2084.2
056100     MOVE INFO-LINE-1 TO PRINT-REC.                               SQ2084.2
056200     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
056300     MOVE INFO-LINE-3 TO PRINT-REC.                               SQ2084.2
056400     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2084.2
056500     MOVE INFO-LINE-4 TO PRINT-REC.                               SQ2084.2
056600     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
056700     MOVE INFO-LINE-6 TO PRINT-REC.                               SQ2084.2
056800     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2084.2
056900     MOVE INFO-LINE-8 TO PRINT-REC.                               SQ2084.2
057000     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2084.2
057100     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
057200     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 66.      SQ2084.2
057300     MOVE 40 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
057400     MOVE 1 TO TOP-CTR, BOTTOM-CTR.                               SQ2084.2
057500     MOVE LAST-LINE TO PRINT-REC.                                 SQ2084.2
057600     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2084.2
057700     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
057800     MOVE 1 TO DL3-LINE-NO.                                       SQ2084.2
057900     PERFORM WRITE-BEFORE 60 TIMES.                               SQ2084.2
058000 WRITE-TEST-GF-07.                                                SQ2084.2
058100*             THIS TEST SHOWS THE RESULTS OF A WRITE AFTER        SQ2084.2
058200*             OPERATION WITH AN EOP PHRASE.  LINAGE AND FOOTING   SQ2084.2
058300*             VALUES ARE SPECIFIED AND EQUAL.                     SQ2084.2
058400     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
058500     PERFORM INITIALIZE-PAGE.                                     SQ2084.2
058600     MOVE "WRITE AFTER" TO FEATURE.                               SQ2084.2
058700     MOVE "WRITE-TEST-GF-07" TO PAR-NAME.                         SQ2084.2
058800     PERFORM PRINT-DETAIL.                                        SQ2084.2
058900     MOVE INFO-LINE-2 TO PRINT-REC.                               SQ2084.2
059000     WRITE PRINT-REC AFTER ADVANCING 5 LINES.                     SQ2084.2
059100     MOVE INFO-LINE-3 TO PRINT-REC.                               SQ2084.2
059200     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2084.2
059300     MOVE INFO-LINE-4 TO PRINT-REC.                               SQ2084.2
059400     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
059500     MOVE INFO-LINE-7 TO PRINT-REC.                               SQ2084.2
059600     WRITE PRINT-REC AFTER ADVANCING 1 LINES.                     SQ2084.2
059700     MOVE INFO-LINE-8 TO PRINT-REC.                               SQ2084.2
059800     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
059900     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
060000     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 66.      SQ2084.2
060100     MOVE 40 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
060200     MOVE 1 TO TOP-CTR, BOTTOM-CTR.                               SQ2084.2
060300     MOVE LAST-LINE TO PRINT-REC.                                 SQ2084.2
060400     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
060500     MOVE SPACE TO DUMMY-RECORD.                                  SQ2084.2
060600     MOVE 1 TO DL3-LINE-NO.                                       SQ2084.2
060700     PERFORM WRITE-AFTER 60 TIMES.                                SQ2084.2
060800 SQ208M-END-ROUTINE.                                              SQ2084.2
060900     MOVE "END OF SQ208M VALIDATION TESTS" TO PRINT-REC.          SQ2084.2
061000     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
061100     GO TO CCVS-EXIT.                                             SQ2084.2
061200 INITIALIZE-PAGE.                                                 SQ2084.2
061300     MOVE 0 TO TOP-CTR, BOTTOM-CTR.                               SQ2084.2
061400     MOVE 66 TO LINAGE-CTR, FOOT-CTR.                             SQ2084.2
061500     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 1.       SQ2084.2
061600 PRINT-LAST-LINE.                                                 SQ2084.2
061700     PERFORM BLANK-LINE-PRINT UNTIL LINAGE-COUNTER EQUAL 65.      SQ2084.2
061800     MOVE LAST-LINE TO PRINT-REC.                                 SQ2084.2
061900     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
062000 PRINT-DETAIL-1.                                                  SQ2084.2
062100     MOVE LINAGE-COUNTER TO DL1-LC.                               SQ2084.2
062200     MOVE DETAIL-LINE-1 TO PRINT-REC.                             SQ2084.2
062300     WRITE PRINT-REC BEFORE ADVANCING 1 LINE AT END-OF-PAGE       SQ2084.2
062400              MOVE LINAGE-COUNTER TO FL1-LC                       SQ2084.2
062500              MOVE FOOT-LINE-1 TO PRINT-REC                       SQ2084.2
062600              WRITE PRINT-REC BEFORE ADVANCING 1 LINE.            SQ2084.2
062700     ADD 1 TO DL1-LINE-NO.                                        SQ2084.2
062800 PRINT-DETAIL-2.                                                  SQ2084.2
062900     MOVE LINAGE-COUNTER TO DL1-LC.                               SQ2084.2
063000     MOVE DETAIL-LINE-1 TO PRINT-REC.                             SQ2084.2
063100     WRITE PRINT-REC BEFORE ADVANCING 1 LINE.                     SQ2084.2
063200     ADD 1 TO DL1-LINE-NO.                                        SQ2084.2
063300 PRINT-DETAIL-3.                                                  SQ2084.2
063400     MOVE LINAGE-COUNTER TO DL2-LC.                               SQ2084.2
063500     MOVE DETAIL-LINE-2 TO PRINT-REC.                             SQ2084.2
063600     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2084.2
063700     ADD 1 TO DL2-LINE-NO.                                        SQ2084.2
063800 PRINT-DETAIL-4.                                                  SQ2084.2
063900     MOVE LINAGE-COUNTER TO DL2-LC.                               SQ2084.2
064000     MOVE DETAIL-LINE-2 TO PRINT-REC.                             SQ2084.2
064100     WRITE PRINT-REC AFTER ADVANCING 1 LINE AT EOP                SQ2084.2
064200              MOVE LINAGE-COUNTER TO FL1-LC                       SQ2084.2
064300              MOVE FOOT-LINE-1 TO PRINT-REC                       SQ2084.2
064400              WRITE PRINT-REC AFTER ADVANCING 1 LINE.             SQ2084.2
064500     ADD 1 TO DL2-LINE-NO.                                        SQ2084.2
064600 WRITE-BEFORE.                                                    SQ2084.2
064700     MOVE LINAGE-COUNTER TO DL3-LC.                               SQ2084.2
064800     MOVE DETAIL-LINE-3 TO PRINT-REC.                             SQ2084.2
064900     WRITE PRINT-REC BEFORE ADVANCING 1 LINE AT EOP               SQ2084.2
065000              MOVE LINAGE-COUNTER TO FL3-LC                       SQ2084.2
065100              MOVE FOOT-LINE-3 TO PRINT-REC                       SQ2084.2
065200              WRITE PRINT-REC BEFORE ADVANCING 1 LINE.            SQ2084.2
065300     ADD 1 TO DL3-LINE-NO.                                        SQ2084.2
065400 WRITE-AFTER.                                                     SQ2084.2
065500     MOVE LINAGE-COUNTER TO DL3-LC.                               SQ2084.2
065600     MOVE DETAIL-LINE-3 TO PRINT-REC.                             SQ2084.2
065700     WRITE PRINT-REC AFTER ADVANCING 1 LINE AT EOP                SQ2084.2
065800              MOVE LINAGE-COUNTER TO FL3-LC                       SQ2084.2
065900              MOVE FOOT-LINE-3 TO PRINT-REC                       SQ2084.2
066000              WRITE PRINT-REC AFTER ADVANCING 1 LINE.             SQ2084.2
066100     ADD 1 TO DL3-LINE-NO.                                        SQ2084.2
066200 CCVS-EXIT SECTION.                                               SQ2084.2
066300 CCVS-999999.                                                     SQ2084.2
066400     GO TO CLOSE-FILES.                                           SQ2084.2
      *END-OF,SQ208M                                                                  
