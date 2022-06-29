      *HEADER,COBOL,SM201A                                                            
000100 IDENTIFICATION DIVISION.                                         SM2014.2
000200 PROGRAM-ID.                                                      SM2014.2
000300     SM201A.                                                      SM2014.2
000400****************************************************************  SM2014.2
000500*                                                              *  SM2014.2
000600*    VALIDATION FOR:-                                          *  SM2014.2
000700*                                                              *  SM2014.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2014.2
000900*                                                              *  SM2014.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2014.2
001100*                                                              *  SM2014.2
001200****************************************************************  SM2014.2
001300*                                                              *  SM2014.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  SM2014.2
001500*                                                              *  SM2014.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  SM2014.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  SM2014.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  SM2014.2
001900*                                                              *  SM2014.2
002000****************************************************************  SM2014.2
002100*                                                              *  SM2014.2
002200*    PROGRAM SM201A TESTS THE "REPLACING" PHRASE OF THE "COPY" *  SM2014.2
002300*    STATEMENT IN THE WORKING-STORAGE SECTION AND              *  SM2014.2
002400*    PROCEDURE DIVISION AND PRODUCES A SEQUENTIAL OUTPUT FILE  *  SM2014.2
002500*    USING "COPY"ED CODE, WHICH IS SUBSEQUENTLY CHECKED BY     *  SM2014.2
002600*    SM202A.                                                   *  SM2014.2
002700*                                                              *  SM2014.2
002800****************************************************************  SM2014.2
002900 ENVIRONMENT DIVISION.                                            SM2014.2
003000 CONFIGURATION SECTION.                                           SM2014.2
003100 SOURCE-COMPUTER.                                                 SM2014.2
003200     XXXXX082.                                                    SM2014.2
003300 OBJECT-COMPUTER.                                                 SM2014.2
003400     XXXXX083.                                                    SM2014.2
003500 INPUT-OUTPUT SECTION.                                            SM2014.2
003600 FILE-CONTROL.                                                    SM2014.2
003700     SELECT PRINT-FILE ASSIGN TO                                  SM2014.2
003800     XXXXX055.                                                    SM2014.2
003900     SELECT TEST-FILE ASSIGN TO                                   SM2014.2
004000     XXXXP001.                                                    SM2014.2
004100 DATA DIVISION.                                                   SM2014.2
004200 FILE SECTION.                                                    SM2014.2
004300 FD  PRINT-FILE.                                                  SM2014.2
004400 01  PRINT-REC PICTURE X(120).                                    SM2014.2
004500 01  DUMMY-RECORD PICTURE X(120).                                 SM2014.2
004600                                                                  SM2014.2
004700                                                                  SM2014.2
004800                                                                  SM2014.2
004900                                                                  SM2014.2
005000                                                                  SM2014.2
005100*                                                                 SM2014.2
005200*********************** COPY STATEMENT USED **********************SM2014.2
005300*                                                                 SM2014.2
005400*FD  TEST-FILE                                         COPY K1FDA SM2014.2
005500*             REPLACING                                           SM2014.2
005600*             PROOF-REC BY TST-TEST.                              SM2014.2
005700*                                                                 SM2014.2
005800******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
005900 FD  TEST-FILE                                         COPY K1FDA SM2014.2
006000              REPLACING                                           SM2014.2
006100              PROOF-REC BY TST-TEST.                              SM2014.2
006200*********************** END OF COPIED TEXT ***********************SM2014.2
006300                                                                  SM2014.2
006400                                                                  SM2014.2
006500                                                                  SM2014.2
006600                                                                  SM2014.2
006700                                                                  SM2014.2
006800*                                                                 SM2014.2
006900*********************** COPY STATEMENT USED **********************SM2014.2
007000*                                                                 SM2014.2
007100*01  TST-TEST                                          COPY K101A SM2014.2
007200*             REPLACING                                           SM2014.2
007300*                       TST-FLD-1 BY TF-1.                        SM2014.2
007400*                                                                 SM2014.2
007500******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
007600 01  TST-TEST                                          COPY K101A SM2014.2
007700              REPLACING                                           SM2014.2
007800                        TST-FLD-1 BY TF-1.                        SM2014.2
007900*********************** END OF COPIED TEXT ***********************SM2014.2
008000 WORKING-STORAGE SECTION.                                         SM2014.2
008100 77  RCD-1 PICTURE 9(5) VALUE 97532.                              SM2014.2
008200 77  RCD-2 PICTURE 9(5) VALUE 23479.                              SM2014.2
008300 77  RCD-3 PICTURE 9(5) VALUE 10901.                              SM2014.2
008400 77  RCD-4 PICTURE 9(5) VALUE 02734.                              SM2014.2
008500 77  RCD-5 PICTURE 9(5) VALUE 14003.                              SM2014.2
008600 77  RCD-6 PICTURE 9(5) VALUE 19922.                              SM2014.2
008700 77  RCD-7 PICTURE 9(5) VALUE 03543.                              SM2014.2
008800*                                                                 SM2014.2
008900*********************** COPY STATEMENT USED **********************SM2014.2
009000*                                                                 SM2014.2
009100*01  TEXT-TEST-1 COPY K101A                                       SM2014.2
009200*            REPLACING ==02 TST-FLD-1  PICTURE 9(5). 02 FILLER    SM2014.2
009300*                      PICTURE X(115)==                           SM2014.2
009400*            BY        ==02 FILLER PICTURE X(115).  02 TXT-FLD-1  SM2014.2
009500*                      PIC 9(5)==.                                SM2014.2
009600*                                                                 SM2014.2
009700******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
009800 01  TEXT-TEST-1 COPY K101A                                       SM2014.2
009900             REPLACING ==02 TST-FLD-1  PICTURE 9(5). 02 FILLER    SM2014.2
010000                       PICTURE X(115)==                           SM2014.2
010100             BY        ==02 FILLER PICTURE X(115).  02 TXT-FLD-1  SM2014.2
010200                       PIC 9(5)==.                                SM2014.2
010300*********************** END OF COPIED TEXT ***********************SM2014.2
010400 01  WSTR-1.                                                      SM2014.2
010500     02  WSTR-1A PICTURE XXX VALUE "ABC".                         SM2014.2
010600                                                                  SM2014.2
010700                                                                  SM2014.2
010800                                                                  SM2014.2
010900                                                                  SM2014.2
011000                                                                  SM2014.2
011100 01  WSTR-2.                                                      SM2014.2
011200*                                                                 SM2014.2
011300*********************** COPY STATEMENT USED **********************SM2014.2
011400*                                                                 SM2014.2
011500*                                                      COPY K1WKA SM2014.2
011600*             REPLACING WSTR-2A BY WSTR999.                       SM2014.2
011700*                                                                 SM2014.2
011800******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
011900                                                       COPY K1WKA SM2014.2
012000              REPLACING WSTR-2A BY WSTR999.                       SM2014.2
012100*********************** END OF COPIED TEXT ***********************SM2014.2
012200                                                                  SM2014.2
012300                                                                  SM2014.2
012400                                                                  SM2014.2
012500                                                                  SM2014.2
012600                                                                  SM2014.2
012700 01  WSTR-3.                                                      SM2014.2
012800*                                                                 SM2014.2
012900*********************** COPY STATEMENT USED **********************SM2014.2
013000*                                                                 SM2014.2
013100*                                                      COPY K1WKA.SM2014.2
013200*                                                                 SM2014.2
013300******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
013400                                                       COPY K1WKA.SM2014.2
013500*********************** END OF COPIED TEXT ***********************SM2014.2
013600                                                                  SM2014.2
013700                                                                  SM2014.2
013800                                                                  SM2014.2
013900                                                                  SM2014.2
014000                                                                  SM2014.2
014100*                                                                 SM2014.2
014200*********************** COPY STATEMENT USED **********************SM2014.2
014300*                                                                 SM2014.2
014400*01  WSTR-4.                                           COPY K1WKB SM2014.2
014500*             REPLACING WSTR4A BY WSTR91                          SM2014.2
014600*                       WSTR4B BY WSTR92                          SM2014.2
014700*                       WSTR4C BY WSTR93.                         SM2014.2
014800*                                                                 SM2014.2
014900******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
015000 01  WSTR-4.                                           COPY K1WKB SM2014.2
015100              REPLACING WSTR4A BY WSTR91                          SM2014.2
015200                        WSTR4B BY WSTR92                          SM2014.2
015300                        WSTR4C BY WSTR93.                         SM2014.2
015400*********************** END OF COPIED TEXT ***********************SM2014.2
015500                                                                  SM2014.2
015600                                                                  SM2014.2
015700                                                                  SM2014.2
015800                                                                  SM2014.2
015900                                                                  SM2014.2
016000*                                                                 SM2014.2
016100*********************** COPY STATEMENT USED **********************SM2014.2
016200*                                                                 SM2014.2
016300*01  WSTR-5.                                           COPY K1WKB.SM2014.2
016400*                                                                 SM2014.2
016500******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
016600 01  WSTR-5.                                           COPY K1WKB.SM2014.2
016700*********************** END OF COPIED TEXT ***********************SM2014.2
016800 01  TEST-RESULTS.                                                SM2014.2
016900     02 FILLER                   PIC X      VALUE SPACE.          SM2014.2
017000     02 FEATURE                  PIC X(20)  VALUE SPACE.          SM2014.2
017100     02 FILLER                   PIC X      VALUE SPACE.          SM2014.2
017200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          SM2014.2
017300     02 FILLER                   PIC X      VALUE SPACE.          SM2014.2
017400     02  PAR-NAME.                                                SM2014.2
017500       03 FILLER                 PIC X(19)  VALUE SPACE.          SM2014.2
017600       03  PARDOT-X              PIC X      VALUE SPACE.          SM2014.2
017700       03 DOTVALUE               PIC 99     VALUE ZERO.           SM2014.2
017800     02 FILLER                   PIC X(8)   VALUE SPACE.          SM2014.2
017900     02 RE-MARK                  PIC X(61).                       SM2014.2
018000 01  TEST-COMPUTED.                                               SM2014.2
018100     02 FILLER                   PIC X(30)  VALUE SPACE.          SM2014.2
018200     02 FILLER                   PIC X(17)  VALUE                 SM2014.2
018300            "       COMPUTED=".                                   SM2014.2
018400     02 COMPUTED-X.                                               SM2014.2
018500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          SM2014.2
018600     03 COMPUTED-N               REDEFINES COMPUTED-A             SM2014.2
018700                                 PIC -9(9).9(9).                  SM2014.2
018800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         SM2014.2
018900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     SM2014.2
019000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     SM2014.2
019100     03       CM-18V0 REDEFINES COMPUTED-A.                       SM2014.2
019200         04 COMPUTED-18V0                    PIC -9(18).          SM2014.2
019300         04 FILLER                           PIC X.               SM2014.2
019400     03 FILLER PIC X(50) VALUE SPACE.                             SM2014.2
019500 01  TEST-CORRECT.                                                SM2014.2
019600     02 FILLER PIC X(30) VALUE SPACE.                             SM2014.2
019700     02 FILLER PIC X(17) VALUE "       CORRECT =".                SM2014.2
019800     02 CORRECT-X.                                                SM2014.2
019900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         SM2014.2
020000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      SM2014.2
020100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         SM2014.2
020200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     SM2014.2
020300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     SM2014.2
020400     03      CR-18V0 REDEFINES CORRECT-A.                         SM2014.2
020500         04 CORRECT-18V0                     PIC -9(18).          SM2014.2
020600         04 FILLER                           PIC X.               SM2014.2
020700     03 FILLER PIC X(2) VALUE SPACE.                              SM2014.2
020800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     SM2014.2
020900 01  CCVS-C-1.                                                    SM2014.2
021000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PASM2014.2
021100-    "SS  PARAGRAPH-NAME                                          SM2014.2
021200-    "       REMARKS".                                            SM2014.2
021300     02 FILLER                     PIC X(20)    VALUE SPACE.      SM2014.2
021400 01  CCVS-C-2.                                                    SM2014.2
021500     02 FILLER                     PIC X        VALUE SPACE.      SM2014.2
021600     02 FILLER                     PIC X(6)     VALUE "TESTED".   SM2014.2
021700     02 FILLER                     PIC X(15)    VALUE SPACE.      SM2014.2
021800     02 FILLER                     PIC X(4)     VALUE "FAIL".     SM2014.2
021900     02 FILLER                     PIC X(94)    VALUE SPACE.      SM2014.2
022000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       SM2014.2
022100 01  REC-CT                        PIC 99       VALUE ZERO.       SM2014.2
022200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       SM2014.2
022300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       SM2014.2
022400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       SM2014.2
022500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       SM2014.2
022600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       SM2014.2
022700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       SM2014.2
022800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      SM2014.2
022900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       SM2014.2
023000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     SM2014.2
023100 01  CCVS-H-1.                                                    SM2014.2
023200     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2014.2
023300     02  FILLER                    PIC X(42)    VALUE             SM2014.2
023400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 SM2014.2
023500     02  FILLER                    PIC X(39)    VALUE SPACES.     SM2014.2
023600 01  CCVS-H-2A.                                                   SM2014.2
023700   02  FILLER                        PIC X(40)  VALUE SPACE.      SM2014.2
023800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  SM2014.2
023900   02  FILLER                        PIC XXXX   VALUE             SM2014.2
024000     "4.2 ".                                                      SM2014.2
024100   02  FILLER                        PIC X(28)  VALUE             SM2014.2
024200            " COPY - NOT FOR DISTRIBUTION".                       SM2014.2
024300   02  FILLER                        PIC X(41)  VALUE SPACE.      SM2014.2
024400                                                                  SM2014.2
024500 01  CCVS-H-2B.                                                   SM2014.2
024600   02  FILLER                        PIC X(15)  VALUE             SM2014.2
024700            "TEST RESULT OF ".                                    SM2014.2
024800   02  TEST-ID                       PIC X(9).                    SM2014.2
024900   02  FILLER                        PIC X(4)   VALUE             SM2014.2
025000            " IN ".                                               SM2014.2
025100   02  FILLER                        PIC X(12)  VALUE             SM2014.2
025200     " HIGH       ".                                              SM2014.2
025300   02  FILLER                        PIC X(22)  VALUE             SM2014.2
025400            " LEVEL VALIDATION FOR ".                             SM2014.2
025500   02  FILLER                        PIC X(58)  VALUE             SM2014.2
025600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2014.2
025700 01  CCVS-H-3.                                                    SM2014.2
025800     02  FILLER                      PIC X(34)  VALUE             SM2014.2
025900            " FOR OFFICIAL USE ONLY    ".                         SM2014.2
026000     02  FILLER                      PIC X(58)  VALUE             SM2014.2
026100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SM2014.2
026200     02  FILLER                      PIC X(28)  VALUE             SM2014.2
026300            "  COPYRIGHT   1985 ".                                SM2014.2
026400 01  CCVS-E-1.                                                    SM2014.2
026500     02 FILLER                       PIC X(52)  VALUE SPACE.      SM2014.2
026600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              SM2014.2
026700     02 ID-AGAIN                     PIC X(9).                    SM2014.2
026800     02 FILLER                       PIC X(45)  VALUE SPACES.     SM2014.2
026900 01  CCVS-E-2.                                                    SM2014.2
027000     02  FILLER                      PIC X(31)  VALUE SPACE.      SM2014.2
027100     02  FILLER                      PIC X(21)  VALUE SPACE.      SM2014.2
027200     02 CCVS-E-2-2.                                               SM2014.2
027300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      SM2014.2
027400         03 FILLER                   PIC X      VALUE SPACE.      SM2014.2
027500         03 ENDER-DESC               PIC X(44)  VALUE             SM2014.2
027600            "ERRORS ENCOUNTERED".                                 SM2014.2
027700 01  CCVS-E-3.                                                    SM2014.2
027800     02  FILLER                      PIC X(22)  VALUE             SM2014.2
027900            " FOR OFFICIAL USE ONLY".                             SM2014.2
028000     02  FILLER                      PIC X(12)  VALUE SPACE.      SM2014.2
028100     02  FILLER                      PIC X(58)  VALUE             SM2014.2
028200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SM2014.2
028300     02  FILLER                      PIC X(13)  VALUE SPACE.      SM2014.2
028400     02 FILLER                       PIC X(15)  VALUE             SM2014.2
028500             " COPYRIGHT 1985".                                   SM2014.2
028600 01  CCVS-E-4.                                                    SM2014.2
028700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      SM2014.2
028800     02 FILLER                       PIC X(4)   VALUE " OF ".     SM2014.2
028900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      SM2014.2
029000     02 FILLER                       PIC X(40)  VALUE             SM2014.2
029100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SM2014.2
029200 01  XXINFO.                                                      SM2014.2
029300     02 FILLER                       PIC X(19)  VALUE             SM2014.2
029400            "*** INFORMATION ***".                                SM2014.2
029500     02 INFO-TEXT.                                                SM2014.2
029600       04 FILLER                     PIC X(8)   VALUE SPACE.      SM2014.2
029700       04 XXCOMPUTED                 PIC X(20).                   SM2014.2
029800       04 FILLER                     PIC X(5)   VALUE SPACE.      SM2014.2
029900       04 XXCORRECT                  PIC X(20).                   SM2014.2
030000     02 INF-ANSI-REFERENCE           PIC X(48).                   SM2014.2
030100 01  HYPHEN-LINE.                                                 SM2014.2
030200     02 FILLER  PIC IS X VALUE IS SPACE.                          SM2014.2
030300     02 FILLER  PIC IS X(65)    VALUE IS "************************SM2014.2
030400-    "*****************************************".                 SM2014.2
030500     02 FILLER  PIC IS X(54)    VALUE IS "************************SM2014.2
030600-    "******************************".                            SM2014.2
030700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             SM2014.2
030800     "SM201A".                                                    SM2014.2
030900 PROCEDURE DIVISION.                                              SM2014.2
031000 CCVS1 SECTION.                                                   SM2014.2
031100 OPEN-FILES.                                                      SM2014.2
031200     OPEN     OUTPUT PRINT-FILE.                                  SM2014.2
031300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SM2014.2
031400     MOVE    SPACE TO TEST-RESULTS.                               SM2014.2
031500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SM2014.2
031600     GO TO CCVS1-EXIT.                                            SM2014.2
031700 CLOSE-FILES.                                                     SM2014.2
031800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SM2014.2
031900 TERMINATE-CCVS.                                                  SM2014.2
032000*    EXIT PROGRAM.                                                SM2014.2
032100*TERMINATE-CALL.                                                  SM2014.2
032200     STOP     RUN.                                                SM2014.2
032300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SM2014.2
032400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SM2014.2
032500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SM2014.2
032600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      SM2014.2
032700     MOVE "****TEST DELETED****" TO RE-MARK.                      SM2014.2
032800 PRINT-DETAIL.                                                    SM2014.2
032900     IF REC-CT NOT EQUAL TO ZERO                                  SM2014.2
033000             MOVE "." TO PARDOT-X                                 SM2014.2
033100             MOVE REC-CT TO DOTVALUE.                             SM2014.2
033200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SM2014.2
033300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SM2014.2
033400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SM2014.2
033500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SM2014.2
033600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SM2014.2
033700     MOVE SPACE TO CORRECT-X.                                     SM2014.2
033800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SM2014.2
033900     MOVE     SPACE TO RE-MARK.                                   SM2014.2
034000 HEAD-ROUTINE.                                                    SM2014.2
034100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2014.2
034200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  SM2014.2
034300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2014.2
034400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  SM2014.2
034500 COLUMN-NAMES-ROUTINE.                                            SM2014.2
034600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2014.2
034700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2014.2
034800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SM2014.2
034900 END-ROUTINE.                                                     SM2014.2
035000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SM2014.2
035100 END-RTN-EXIT.                                                    SM2014.2
035200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2014.2
035300 END-ROUTINE-1.                                                   SM2014.2
035400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SM2014.2
035500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               SM2014.2
035600      ADD PASS-COUNTER TO ERROR-HOLD.                             SM2014.2
035700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SM2014.2
035800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SM2014.2
035900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SM2014.2
036000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SM2014.2
036100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SM2014.2
036200  END-ROUTINE-12.                                                 SM2014.2
036300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SM2014.2
036400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SM2014.2
036500         MOVE "NO " TO ERROR-TOTAL                                SM2014.2
036600         ELSE                                                     SM2014.2
036700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SM2014.2
036800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SM2014.2
036900     PERFORM WRITE-LINE.                                          SM2014.2
037000 END-ROUTINE-13.                                                  SM2014.2
037100     IF DELETE-COUNTER IS EQUAL TO ZERO                           SM2014.2
037200         MOVE "NO " TO ERROR-TOTAL  ELSE                          SM2014.2
037300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      SM2014.2
037400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SM2014.2
037500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2014.2
037600      IF   INSPECT-COUNTER EQUAL TO ZERO                          SM2014.2
037700          MOVE "NO " TO ERROR-TOTAL                               SM2014.2
037800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SM2014.2
037900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SM2014.2
038000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SM2014.2
038100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SM2014.2
038200 WRITE-LINE.                                                      SM2014.2
038300     ADD 1 TO RECORD-COUNT.                                       SM2014.2
038400*    IF RECORD-COUNT GREATER 50                                   SM2014.2
038500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SM2014.2
038600*        MOVE SPACE TO DUMMY-RECORD                               SM2014.2
038700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SM2014.2
038800*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SM2014.2
038900*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SM2014.2
039000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SM2014.2
039100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SM2014.2
039200*        MOVE ZERO TO RECORD-COUNT.                               SM2014.2
039300     PERFORM WRT-LN.                                              SM2014.2
039400 WRT-LN.                                                          SM2014.2
039500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SM2014.2
039600     MOVE SPACE TO DUMMY-RECORD.                                  SM2014.2
039700 BLANK-LINE-PRINT.                                                SM2014.2
039800     PERFORM WRT-LN.                                              SM2014.2
039900 FAIL-ROUTINE.                                                    SM2014.2
040000     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. SM2014.2
040100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.SM2014.2
040200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2014.2
040300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   SM2014.2
040400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2014.2
040500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2014.2
040600     GO TO  FAIL-ROUTINE-EX.                                      SM2014.2
040700 FAIL-ROUTINE-WRITE.                                              SM2014.2
040800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         SM2014.2
040900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 SM2014.2
041000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. SM2014.2
041100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         SM2014.2
041200 FAIL-ROUTINE-EX. EXIT.                                           SM2014.2
041300 BAIL-OUT.                                                        SM2014.2
041400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   SM2014.2
041500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           SM2014.2
041600 BAIL-OUT-WRITE.                                                  SM2014.2
041700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SM2014.2
041800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 SM2014.2
041900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SM2014.2
042000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         SM2014.2
042100 BAIL-OUT-EX. EXIT.                                               SM2014.2
042200 CCVS1-EXIT.                                                      SM2014.2
042300     EXIT.                                                        SM2014.2
042400 INITIALIZATION SECTION.                                          SM2014.2
042500 SM201A-INIT.                                                     SM2014.2
042600     OPEN     OUTPUT TEST-FILE.                                   SM2014.2
042700     MOVE     "OUTPUT OF SM201A IS USED AS" TO RE-MARK.           SM2014.2
042800     PERFORM  PRINT-DETAIL.                                       SM2014.2
042900     MOVE     "INPUT FOR SM202A."           TO RE-MARK.           SM2014.2
043000     PERFORM  PRINT-DETAIL.                                       SM2014.2
043100     MOVE     "COPY 01 LEVEL --- " TO FEATURE.                    SM2014.2
043200     PERFORM  PRINT-DETAIL.                                       SM2014.2
043300 WORKING-STORAGE-TEST SECTION.                                    SM2014.2
043400 COPY-TEST-1.                                                     SM2014.2
043500     IF       WSTR-1A EQUAL TO WSTR999                            SM2014.2
043600              PERFORM PASS GO TO COPY-WRITE-1.                    SM2014.2
043700*        NOTE TESTS COPYING WITH REPLACEMENT IN WORKING-STORAGE.  SM2014.2
043800     GO       TO COPY-FAIL-1.                                     SM2014.2
043900 COPY-DELETE-1.                                                   SM2014.2
044000     PERFORM  DE-LETE.                                            SM2014.2
044100     GO       TO COPY-WRITE-1.                                    SM2014.2
044200 COPY-FAIL-1.                                                     SM2014.2
044300     MOVE     WSTR999 TO COMPUTED-A.                              SM2014.2
044400     MOVE     "ABC" TO CORRECT-A.                                 SM2014.2
044500     PERFORM  FAIL.                                               SM2014.2
044600 COPY-WRITE-1.                                                    SM2014.2
044700     MOVE     "  REPLACING" TO FEATURE.                           SM2014.2
044800     MOVE     "COPY-TEST-1 " TO PAR-NAME.                         SM2014.2
044900     PERFORM  PRINT-DETAIL.                                       SM2014.2
045000 COPY-TEST-2.                                                     SM2014.2
045100     IF       WSTR-2A EQUAL TO "ABC"                              SM2014.2
045200              PERFORM PASS GO TO COPY-WRITE-2.                    SM2014.2
045300*    NOTE     TESTS ORDINARY COPY OF ENTRIES WHICH ARE ALSO COPIEDSM2014.2
045400*             BY COPY REPLACING.                                  SM2014.2
045500     GO       TO COPY-FAIL-2.                                     SM2014.2
045600 COPY-DELETE-2.                                                   SM2014.2
045700     PERFORM  DE-LETE.                                            SM2014.2
045800     GO       TO COPY-WRITE-2.                                    SM2014.2
045900 COPY-FAIL-2.                                                     SM2014.2
046000     MOVE     WSTR-2A TO COMPUTED-A.                              SM2014.2
046100     MOVE     "ABC" TO CORRECT-A.                                 SM2014.2
046200     PERFORM  FAIL.                                               SM2014.2
046300 COPY-WRITE-2.                                                    SM2014.2
046400     MOVE     "  (NO REPLACING)" TO FEATURE.                      SM2014.2
046500     MOVE     "COPY-TEST-2 " TO PAR-NAME.                         SM2014.2
046600     PERFORM  PRINT-DETAIL.                                       SM2014.2
046700 COPY-INIT-A.                                                     SM2014.2
046800     MOVE     "  REPLACING" TO FEATURE.                           SM2014.2
046900 COPY-TEST-3.                                                     SM2014.2
047000     IF       WSTR91 EQUAL TO "ABC"                               SM2014.2
047100              PERFORM PASS GO TO COPY-WRITE-3.                    SM2014.2
047200*        NOTE COPY-TEST-3, 4, AND 5 TEST COPYING WITH A           SM2014.2
047300*             SERIES OF REPLACEMENTS.                             SM2014.2
047400     GO       TO COPY-FAIL-3.                                     SM2014.2
047500 COPY-DELETE-3.                                                   SM2014.2
047600     PERFORM  DE-LETE.                                            SM2014.2
047700     GO       TO COPY-WRITE-3.                                    SM2014.2
047800 COPY-FAIL-3.                                                     SM2014.2
047900     MOVE     WSTR91 TO COMPUTED-A.                               SM2014.2
048000     MOVE     "ABC" TO CORRECT-A.                                 SM2014.2
048100     PERFORM  FAIL.                                               SM2014.2
048200 COPY-WRITE-3.                                                    SM2014.2
048300     MOVE     "COPY-TEST-3 " TO PAR-NAME.                         SM2014.2
048400     PERFORM  PRINT-DETAIL.                                       SM2014.2
048500 COPY-TEST-4.                                                     SM2014.2
048600     IF       WSTR92 EQUAL TO "DEF"                               SM2014.2
048700              PERFORM PASS GO TO COPY-WRITE-4.                    SM2014.2
048800     GO       TO COPY-FAIL-4.                                     SM2014.2
048900 COPY-DELETE-4.                                                   SM2014.2
049000     PERFORM  DE-LETE.                                            SM2014.2
049100     GO       TO COPY-WRITE-4.                                    SM2014.2
049200 COPY-FAIL-4.                                                     SM2014.2
049300     MOVE     WSTR92 TO COMPUTED-A.                               SM2014.2
049400     MOVE     "DEF" TO CORRECT-A.                                 SM2014.2
049500     PERFORM  FAIL.                                               SM2014.2
049600 COPY-WRITE-4.                                                    SM2014.2
049700     MOVE     "COPY-TEST-4 " TO PAR-NAME.                         SM2014.2
049800     PERFORM  PRINT-DETAIL.                                       SM2014.2
049900 COPY-TEST-5.                                                     SM2014.2
050000     IF       WSTR93 EQUAL TO "GHI"                               SM2014.2
050100              PERFORM PASS GO TO COPY-WRITE-5.                    SM2014.2
050200     GO       TO COPY-FAIL-5.                                     SM2014.2
050300 COPY-DELETE-5.                                                   SM2014.2
050400     PERFORM  DE-LETE.                                            SM2014.2
050500     GO       TO COPY-WRITE-5.                                    SM2014.2
050600 COPY-FAIL-5.                                                     SM2014.2
050700     MOVE     WSTR93 TO COMPUTED-A.                               SM2014.2
050800     MOVE     "GHI" TO CORRECT-A.                                 SM2014.2
050900     PERFORM  FAIL.                                               SM2014.2
051000 COPY-WRITE-5.                                                    SM2014.2
051100     MOVE     "COPY-TEST-5 " TO PAR-NAME.                         SM2014.2
051200     PERFORM  PRINT-DETAIL.                                       SM2014.2
051300 COPY-INIT-B.                                                     SM2014.2
051400     MOVE     "  (NOT REPLACING)" TO FEATURE.                     SM2014.2
051500 COPY-TEST-6.                                                     SM2014.2
051600     IF       WSTR4A EQUAL TO "ABC"                               SM2014.2
051700              PERFORM PASS GO TO COPY-WRITE-6.                    SM2014.2
051800*        NOTE COPY-TEST-6, 7, AND 8 TEST ORDINARY COPYING OF      SM2014.2
051900*             ENTRIES WHICH ARE ALSO COPIED WITH REPLACEMENT.     SM2014.2
052000     GO       TO COPY-FAIL-6.                                     SM2014.2
052100 COPY-DELETE-6.                                                   SM2014.2
052200     PERFORM  DE-LETE.                                            SM2014.2
052300     GO       TO COPY-WRITE-6.                                    SM2014.2
052400 COPY-FAIL-6.                                                     SM2014.2
052500     MOVE     WSTR4A TO COMPUTED-A.                               SM2014.2
052600     MOVE     "ABC" TO CORRECT-A.                                 SM2014.2
052700     PERFORM  FAIL.                                               SM2014.2
052800 COPY-WRITE-6.                                                    SM2014.2
052900     MOVE     "COPY-TEST-6 " TO PAR-NAME.                         SM2014.2
053000     PERFORM  PRINT-DETAIL.                                       SM2014.2
053100 COPY-TEST-7.                                                     SM2014.2
053200     IF       WSTR4B EQUAL TO "DEF"                               SM2014.2
053300              PERFORM PASS GO TO COPY-WRITE-7.                    SM2014.2
053400     GO       TO COPY-FAIL-7.                                     SM2014.2
053500 COPY-DELETE-7.                                                   SM2014.2
053600     PERFORM  DE-LETE.                                            SM2014.2
053700     GO       TO COPY-WRITE-7.                                    SM2014.2
053800 COPY-FAIL-7.                                                     SM2014.2
053900     MOVE     WSTR4B TO COMPUTED-A.                               SM2014.2
054000     MOVE     "DEF" TO CORRECT-A.                                 SM2014.2
054100     PERFORM  FAIL.                                               SM2014.2
054200 COPY-WRITE-7.                                                    SM2014.2
054300     MOVE     "COPY-TEST-7 " TO PAR-NAME.                         SM2014.2
054400     PERFORM  PRINT-DETAIL.                                       SM2014.2
054500 COPY-TEST-8.                                                     SM2014.2
054600     IF       WSTR4C EQUAL TO "GHI"                               SM2014.2
054700              PERFORM PASS GO TO COPY-WRITE-8.                    SM2014.2
054800     GO       TO COPY-FAIL-8.                                     SM2014.2
054900 COPY-DELETE-8.                                                   SM2014.2
055000     PERFORM  DE-LETE.                                            SM2014.2
055100     GO       TO COPY-WRITE-8.                                    SM2014.2
055200 COPY-FAIL-8.                                                     SM2014.2
055300     MOVE     WSTR4C TO COMPUTED-A.                               SM2014.2
055400     MOVE     "GHI" TO CORRECT-A.                                 SM2014.2
055500     PERFORM  FAIL.                                               SM2014.2
055600 COPY-WRITE-8.                                                    SM2014.2
055700     MOVE     "COPY-TEST-8 " TO PAR-NAME.                         SM2014.2
055800     PERFORM  PRINT-DETAIL.                                       SM2014.2
055900 PARAGRAPH-TEST SECTION.                                          SM2014.2
056000 COPY-TEST-9.                                                     SM2014.2
056100                                                                  SM2014.2
056200                                                                  SM2014.2
056300                                                                  SM2014.2
056400                                                                  SM2014.2
056500                                                                  SM2014.2
056600*                                                                 SM2014.2
056700*********************** COPY STATEMENT USED **********************SM2014.2
056800*                                                                 SM2014.2
056900*                                                      COPY K1PRB SM2014.2
057000*             REPLACING WSTR4C BY WSTR4B.                         SM2014.2
057100*                                                                 SM2014.2
057200******************** COPIED TEXT BEGINS BELOW ********************SM2014.2
057300                                                       COPY K1PRB SM2014.2
057400              REPLACING WSTR4C BY WSTR4B.                         SM2014.2
057500*********************** END OF COPIED TEXT ***********************SM2014.2
057600*    NOTE     COPY A PROCEDURE WHICH REFERENCES COPIED DATA.      SM2014.2
057700     IF       WSTR-4 EQUAL TO "DEFABCDEF"                         SM2014.2
057800              PERFORM PASS GO TO COPY-WRITE-9.                    SM2014.2
057900     GO       TO COPY-FAIL-9.                                     SM2014.2
058000 COPY-DELETE-9.                                                   SM2014.2
058100     PERFORM  DE-LETE.                                            SM2014.2
058200     GO       TO COPY-WRITE-9.                                    SM2014.2
058300 COPY-FAIL-9.                                                     SM2014.2
058400     MOVE     WSTR-4 TO COMPUTED-A.                               SM2014.2
058500     MOVE     "DEFABCDEF" TO CORRECT-A.                           SM2014.2
058600     PERFORM  FAIL.                                               SM2014.2
058700 COPY-WRITE-9.                                                    SM2014.2
058800     MOVE     "COPY PARA REPLACING" TO FEATURE.                   SM2014.2
058900     MOVE     "COPY-TEST-9 " TO PAR-NAME.                         SM2014.2
059000     PERFORM  PRINT-DETAIL.                                       SM2014.2
059100 BUILD SECTION.                                                   SM2014.2
059200 COPY-TEST-10.                                                    SM2014.2
059300     MOVE     RCD-1 TO TF-1.                                      SM2014.2
059400     WRITE    TST-TEST.                                           SM2014.2
059500     MOVE     RCD-2 TO TF-1.                                      SM2014.2
059600     WRITE    TST-TEST.                                           SM2014.2
059700     MOVE     RCD-3 TO TF-1.                                      SM2014.2
059800     WRITE    TST-TEST.                                           SM2014.2
059900     MOVE     RCD-4 TO TF-1.                                      SM2014.2
060000     WRITE    TST-TEST.                                           SM2014.2
060100     MOVE     RCD-5 TO TF-1.                                      SM2014.2
060200     WRITE    TST-TEST.                                           SM2014.2
060300     MOVE     RCD-6 TO TF-1.                                      SM2014.2
060400     WRITE    TST-TEST.                                           SM2014.2
060500     MOVE     RCD-7 TO TF-1.                                      SM2014.2
060600     WRITE    TST-TEST.                                           SM2014.2
060700     PERFORM  PASS.                                               SM2014.2
060800     GO       TO COPY-WRITE-10.                                   SM2014.2
060900 COPY-DELETE-10.                                                  SM2014.2
061000     PERFORM  DE-LETE.                                            SM2014.2
061100 COPY-WRITE-10.                                                   SM2014.2
061200     MOVE     "COPY FD REPLACING" TO FEATURE.                     SM2014.2
061300     MOVE     "COPY-TEST-10 " TO PAR-NAME.                        SM2014.2
061400     MOVE     "OUTPUT PASSED ONTO SM202" TO RE-MARK.              SM2014.2
061500     PERFORM  PRINT-DETAIL.                                       SM2014.2
061600     CLOSE    TEST-FILE.                                          SM2014.2
061700 MORE-TESTS SECTION.                                              SM2014.2
061800 COPY-TEST-11.                                                    SM2014.2
061900     MOVE SPACES TO TEXT-TEST-1.                                  SM2014.2
062000     MOVE 12345 TO TXT-FLD-1.                                     SM2014.2
062100     IF TEXT-TEST-1 IS EQUAL TO "                                 SM2014.2
062200-    "                                                            SM2014.2
062300-    "                      12345"                                SM2014.2
062400         PERFORM PASS  ELSE  PERFORM FAIL.                        SM2014.2
062500     GO TO COPY-WRITE-11.                                         SM2014.2
062600 COPY-DELETE-11.                                                  SM2014.2
062700     PERFORM DE-LETE.                                             SM2014.2
062800 COPY-WRITE-11.                                                   SM2014.2
062900     MOVE "PSEUDO TEXT" TO FEATURE.                               SM2014.2
063000     MOVE "COPY-TEST-11" TO PAR-NAME.                             SM2014.2
063100     PERFORM PRINT-DETAIL.                                        SM2014.2
063200 CCVS-EXIT SECTION.                                               SM2014.2
063300 CCVS-999999.                                                     SM2014.2
063400     GO TO CLOSE-FILES.                                           SM2014.2
      *END-OF,SM201A                                                                  
