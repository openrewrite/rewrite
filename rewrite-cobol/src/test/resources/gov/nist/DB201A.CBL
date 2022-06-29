      *HEADER,COBOL,DB201A                                                            
000100 IDENTIFICATION DIVISION.                                         DB2014.2
000200 PROGRAM-ID.                                                      DB2014.2
000300     DB201A.                                                      DB2014.2
000400 AUTHOR.                                                          DB2014.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB2014.2
000600 INSTALLATION.                                                    DB2014.2
000700     GENERAL SERVICES ADMINISTRATION                              DB2014.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB2014.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB2014.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB2014.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB2014.2
001200                                                                  DB2014.2
001300     PHONE   (703) 756-6153                                       DB2014.2
001400                                                                  DB2014.2
001500     " HIGH       ".                                              DB2014.2
001600 DATE-WRITTEN.                                                    DB2014.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB2014.2
001800     CREATION DATE     /    VALIDATION DATE                       DB2014.2
001900     "4.2 ".                                                      DB2014.2
002000 SECURITY.                                                        DB2014.2
002100     NONE.                                                        DB2014.2
002200*                                                                 DB2014.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB2014.2
002400*                                                                 DB2014.2
002500*                       PROGRAM ABSTRACT                          DB2014.2
002600*                                                                 DB2014.2
002700*    DB201A TESTS THE CAPABILITY OF THE DEBUG MODULE TO HANDLE    DB2014.2
002800*    DEBUGGING PROCEDURES WHICH ARE MONITORING IDENTIFIERS        DB2014.2
002900*    SPECIFIED WITH AND WITHOUT THE "ALL REFERENCES" OPTION.      DB2014.2
003000*    THIS PROGRAM IS TO BE COMPILED AND EXECUTED WITH BOTH        DB2014.2
003100*    COMPILE AND OBJECT TIME DEBUGGING SWITCHES ENABLED.  THE     DB2014.2
003200*    DEBUGGING PROCEDURES SHOULD BE INCLUDED IN COMPILATION       DB2014.2
003300*    AND GENERATE CODE.  DEBUGGING ACTIONS ON THE FOLLOWING       DB2014.2
003400*    CONDITIONS ARE ANALYZED                                      DB2014.2
003500*                                                                 DB2014.2
003600*        (1)  REFERENCE TO IDENTIFIER WITHIN "VARYING", "AFTER",  DB2014.2
003700*             AND "UNTIL" PHRASES OF "PERFORM" STATEMENTS.        DB2014.2
003800*        (2)  REFERENCE TO CHANGED AND UNCHANGED IDENTIFIER       DB2014.2
003900*             FIELDS.                                             DB2014.2
004000*        (3)  REFERENCE TO SUBSCRIPTED IDENTIFIERS.               DB2014.2
004100*        (4)  REFERENCE TO QUALIFIED IDENTIFIERS.                 DB2014.2
004200*        (5)  REFERENCE TO IDENTIFIER USED IN "GO TO DEPENDING".  DB2014.2
004300*        (6)  REFERENCE TO IDENTIFIER IN UNEXECUTED STATEMENTS.   DB2014.2
004400*        (7)  MULTIPLE REFERENCES TO SAME IDENTIFIER IN SAME      DB2014.2
004500*             STATEMENT.                                          DB2014.2
004600*                                                                 DB2014.2
004700*                                                                 DB2014.2
004800*                                                                 DB2014.2
004900 ENVIRONMENT DIVISION.                                            DB2014.2
005000 CONFIGURATION SECTION.                                           DB2014.2
005100 SOURCE-COMPUTER.                                                 DB2014.2
005200     XXXXX082                                                     DB2014.2
005300         WITH DEBUGGING MODE.                                     DB2014.2
005400 OBJECT-COMPUTER.                                                 DB2014.2
005500     XXXXX083.                                                    DB2014.2
005600 INPUT-OUTPUT SECTION.                                            DB2014.2
005700 FILE-CONTROL.                                                    DB2014.2
005800     SELECT PRINT-FILE ASSIGN TO                                  DB2014.2
005900     XXXXX055.                                                    DB2014.2
006000 DATA DIVISION.                                                   DB2014.2
006100 FILE SECTION.                                                    DB2014.2
006200 FD  PRINT-FILE                                                   DB2014.2
006300     LABEL RECORDS                                                DB2014.2
006400     XXXXX084                                                     DB2014.2
006500     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB2014.2
006600 01  PRINT-REC PICTURE X(120).                                    DB2014.2
006700 01  DUMMY-RECORD PICTURE X(120).                                 DB2014.2
006800 WORKING-STORAGE SECTION.                                         DB2014.2
006900 77  COUNTER PIC 999 VALUE 0.                                     DB2014.2
007000 77  GO-TO-DEP-KEY PIC 9.                                         DB2014.2
007100 77  GO-TO-DEP-KEY-1 PIC 9.                                       DB2014.2
007200 01  ITEM-1.                                                      DB2014.2
007300     02  KEY-1 PIC 99   VALUE ZERO.                               DB2014.2
007400     02  LINE-1 PIC X(6).                                         DB2014.2
007500     02  NAME-1 PIC X(30).                                        DB2014.2
007600     02  UNQUAL-NAME-1 PIC X(30).                                 DB2014.2
007700     02  SUB-1-1 PIC X(5).                                        DB2014.2
007800     02  SUB-2-1 PIC X(5).                                        DB2014.2
007900     02  SUB-3-1 PIC X(5).                                        DB2014.2
008000     02  CONTENTS-1 PIC X(30).                                    DB2014.2
008100 01  ITEM-2.                                                      DB2014.2
008200     02  KEY-2 PIC 99.                                            DB2014.2
008300     02  LINE-2 PIC X(6).                                         DB2014.2
008400     02  NAME-2 PIC X(30).                                        DB2014.2
008500     02  UNQUAL-NAME-2 PIC X(30).                                 DB2014.2
008600     02  CONTENTS-2 PIC X(30).                                    DB2014.2
008700 01  ID-1 PIC 99.                                                 DB2014.2
008800 01  ID-1A REDEFINES ID-1 PIC 99.                                 DB2014.2
008900 01  ID-2 PIC 99.                                                 DB2014.2
009000 01  ID-2A REDEFINES ID-2 PIC 99.                                 DB2014.2
009100 01  ID-3 PIC 99.                                                 DB2014.2
009200 01  A-GROUP.                                                     DB2014.2
009300     02  A1.                                                      DB2014.2
009400         03  AB1 PIC X OCCURS 5 TIMES.                            DB2014.2
009500         03  AB2.                                                 DB2014.2
009600             04  ABC1 PIC X.                                      DB2014.2
009700             04  ABC2 PIC X.                                      DB2014.2
009800     02  A2.                                                      DB2014.2
009900         03  AB1 PIC X OCCURS 5 TIMES.                            DB2014.2
010000         03  AB2.                                                 DB2014.2
010100             04  ABC1 PIC X.                                      DB2014.2
010200             04  ABC2 PIC X.                                      DB2014.2
010300 01  B-GROUP.                                                     DB2014.2
010400     02  B-LEVEL-1 OCCURS 10 TIMES INDEXED BY I.                  DB2014.2
010500         03  B-LEVEL-2 OCCURS 10 TIMES INDEXED BY J.              DB2014.2
010600             04  B-LEVEL-3 OCCURS 10 TIMES INDEXED BY K PIC X.    DB2014.2
010700 01  TEST-RESULTS.                                                DB2014.2
010800     02 FILLER                    PICTURE X VALUE SPACE.          DB2014.2
010900     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB2014.2
011000     02 FILLER                    PICTURE X VALUE SPACE.          DB2014.2
011100     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB2014.2
011200     02 FILLER                    PICTURE X  VALUE SPACE.         DB2014.2
011300     02  PAR-NAME.                                                DB2014.2
011400       03 FILLER PICTURE X(12) VALUE SPACE.                       DB2014.2
011500       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB2014.2
011600       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB2014.2
011700       03 FILLER PIC X(5) VALUE SPACE.                            DB2014.2
011800     02 FILLER PIC X(10) VALUE SPACE.                             DB2014.2
011900     02 RE-MARK PIC X(61).                                        DB2014.2
012000 01  TEST-COMPUTED.                                               DB2014.2
012100     02 FILLER PIC X(30) VALUE SPACE.                             DB2014.2
012200     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB2014.2
012300     02 COMPUTED-X.                                               DB2014.2
012400     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB2014.2
012500     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB2014.2
012600     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB2014.2
012700     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB2014.2
012800     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB2014.2
012900     03       CM-18V0 REDEFINES COMPUTED-A.                       DB2014.2
013000         04 COMPUTED-18V0                   PICTURE -9(18).       DB2014.2
013100         04 FILLER                          PICTURE X.            DB2014.2
013200     03 FILLER PIC X(50) VALUE SPACE.                             DB2014.2
013300 01  TEST-CORRECT.                                                DB2014.2
013400     02 FILLER PIC X(30) VALUE SPACE.                             DB2014.2
013500     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB2014.2
013600     02 CORRECT-X.                                                DB2014.2
013700     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB2014.2
013800     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB2014.2
013900     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB2014.2
014000     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB2014.2
014100     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB2014.2
014200     03      CR-18V0 REDEFINES CORRECT-A.                         DB2014.2
014300         04 CORRECT-18V0                    PICTURE -9(18).       DB2014.2
014400         04 FILLER                          PICTURE X.            DB2014.2
014500     03 FILLER PIC X(50) VALUE SPACE.                             DB2014.2
014600 01  CCVS-C-1.                                                    DB2014.2
014700     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB2014.2
014800-    "SS  PARAGRAPH-NAME                                          DB2014.2
014900-    "        REMARKS".                                           DB2014.2
015000     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB2014.2
015100 01  CCVS-C-2.                                                    DB2014.2
015200     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2014.2
015300     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB2014.2
015400     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB2014.2
015500     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB2014.2
015600     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB2014.2
015700 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB2014.2
015800 01  REC-CT PICTURE 99 VALUE ZERO.                                DB2014.2
015900 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB2014.2
016000 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB2014.2
016100 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB2014.2
016200 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB2014.2
016300 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB2014.2
016400 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB2014.2
016500 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB2014.2
016600 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB2014.2
016700 01  CCVS-H-1.                                                    DB2014.2
016800     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB2014.2
016900     02 FILLER PICTURE X(67) VALUE                                DB2014.2
017000     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB2014.2
017100-    " SYSTEM".                                                   DB2014.2
017200     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB2014.2
017300 01  CCVS-H-2.                                                    DB2014.2
017400     02 FILLER PICTURE X(52) VALUE IS                             DB2014.2
017500     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB2014.2
017600     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB2014.2
017700     02 TEST-ID PICTURE IS X(9).                                  DB2014.2
017800     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB2014.2
017900 01  CCVS-H-3.                                                    DB2014.2
018000     02  FILLER PICTURE X(34) VALUE                               DB2014.2
018100     " FOR OFFICIAL USE ONLY    ".                                DB2014.2
018200     02  FILLER PICTURE X(58) VALUE                               DB2014.2
018300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB2014.2
018400     02  FILLER PICTURE X(28) VALUE                               DB2014.2
018500     "  COPYRIGHT   1974 ".                                       DB2014.2
018600 01  CCVS-E-1.                                                    DB2014.2
018700     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB2014.2
018800     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB2014.2
018900     02 ID-AGAIN PICTURE IS X(9).                                 DB2014.2
019000     02 FILLER PICTURE X(45) VALUE IS                             DB2014.2
019100     " NTIS DISTRIBUTION COBOL 74".                               DB2014.2
019200 01  CCVS-E-2.                                                    DB2014.2
019300     02  FILLER                   PICTURE X(31)  VALUE            DB2014.2
019400     SPACE.                                                       DB2014.2
019500     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB2014.2
019600     02 CCVS-E-2-2.                                               DB2014.2
019700         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB2014.2
019800         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB2014.2
019900         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB2014.2
020000 01  CCVS-E-3.                                                    DB2014.2
020100     02  FILLER PICTURE X(22) VALUE                               DB2014.2
020200     " FOR OFFICIAL USE ONLY".                                    DB2014.2
020300     02  FILLER PICTURE X(12) VALUE SPACE.                        DB2014.2
020400     02  FILLER PICTURE X(58) VALUE                               DB2014.2
020500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB2014.2
020600     02  FILLER PICTURE X(13) VALUE SPACE.                        DB2014.2
020700     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB2014.2
020800 01  CCVS-E-4.                                                    DB2014.2
020900     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB2014.2
021000     02 FILLER PIC XXXX VALUE " OF ".                             DB2014.2
021100     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB2014.2
021200     02 FILLER PIC X(40) VALUE                                    DB2014.2
021300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB2014.2
021400 01  XXINFO.                                                      DB2014.2
021500     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB2014.2
021600     02 INFO-TEXT.                                                DB2014.2
021700     04 FILLER PIC X(20) VALUE SPACE.                             DB2014.2
021800     04 XXCOMPUTED PIC X(20).                                     DB2014.2
021900     04 FILLER PIC X(5) VALUE SPACE.                              DB2014.2
022000     04 XXCORRECT PIC X(20).                                      DB2014.2
022100 01  HYPHEN-LINE.                                                 DB2014.2
022200     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2014.2
022300     02 FILLER PICTURE IS X(65) VALUE IS "************************DB2014.2
022400-    "*****************************************".                 DB2014.2
022500     02 FILLER PICTURE IS X(54) VALUE IS "************************DB2014.2
022600-    "******************************".                            DB2014.2
022700 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB2014.2
022800     "DB201A".                                                    DB2014.2
022900 PROCEDURE DIVISION.                                              DB2014.2
023000 DECLARATIVES.                                                    DB2014.2
023100 GO-TO-DEPENDING-PROC-1 SECTION.                                  DB2014.2
023200     USE FOR DEBUGGING ON  ALL REFERENCES OF GO-TO-DEP-KEY        DB2014.2
023300                           ALL               ID-2.                DB2014.2
023400 GO-TO-DEPEND-1.                                                  DB2014.2
023500     ADD 1 TO KEY-1.                                              DB2014.2
023600 DB-COMMON-1.                                                     DB2014.2
023700     MOVE DEBUG-LINE TO LINE-1.                                   DB2014.2
023800     MOVE DEBUG-NAME TO NAME-1  UNQUAL-NAME-1.                    DB2014.2
023900     MOVE DEBUG-CONTENTS TO CONTENTS-1.                           DB2014.2
024000 DB-CLEAR-QUALIFIER-1.                                            DB2014.2
024100     INSPECT UNQUAL-NAME-1 REPLACING CHARACTERS BY SPACES         DB2014.2
024200         AFTER INITIAL SPACE.                                     DB2014.2
024300 GO-TO-DEPENDING-PROC-2 SECTION.                                  DB2014.2
024400     USE FOR DEBUGGING ON G-T-D-2.                                DB2014.2
024500 GO-TO-DEPEND-2.                                                  DB2014.2
024600     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
024700         MOVE 2 TO KEY-2                                          DB2014.2
024800         ELSE MOVE 1 TO KEY-2.                                    DB2014.2
024900 DB-COMMON-2.                                                     DB2014.2
025000     MOVE DEBUG-LINE TO LINE-2.                                   DB2014.2
025100     MOVE DEBUG-NAME TO NAME-2  UNQUAL-NAME-2.                    DB2014.2
025200     MOVE DEBUG-CONTENTS TO CONTENTS-2.                           DB2014.2
025300 DB-CLEAR-QUALIFIER-2.                                            DB2014.2
025400     INSPECT UNQUAL-NAME-2 REPLACING CHARACTERS BY SPACES         DB2014.2
025500         AFTER INITIAL SPACE.                                     DB2014.2
025600 GO-TO-DEPENDING-PROC-3 SECTION.                                  DB2014.2
025700     USE FOR DEBUGGING ON GO-TO-DEP-KEY-1.                        DB2014.2
025800 GO-TO-DEPEND-3.                                                  DB2014.2
025900     MOVE 1 TO KEY-1.                                             DB2014.2
026000 PERFORM-PROC-1 SECTION.                                          DB2014.2
026100     USE FOR DEBUGGING ON ID-1.                                   DB2014.2
026200 PERFORM-1.                                                       DB2014.2
026300     ADD 1 TO KEY-1.                                              DB2014.2
026400     PERFORM DB-COMMON-1.                                         DB2014.2
026500     PERFORM DB-CLEAR-QUALIFIER-1.                                DB2014.2
026600 TABLE-PROC-1 SECTION.                                            DB2014.2
026700     USE FOR DEBUGGING ON  B-LEVEL-1  B-LEVEL-2  B-LEVEL-3.       DB2014.2
026800 TABLE-1.                                                         DB2014.2
026900     MOVE 1 TO KEY-1.                                             DB2014.2
027000     PERFORM DB-COMMON-1.                                         DB2014.2
027100     PERFORM DB-CLEAR-QUALIFIER-1.                                DB2014.2
027200 DB-MOVE-SUBSC-1.                                                 DB2014.2
027300     MOVE DEBUG-SUB-1 TO SUB-1-1.                                 DB2014.2
027400     IF PAR-NAME = "SUBSC-TEST-2"                                 DB2014.2
027500          MOVE DEBUG-SUB-2 TO SUB-2-1.                            DB2014.2
027600     MOVE DEBUG-SUB-3 TO SUB-3-1.                                 DB2014.2
027700 QUAL-PROC-1 SECTION.                                             DB2014.2
027800     USE FOR DEBUGGING ON ALL REFERENCES OF ABC1 OF AB2 OF A1     DB2014.2
027900                          ALL                       AB2 OF A2.    DB2014.2
028000 QUAL-1.                                                          DB2014.2
028100     MOVE 1 TO KEY-1.                                             DB2014.2
028200     PERFORM DB-COMMON-1.                                         DB2014.2
028300 QUAL-SUBC-PROC-1 SECTION.                                        DB2014.2
028400     USE FOR DEBUGGING ON ALL REFERENCES OF AB1 OF A1.            DB2014.2
028500 QUAL-SUBC-1.                                                     DB2014.2
028600     MOVE 1 TO KEY-1.                                             DB2014.2
028700     PERFORM DB-COMMON-1.                                         DB2014.2
028800     MOVE DEBUG-SUB-1 TO SUB-1-1.                                 DB2014.2
028900 END DECLARATIVES.                                                DB2014.2
029000 CCVS1 SECTION.                                                   DB2014.2
029100 OPEN-FILES.                                                      DB2014.2
029200     OPEN     OUTPUT PRINT-FILE.                                  DB2014.2
029300     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB2014.2
029400     MOVE    SPACE TO TEST-RESULTS.                               DB2014.2
029500     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB2014.2
029600     GO TO CCVS1-EXIT.                                            DB2014.2
029700 CLOSE-FILES.                                                     DB2014.2
029800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB2014.2
029900 TERMINATE-CCVS.                                                  DB2014.2
030000*    EXIT PROGRAM.                                                DB2014.2
030100*TERMINATE-CALL.                                                  DB2014.2
030200     STOP     RUN.                                                DB2014.2
030300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB2014.2
030400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB2014.2
030500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB2014.2
030600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB2014.2
030700     MOVE "****TEST DELETED****" TO RE-MARK.                      DB2014.2
030800 PRINT-DETAIL.                                                    DB2014.2
030900     IF REC-CT NOT EQUAL TO ZERO                                  DB2014.2
031000             MOVE "." TO PARDOT-X                                 DB2014.2
031100             MOVE REC-CT TO DOTVALUE.                             DB2014.2
031200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB2014.2
031300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB2014.2
031400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB2014.2
031500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB2014.2
031600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB2014.2
031700     MOVE SPACE TO CORRECT-X.                                     DB2014.2
031800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB2014.2
031900     MOVE     SPACE TO RE-MARK.                                   DB2014.2
032000 HEAD-ROUTINE.                                                    DB2014.2
032100     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2014.2
032200     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB2014.2
032300     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB2014.2
032400 COLUMN-NAMES-ROUTINE.                                            DB2014.2
032500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2014.2
032600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2014.2
032700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB2014.2
032800 END-ROUTINE.                                                     DB2014.2
032900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB2014.2
033000 END-RTN-EXIT.                                                    DB2014.2
033100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2014.2
033200 END-ROUTINE-1.                                                   DB2014.2
033300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB2014.2
033400      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB2014.2
033500      ADD PASS-COUNTER TO ERROR-HOLD.                             DB2014.2
033600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB2014.2
033700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB2014.2
033800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB2014.2
033900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB2014.2
034000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB2014.2
034100  END-ROUTINE-12.                                                 DB2014.2
034200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB2014.2
034300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB2014.2
034400         MOVE "NO " TO ERROR-TOTAL                                DB2014.2
034500         ELSE                                                     DB2014.2
034600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB2014.2
034700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB2014.2
034800     PERFORM WRITE-LINE.                                          DB2014.2
034900 END-ROUTINE-13.                                                  DB2014.2
035000     IF DELETE-CNT IS EQUAL TO ZERO                               DB2014.2
035100         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB2014.2
035200         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB2014.2
035300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB2014.2
035400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2014.2
035500      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB2014.2
035600          MOVE "NO " TO ERROR-TOTAL                               DB2014.2
035700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB2014.2
035800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB2014.2
035900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB2014.2
036000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2014.2
036100 WRITE-LINE.                                                      DB2014.2
036200     ADD 1 TO RECORD-COUNT.                                       DB2014.2
036300*    IF RECORD-COUNT GREATER 50                                   DB2014.2
036400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB2014.2
036500*        MOVE SPACE TO DUMMY-RECORD                               DB2014.2
036600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB2014.2
036700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB2014.2
036800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB2014.2
036900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB2014.2
037000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB2014.2
037100*        MOVE ZERO TO RECORD-COUNT.                               DB2014.2
037200     PERFORM WRT-LN.                                              DB2014.2
037300 WRT-LN.                                                          DB2014.2
037400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB2014.2
037500     MOVE SPACE TO DUMMY-RECORD.                                  DB2014.2
037600 BLANK-LINE-PRINT.                                                DB2014.2
037700     PERFORM WRT-LN.                                              DB2014.2
037800 FAIL-ROUTINE.                                                    DB2014.2
037900     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB2014.2
038000     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB2014.2
038100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB2014.2
038200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2014.2
038300     GO TO FAIL-ROUTINE-EX.                                       DB2014.2
038400 FAIL-ROUTINE-WRITE.                                              DB2014.2
038500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB2014.2
038600     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB2014.2
038700 FAIL-ROUTINE-EX. EXIT.                                           DB2014.2
038800 BAIL-OUT.                                                        DB2014.2
038900     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB2014.2
039000     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB2014.2
039100 BAIL-OUT-WRITE.                                                  DB2014.2
039200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB2014.2
039300     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2014.2
039400 BAIL-OUT-EX. EXIT.                                               DB2014.2
039500 CCVS1-EXIT.                                                      DB2014.2
039600     EXIT.                                                        DB2014.2
039700 SECT-DB201A-0001 SECTION.                                        DB2014.2
039800 GO-TO-DEPENDING-INIT.                                            DB2014.2
039900     MOVE "GO TO DEP/ALL REF" TO FEATURE.                         DB2014.2
040000     MOVE "GO-TO-DEPENDING" TO PAR-NAME.                          DB2014.2
040100 GO-TO-DEPENDING-0.                                               DB2014.2
040200     MOVE 2 TO GO-TO-DEP-KEY.                                     DB2014.2
040300     MOVE SPACES TO ITEM-1  ITEM-2.                               DB2014.2
040400     MOVE 0 TO KEY-1  KEY-2.                                      DB2014.2
040500******************************************************************DB2014.2
040600*    THE DEBUG-LINE (INSPT) TESTS NAMED IN THE OUTPUT REPORT AS  *DB2014.2
040700*    "G-T-D-2A" AND "G-T-D-3A" SHOULD EACH POINT TO THE          *DB2014.2
040800*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB2014.2
040900*    WHICH READS, "GO TO G-T-D-1 G-T-D-2 G-T-D-5 DEPENDING ON    *DB2014.2
041000*    GO-TO-DEP-KEY.".                                            *DB2014.2
041100******************************************************************DB2014.2
041200     GO TO G-T-D-1  G-T-D-2  G-T-D-5  DEPENDING ON GO-TO-DEP-KEY. DB2014.2
041300 GO-TO-DEPENDING-DELETE.                                          DB2014.2
041400     GO TO G-T-D-2-DELETE.                                        DB2014.2
041500 G-T-D-1.                                                         DB2014.2
041600     GO TO G-T-D-2-DELETE.                                        DB2014.2
041700 G-T-D-2.                                                         DB2014.2
041800     MOVE "G-T-D-2" TO PAR-NAME.                                  DB2014.2
041900     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
042000         MOVE "DEBUG EXECUTED ON IDENTIFR" TO RE-MARK             DB2014.2
042100         PERFORM PASS                                             DB2014.2
042200         PERFORM G-T-D-WRITE                                      DB2014.2
042300         GO TO G-T-D-2A                                           DB2014.2
042400     ELSE PERFORM FAIL                                            DB2014.2
042500         MOVE "DEBUG NOT EXECUTED ON IDENTIFR" TO RE-MARK         DB2014.2
042600         PERFORM G-T-D-WRITE                                      DB2014.2
042700         PERFORM DELETE-SUBTESTS-2                                DB2014.2
042800         GO TO G-T-D-3.                                           DB2014.2
042900 G-T-D-2-DELETE.                                                  DB2014.2
043000     PERFORM DE-LETE.                                             DB2014.2
043100     PERFORM G-T-D-WRITE.                                         DB2014.2
043200     PERFORM DELETE-SUBTESTS-2.                                   DB2014.2
043300     GO TO G-T-D-3-DELETE.                                        DB2014.2
043400 DELETE-SUBTESTS-2.                                               DB2014.2
043500     MOVE "G-T-D-2A" TO PAR-NAME.                                 DB2014.2
043600     PERFORM DE-LETE.                                             DB2014.2
043700     PERFORM G-T-D-WRITE.                                         DB2014.2
043800     MOVE "G-T-D-2B" TO PAR-NAME.                                 DB2014.2
043900     PERFORM DE-LETE.                                             DB2014.2
044000     PERFORM G-T-D-WRITE.                                         DB2014.2
044100     MOVE "G-T-D-2C" TO PAR-NAME.                                 DB2014.2
044200     PERFORM DE-LETE.                                             DB2014.2
044300     PERFORM G-T-D-WRITE.                                         DB2014.2
044400 G-T-D-2A.                                                        DB2014.2
044500     MOVE "G-T-D-2A" TO PAR-NAME.                                 DB2014.2
044600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
044700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
044800     MOVE LINE-1 TO COMPUTED-A.                                   DB2014.2
044900     PERFORM INSPT.                                               DB2014.2
045000     PERFORM G-T-D-WRITE.                                         DB2014.2
045100 G-T-D-2B.                                                        DB2014.2
045200     MOVE "G-T-D-2B" TO PAR-NAME.                                 DB2014.2
045300     IF UNQUAL-NAME-1 IS EQUAL TO "GO-TO-DEP-KEY"                 DB2014.2
045400         PERFORM PASS                                             DB2014.2
045500     ELSE PERFORM FAIL                                            DB2014.2
045600         MOVE "GO-TO-DEP-KEY" TO CORRECT-A                        DB2014.2
045700         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
045800     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
045900     PERFORM G-T-D-WRITE.                                         DB2014.2
046000 G-T-D-2C.                                                        DB2014.2
046100     MOVE "G-T-D-2C" TO PAR-NAME.                                 DB2014.2
046200     IF CONTENTS-1 IS EQUAL TO "2"                                DB2014.2
046300         PERFORM PASS                                             DB2014.2
046400     ELSE PERFORM FAIL                                            DB2014.2
046500         MOVE "2" TO CORRECT-A                                    DB2014.2
046600         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
046700     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
046800     PERFORM G-T-D-WRITE.                                         DB2014.2
046900 G-T-D-3.                                                         DB2014.2
047000     MOVE "G-T-D-3" TO PAR-NAME.                                  DB2014.2
047100     IF KEY-2 IS EQUAL TO 1 OR 2                                  DB2014.2
047200         MOVE "DEBUG ON PROC-NAME EXECUTED" TO RE-MARK            DB2014.2
047300         PERFORM PASS                                             DB2014.2
047400         PERFORM G-T-D-WRITE                                      DB2014.2
047500         GO TO G-T-D-3A                                           DB2014.2
047600     ELSE                                                         DB2014.2
047700         MOVE "DEBUG ON PRC-NAME NOT EXECUTED" TO RE-MARK         DB2014.2
047800         PERFORM FAIL                                             DB2014.2
047900         PERFORM G-T-D-WRITE                                      DB2014.2
048000         PERFORM DELETE-SUBTESTS-3                                DB2014.2
048100         GO TO G-T-D-4.                                           DB2014.2
048200 G-T-D-3-DELETE.                                                  DB2014.2
048300     MOVE "G-T-D-3" TO PAR-NAME.                                  DB2014.2
048400     PERFORM DE-LETE.                                             DB2014.2
048500     PERFORM G-T-D-WRITE.                                         DB2014.2
048600     PERFORM DELETE-SUBTESTS-3                                    DB2014.2
048700     GO TO G-T-D-4-DELETE.                                        DB2014.2
048800 DELETE-SUBTESTS-3.                                               DB2014.2
048900     MOVE "G-T-D-3A" TO PAR-NAME.                                 DB2014.2
049000     PERFORM DE-LETE.                                             DB2014.2
049100     PERFORM G-T-D-WRITE.                                         DB2014.2
049200     MOVE "G-T-D-3B" TO PAR-NAME.                                 DB2014.2
049300     PERFORM DE-LETE.                                             DB2014.2
049400     PERFORM G-T-D-WRITE.                                         DB2014.2
049500     MOVE "G-T-D-3C" TO PAR-NAME.                                 DB2014.2
049600     PERFORM DE-LETE.                                             DB2014.2
049700     PERFORM G-T-D-WRITE.                                         DB2014.2
049800 G-T-D-3A.                                                        DB2014.2
049900     MOVE "G-T-D-3A" TO PAR-NAME.                                 DB2014.2
050000     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
050100     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
050200     MOVE LINE-2 TO COMPUTED-A.                                   DB2014.2
050300     PERFORM INSPT.                                               DB2014.2
050400     PERFORM G-T-D-WRITE.                                         DB2014.2
050500 G-T-D-3B.                                                        DB2014.2
050600     MOVE "G-T-D-3B" TO PAR-NAME.                                 DB2014.2
050700     IF UNQUAL-NAME-2 IS EQUAL TO "G-T-D-2"                       DB2014.2
050800         PERFORM PASS                                             DB2014.2
050900     ELSE PERFORM FAIL                                            DB2014.2
051000         MOVE "G-T-D-2" TO CORRECT-A                              DB2014.2
051100         MOVE NAME-2 TO COMPUTED-A.                               DB2014.2
051200     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
051300     PERFORM G-T-D-WRITE.                                         DB2014.2
051400 G-T-D-3C.                                                        DB2014.2
051500     MOVE "G-T-D-3C" TO PAR-NAME.                                 DB2014.2
051600     IF CONTENTS-2 IS EQUAL TO SPACES                             DB2014.2
051700         PERFORM PASS                                             DB2014.2
051800     ELSE PERFORM FAIL                                            DB2014.2
051900         MOVE "(SPACES)" TO CORRECT-A                             DB2014.2
052000         MOVE CONTENTS-2 TO COMPUTED-A.                           DB2014.2
052100     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
052200     PERFORM G-T-D-WRITE.                                         DB2014.2
052300 G-T-D-4.                                                         DB2014.2
052400     MOVE "G-T-D-4" TO PAR-NAME.                                  DB2014.2
052500     IF KEY-2 IS EQUAL TO 2                                       DB2014.2
052600         MOVE "PROCS EXECUTED IN RIGHT ORDER" TO RE-MARK          DB2014.2
052700         PERFORM PASS                                             DB2014.2
052800     ELSE PERFORM FAIL                                            DB2014.2
052900         MOVE "PROCS EXECUTED IN WRONG ORDER" TO RE-MARK.         DB2014.2
053000     GO TO G-T-D-WRITE.                                           DB2014.2
053100 G-T-D-4-DELETE.                                                  DB2014.2
053200     MOVE "G-T-D-4" TO PAR-NAME.                                  DB2014.2
053300     PERFORM DE-LETE.                                             DB2014.2
053400     GO TO G-T-D-WRITE.                                           DB2014.2
053500 G-T-D-5.                                                         DB2014.2
053600     GO TO G-T-D-2-DELETE.                                        DB2014.2
053700 G-T-D-WRITE.                                                     DB2014.2
053800     PERFORM PRINT-DETAIL.                                        DB2014.2
053900 G-T-D-6-INIT.                                                    DB2014.2
054000     MOVE "GO TO DEP/NOT ALL" TO FEATURE.                         DB2014.2
054100     MOVE "G-T-D-6" TO PAR-NAME.                                  DB2014.2
054200 G-T-D-6.                                                         DB2014.2
054300     MOVE 2 TO GO-TO-DEP-KEY-1.                                   DB2014.2
054400     MOVE SPACES TO ITEM-1  ITEM-2.                               DB2014.2
054500     MOVE 0 TO KEY-1  KEY-2.                                      DB2014.2
054600     GO TO G-T-D-7  G-T-D-8  G-T-D-9 DEPENDING ON GO-TO-DEP-KEY-1.DB2014.2
054700 G-T-D-6-DELETE.                                                  DB2014.2
054800     PERFORM DE-LETE.                                             DB2014.2
054900     PERFORM G-T-D-WRITE.                                         DB2014.2
055000     GO TO PERFORM-TESTS-INIT.                                    DB2014.2
055100 G-T-D-7.                                                         DB2014.2
055200     GO TO G-T-D-8.                                               DB2014.2
055300 G-T-D-8.                                                         DB2014.2
055400     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
055500         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
055600         PERFORM PASS                                             DB2014.2
055700     ELSE PERFORM FAIL                                            DB2014.2
055800         MOVE "DEBUG PROC EXECUTED" TO RE-MARK.                   DB2014.2
055900     PERFORM G-T-D-WRITE.                                         DB2014.2
056000     GO TO PERFORM-TESTS-INIT.                                    DB2014.2
056100 G-T-D-9.                                                         DB2014.2
056200     GO TO G-T-D-8.                                               DB2014.2
056300 PERFORM-TESTS-INIT.                                              DB2014.2
056400     MOVE "PERFORM VARYING" TO FEATURE.                           DB2014.2
056500     MOVE SPACES TO ITEM-1.                                       DB2014.2
056600     MOVE "PERFORM-VARY-1" TO PAR-NAME.                           DB2014.2
056700     MOVE 0 TO KEY-1.                                             DB2014.2
056800******************************************************************DB2014.2
056900*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2014.2
057000*    "P-V-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT WHICH     *DB2014.2
057100*    FOLLOWS THIS COMMENT SET AND WHICH READS, "PERFORM          *DB2014.2
057200*    P-V-1-SUBR VARYING ID-1 FROM 1 BY 1 UNTIL ID-1A IS GREATER  *DB2014.2
057300*    THAN 5.".                                                   *DB2014.2
057400******************************************************************DB2014.2
057500 PERFORM-VARY-1.                                                  DB2014.2
057600     PERFORM P-V-1-SUBR  VARYING ID-1 FROM 1 BY 1                 DB2014.2
057700         UNTIL ID-1A IS GREATER THAN 5.                           DB2014.2
057800     GO TO P-V-1-TEST.                                            DB2014.2
057900 PERFORM-VARYING-1-DELETE.                                        DB2014.2
058000     PERFORM DE-LETE.                                             DB2014.2
058100     PERFORM P-V-WRITE.                                           DB2014.2
058200     PERFORM DELETE-VARYING-SUBTESTS.                             DB2014.2
058300     GO TO PERFORM-VARYING-2-DELETE.                              DB2014.2
058400 DELETE-VARYING-SUBTESTS.                                         DB2014.2
058500     MOVE "P-V-1A" TO PAR-NAME.                                   DB2014.2
058600     PERFORM DE-LETE.                                             DB2014.2
058700     PERFORM P-V-WRITE.                                           DB2014.2
058800     MOVE "P-V-1B" TO PAR-NAME.                                   DB2014.2
058900     PERFORM DE-LETE.                                             DB2014.2
059000     PERFORM P-V-WRITE.                                           DB2014.2
059100     MOVE "P-V-1C" TO PAR-NAME.                                   DB2014.2
059200     PERFORM DE-LETE.                                             DB2014.2
059300     PERFORM P-V-WRITE.                                           DB2014.2
059400 P-V-1-SUBR.                                                      DB2014.2
059500     ADD 1 TO COUNTER.                                            DB2014.2
059600 P-V-1-TEST.                                                      DB2014.2
059700     IF KEY-1 IS EQUAL TO 6                                       DB2014.2
059800         MOVE "DEBUG PROC EXECUTED 6 TIMES" TO RE-MARK            DB2014.2
059900         PERFORM PASS                                             DB2014.2
060000     ELSE PERFORM FAIL                                            DB2014.2
060100         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK          DB2014.2
060200         MOVE KEY-1 TO COMPUTED-18V0                              DB2014.2
060300         MOVE 6 TO CORRECT-18V0.                                  DB2014.2
060400     PERFORM P-V-WRITE.                                           DB2014.2
060500     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
060600         PERFORM DELETE-VARYING-SUBTESTS                          DB2014.2
060700         GO TO PERFORM-VARYING-2-DELETE.                          DB2014.2
060800 P-V-1A.                                                          DB2014.2
060900     MOVE "P-V-1A" TO PAR-NAME.                                   DB2014.2
061000     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
061100     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
061200     MOVE LINE-1 TO COMPUTED-A.                                   DB2014.2
061300     PERFORM INSPT.                                               DB2014.2
061400     PERFORM P-V-WRITE.                                           DB2014.2
061500 P-V-1B.                                                          DB2014.2
061600     MOVE "P-V-1B" TO PAR-NAME.                                   DB2014.2
061700     IF UNQUAL-NAME-1 IS EQUAL TO "ID-1"                          DB2014.2
061800         PERFORM PASS                                             DB2014.2
061900     ELSE PERFORM FAIL                                            DB2014.2
062000         MOVE "ID-1" TO CORRECT-A                                 DB2014.2
062100         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
062200     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
062300     PERFORM P-V-WRITE.                                           DB2014.2
062400 P-V-1C.                                                          DB2014.2
062500     MOVE "P-V-1C" TO PAR-NAME.                                   DB2014.2
062600     IF CONTENTS-1 IS EQUAL TO "06"                               DB2014.2
062700         PERFORM PASS                                             DB2014.2
062800     ELSE PERFORM FAIL                                            DB2014.2
062900         MOVE "06" TO CORRECT-A                                   DB2014.2
063000         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
063100     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
063200     PERFORM P-V-WRITE.                                           DB2014.2
063300 PERFORM-VARY-2.                                                  DB2014.2
063400     PERFORM PERFORM-TESTS-INIT.                                  DB2014.2
063500     MOVE "PERFORM-VARY-2" TO PAR-NAME.                           DB2014.2
063600     PERFORM P-V-1-SUBR VARYING ID-1 FROM 1 BY 1                  DB2014.2
063700         UNTIL ID-1 IS GREATER THAN 5.                            DB2014.2
063800     GO TO P-V-2-TEST.                                            DB2014.2
063900 PERFORM-VARYING-2-DELETE.                                        DB2014.2
064000     MOVE "PERFORM-VARY-2" TO PAR-NAME.                           DB2014.2
064100     PERFORM DE-LETE.                                             DB2014.2
064200     PERFORM P-V-WRITE.                                           DB2014.2
064300     GO TO PERFORM-AFTER-INIT.                                    DB2014.2
064400 P-V-2-TEST.                                                      DB2014.2
064500     IF KEY-1 IS EQUAL TO 12                                      DB2014.2
064600         PERFORM PASS                                             DB2014.2
064700         MOVE "DEBUG PROC EXECUTED 12 TIMES" TO RE-MARK           DB2014.2
064800     ELSE PERFORM FAIL                                            DB2014.2
064900         MOVE "12" TO CORRECT-A                                   DB2014.2
065000         MOVE KEY-1 TO COMPUTED-A                                 DB2014.2
065100         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK.         DB2014.2
065200 P-V-WRITE.                                                       DB2014.2
065300     PERFORM PRINT-DETAIL.                                        DB2014.2
065400 PERFORM-AFTER-INIT.                                              DB2014.2
065500     MOVE "PERFORM AFTER" TO FEATURE.                             DB2014.2
065600     MOVE SPACES TO ITEM-1.                                       DB2014.2
065700     MOVE 0 TO KEY-1.                                             DB2014.2
065800     MOVE "PERFORM-AFTER-1" TO PAR-NAME.                          DB2014.2
065900 PERFORM-AFTER-1.                                                 DB2014.2
066000     PERFORM P-V-1-SUBR                                           DB2014.2
066100         VARYING ID-2A FROM 1 BY 1 UNTIL ID-2A IS GREATER THAN 5  DB2014.2
066200         AFTER ID-1 FROM 1 BY 1 UNTIL ID-1A IS GREATER THAN 5.    DB2014.2
066300     GO TO PERFORM-AFTER-1-TEST.                                  DB2014.2
066400 PERFORM-AFTER-1-DELETE.                                          DB2014.2
066500     PERFORM DE-LETE.                                             DB2014.2
066600     PERFORM P-A-WRITE.                                           DB2014.2
066700     GO TO DELETE-AFTER-SUBTEST.                                  DB2014.2
066800 PERFORM-AFTER-1-TEST.                                            DB2014.2
066900     IF KEY-1 IS EQUAL TO 31                                      DB2014.2
067000         MOVE "DEBUG PROC EXECUTED 31 TIMES" TO RE-MARK           DB2014.2
067100         PERFORM PASS                                             DB2014.2
067200     ELSE PERFORM FAIL                                            DB2014.2
067300         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK          DB2014.2
067400         MOVE KEY-1 TO COMPUTED-18V0                              DB2014.2
067500         MOVE 31 TO CORRECT-18V0.                                 DB2014.2
067600     PERFORM P-A-WRITE.                                           DB2014.2
067700     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
067800         GO TO DELETE-AFTER-SUBTEST.                              DB2014.2
067900 PERFORM-AFT-1A.                                                  DB2014.2
068000     MOVE "PERFORM-AFT-1A" TO PAR-NAME.                           DB2014.2
068100     IF CONTENTS-1 IS EQUAL TO "01"                               DB2014.2
068200         PERFORM PASS                                             DB2014.2
068300     ELSE PERFORM FAIL                                            DB2014.2
068400         MOVE "01" TO CORRECT-A                                   DB2014.2
068500         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
068600     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
068700     GO TO P-A-WRITE.                                             DB2014.2
068800 DELETE-AFTER-SUBTEST.                                            DB2014.2
068900     MOVE "PERFORM-AFT-1A"  TO PAR-NAME.                          DB2014.2
069000     PERFORM DE-LETE.                                             DB2014.2
069100 P-A-WRITE.                                                       DB2014.2
069200     PERFORM PRINT-DETAIL.                                        DB2014.2
069300 PERFORM-UNTIL-INIT.                                              DB2014.2
069400     MOVE "PERFORM UNTIL" TO FEATURE.                             DB2014.2
069500     MOVE SPACES TO ITEM-1.                                       DB2014.2
069600     MOVE 0 TO KEY-1.                                             DB2014.2
069700     MOVE "PERFORM-UNTIL-1" TO PAR-NAME.                          DB2014.2
069800 PERFORM-UNTIL-1.                                                 DB2014.2
069900     PERFORM P-V-1-SUBR                                           DB2014.2
070000         VARYING ID-1A FROM 1 BY 1 UNTIL ID-1 IS GREATER THAN 5.  DB2014.2
070100     GO TO PERFORM-UNTIL-1-TEST.                                  DB2014.2
070200 PERFORM-UNTIL-1-DELETE.                                          DB2014.2
070300     PERFORM DE-LETE.                                             DB2014.2
070400     PERFORM P-U-WRITE.                                           DB2014.2
070500     GO TO DELETE-UNTIL-SUBTEST.                                  DB2014.2
070600 PERFORM-UNTIL-1-TEST.                                            DB2014.2
070700     IF KEY-1 IS EQUAL TO 6                                       DB2014.2
070800         MOVE "DEBUG PROC EXECUTED 6 TIMES" TO RE-MARK            DB2014.2
070900         PERFORM PASS                                             DB2014.2
071000     ELSE PERFORM FAIL                                            DB2014.2
071100         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK          DB2014.2
071200         MOVE KEY-1 TO COMPUTED-18V0                              DB2014.2
071300         MOVE 6 TO CORRECT-18V0.                                  DB2014.2
071400     PERFORM P-A-WRITE.                                           DB2014.2
071500     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
071600         GO TO DELETE-UNTIL-SUBTEST.                              DB2014.2
071700 PERFORM-UNT-1A.                                                  DB2014.2
071800     MOVE "PERFORM-UNT-1A" TO PAR-NAME.                           DB2014.2
071900     IF CONTENTS-1 IS EQUAL TO "06"                               DB2014.2
072000         PERFORM PASS                                             DB2014.2
072100     ELSE PERFORM FAIL                                            DB2014.2
072200         MOVE "06" TO CORRECT-A                                   DB2014.2
072300         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
072400     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
072500     GO TO P-U-WRITE.                                             DB2014.2
072600 DELETE-UNTIL-SUBTEST.                                            DB2014.2
072700     MOVE "PERFORM-UNT-1A" TO PAR-NAME.                           DB2014.2
072800     PERFORM DE-LETE.                                             DB2014.2
072900 P-U-WRITE.                                                       DB2014.2
073000     PERFORM PRINT-DETAIL.                                        DB2014.2
073100 BYPASSED-CODE-1.                                                 DB2014.2
073200     MOVE 0 TO ID-3  KEY-1  KEY-2.                                DB2014.2
073300     MULTIPLY ID-1A BY ID-3.                                      DB2014.2
073400     MOVE "ALL REF OF IDENT" TO FEATURE.                          DB2014.2
073500     IF ID-3 IS NOT EQUAL TO 0                                    DB2014.2
073600         MOVE 1 TO ID-2  KEY-2.                                   DB2014.2
073700     IF KEY-2 IS NOT EQUAL TO 0                                   DB2014.2
073800         GO TO BYPASSED-CODE-DELETE.                              DB2014.2
073900     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
074000         PERFORM PASS                                             DB2014.2
074100     ELSE PERFORM FAIL                                            DB2014.2
074200         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK.               DB2014.2
074300     GO TO BYPASSED-CODE-WRITE.                                   DB2014.2
074400 BYPASSED-CODE-DELETE.                                            DB2014.2
074500     PERFORM DE-LETE.                                             DB2014.2
074600 BYPASSED-CODE-WRITE.                                             DB2014.2
074700     MOVE "BYPASSED-CODE-1" TO PAR-NAME.                          DB2014.2
074800     PERFORM PRINT-DETAIL.                                        DB2014.2
074900 REDEFINED-ID-1.                                                  DB2014.2
075000     MOVE 0 TO KEY-1.                                             DB2014.2
075100     MOVE "ALL REF OF IDENT" TO FEATURE.                          DB2014.2
075200     MOVE 0 TO ID-2A.                                             DB2014.2
075300     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
075400         PERFORM PASS                                             DB2014.2
075500     ELSE PERFORM FAIL                                            DB2014.2
075600         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK.               DB2014.2
075700     GO TO REDEFINED-ID-WRITE.                                    DB2014.2
075800 REDEFINED-ID-DELETE.                                             DB2014.2
075900     PERFORM DE-LETE.                                             DB2014.2
076000 REDEFINED-ID-WRITE.                                              DB2014.2
076100     MOVE "REDEFINED-ID-1" TO PAR-NAME.                           DB2014.2
076200     PERFORM PRINT-DETAIL.                                        DB2014.2
076300 MOVE-TEST-1-INIT.                                                DB2014.2
076400     MOVE "ALL REF OF IDENT" TO FEATURE.                          DB2014.2
076500     MOVE "MOVE-TEST-1" TO PAR-NAME.                              DB2014.2
076600     MOVE SPACES TO ITEM-1.                                       DB2014.2
076700     MOVE 0 TO KEY-1.                                             DB2014.2
076800******************************************************************DB2014.2
076900*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2014.2
077000*    "MOVE-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT     *DB2014.2
077100*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS, "MOVE 3 TO  *DB2014.2
077200*    ID-2.".                                                     *DB2014.2
077300******************************************************************DB2014.2
077400 MOVE-TEST-1.                                                     DB2014.2
077500     MOVE 3 TO ID-2.                                              DB2014.2
077600     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
077700         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
077800         PERFORM PASS                                             DB2014.2
077900         PERFORM MOVE-TEST-WRITE                                  DB2014.2
078000         GO TO MOVE-TEST-1A                                       DB2014.2
078100     ELSE PERFORM FAIL                                            DB2014.2
078200         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
078300         PERFORM MOVE-TEST-WRITE                                  DB2014.2
078400         PERFORM DELETE-MOVE-1-SUBTESTS                           DB2014.2
078500         GO TO MOVE-TEST-2-INIT.                                  DB2014.2
078600 MOVE-TEST-1-DELETE.                                              DB2014.2
078700     PERFORM DE-LETE.                                             DB2014.2
078800     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
078900     PERFORM DELETE-MOVE-1-SUBTESTS.                              DB2014.2
079000     GO TO MOVE-TEST-2-INIT.                                      DB2014.2
079100 DELETE-MOVE-1-SUBTESTS.                                          DB2014.2
079200     MOVE "MOVE-TEST-1A" TO PAR-NAME.                             DB2014.2
079300     PERFORM DE-LETE.                                             DB2014.2
079400     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
079500     MOVE "MOVE-TEST-1B" TO PAR-NAME.                             DB2014.2
079600     PERFORM DE-LETE.                                             DB2014.2
079700     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
079800     MOVE "MOVE-TEST-1C" TO PAR-NAME.                             DB2014.2
079900     PERFORM DE-LETE.                                             DB2014.2
080000     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
080100 MOVE-TEST-1A.                                                    DB2014.2
080200     MOVE "MOVE-TEST-1A" TO PAR-NAME.                             DB2014.2
080300     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
080400     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
080500     MOVE LINE-1 TO COMPUTED-A.                                   DB2014.2
080600     PERFORM INSPT.                                               DB2014.2
080700     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
080800 MOVE-TEST-1B.                                                    DB2014.2
080900     MOVE "MOVE-TEST-1B" TO PAR-NAME.                             DB2014.2
081000     IF UNQUAL-NAME-1 IS EQUAL TO "ID-2"                          DB2014.2
081100         PERFORM PASS                                             DB2014.2
081200     ELSE PERFORM FAIL                                            DB2014.2
081300         MOVE "ID-2" TO CORRECT-A                                 DB2014.2
081400         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
081500     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
081600     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
081700 MOVE-TEST-1C.                                                    DB2014.2
081800     MOVE "MOVE-TEST-1C" TO PAR-NAME.                             DB2014.2
081900     IF CONTENTS-1 IS EQUAL TO "03"                               DB2014.2
082000         PERFORM PASS                                             DB2014.2
082100     ELSE PERFORM FAIL                                            DB2014.2
082200         MOVE "03" TO CORRECT-A                                   DB2014.2
082300         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
082400     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
082500     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
082600 MOVE-TEST-2-INIT.                                                DB2014.2
082700     MOVE "NOT ALL REF OF IDENT" TO FEATURE.                      DB2014.2
082800     MOVE "MOVE-TEST-2" TO PAR-NAME.                              DB2014.2
082900     MOVE 0 TO ID-1A.                                             DB2014.2
083000     MOVE SPACES TO ITEM-1.                                       DB2014.2
083100     MOVE 0 TO KEY-1.                                             DB2014.2
083200 MOVE-TEST-2.                                                     DB2014.2
083300     MOVE 5 TO ID-1.                                              DB2014.2
083400     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
083500         MOVE "DEBUG EXECUTED" TO RE-MARK                         DB2014.2
083600         PERFORM PASS                                             DB2014.2
083700         PERFORM MOVE-TEST-WRITE                                  DB2014.2
083800         GO TO MOVE-TEST-2A                                       DB2014.2
083900     ELSE PERFORM FAIL                                            DB2014.2
084000         MOVE "DEBUG NOT EXECUTED" TO RE-MARK                     DB2014.2
084100         PERFORM MOVE-TEST-WRITE.                                 DB2014.2
084200 DELETE-MOVE-2-SUBTEST.                                           DB2014.2
084300     MOVE "MOVE-TEST-2A" TO PAR-NAME                              DB2014.2
084400     PERFORM DE-LETE.                                             DB2014.2
084500     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
084600     GO TO MOVE-TEST-3-DELETE.                                    DB2014.2
084700 MOVE-TEST-2A.                                                    DB2014.2
084800     MOVE "MOVE-TEST-2A" TO PAR-NAME.                             DB2014.2
084900     IF CONTENTS-1 IS EQUAL TO "05"                               DB2014.2
085000         PERFORM PASS                                             DB2014.2
085100     ELSE PERFORM FAIL                                            DB2014.2
085200         MOVE "05" TO CORRECT-A                                   DB2014.2
085300         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
085400     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
085500     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
085600 MOVE-TEST-3-INIT.                                                DB2014.2
085700     MOVE "REPEATED NOT ALL REF" TO FEATURE.                      DB2014.2
085800     MOVE "MOVE-TEST-3" TO PAR-NAME.                              DB2014.2
085900     MOVE 5 TO ID-1A.                                             DB2014.2
086000     MOVE SPACES TO ITEM-1.                                       DB2014.2
086100     MOVE 0 TO KEY-1.                                             DB2014.2
086200 MOVE-TEST-3.                                                     DB2014.2
086300     MOVE 5 TO ID-1.                                              DB2014.2
086400     IF KEY-1 IS  EQUAL TO 1                                      DB2014.2
086500         MOVE "REPEATED MOVE INVOKED PROC" TO RE-MARK             DB2014.2
086600     ELSE MOVE "REPEATED MOVE DIDN""T CALL PROC" TO RE-MARK.      DB2014.2
086700     MOVE "INFO" TO P-OR-F.                                       DB2014.2
086800     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
086900     GO TO MOVE-TEST-4-INIT.                                      DB2014.2
087000 MOVE-TEST-3-DELETE.                                              DB2014.2
087100     MOVE "REPEATED/NOT ALL REF" TO FEATURE.                      DB2014.2
087200     MOVE "MOVE-TEST-3" TO PAR-NAME.                              DB2014.2
087300     PERFORM DE-LETE.                                             DB2014.2
087400     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
087500 MOVE-TEST-4-INIT.                                                DB2014.2
087600     MOVE 2 TO ID-2A.                                             DB2014.2
087700     MOVE SPACES TO ITEM-1.                                       DB2014.2
087800     MOVE 0 TO KEY-1.                                             DB2014.2
087900     MOVE "SRC OPR/ALL REF" TO FEATURE.                           DB2014.2
088000     MOVE "MOVE-TEST-4" TO PAR-NAME.                              DB2014.2
088100******************************************************************DB2014.2
088200*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2014.2
088300*    "MOVE-TEST-4A" SHOULD POINT TO THE EXECUTABLE STATEMENT     *DB2014.2
088400*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS, "MOVE ID-2  *DB2014.2
088500*    TO ID-3.".                                                  *DB2014.2
088600******************************************************************DB2014.2
088700 MOVE-TEST-4.                                                     DB2014.2
088800     MOVE ID-2 TO ID-3.                                           DB2014.2
088900     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
089000         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
089100         PERFORM PASS                                             DB2014.2
089200         PERFORM MOVE-TEST-WRITE                                  DB2014.2
089300         GO TO MOVE-TEST-4A                                       DB2014.2
089400     ELSE PERFORM FAIL                                            DB2014.2
089500         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
089600         PERFORM MOVE-TEST-WRITE                                  DB2014.2
089700         PERFORM DELETE-MOVE-4-SUBTESTS                           DB2014.2
089800         GO TO MOVE-TEST-5-INIT.                                  DB2014.2
089900 MOVE-TEST-4-DELETE.                                              DB2014.2
090000     PERFORM DE-LETE.                                             DB2014.2
090100     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
090200     PERFORM DELETE-MOVE-4-SUBTESTS.                              DB2014.2
090300     GO TO MOVE-TEST-5-INIT.                                      DB2014.2
090400 DELETE-MOVE-4-SUBTESTS.                                          DB2014.2
090500     MOVE "MOVE-TEST-4A" TO PAR-NAME.                             DB2014.2
090600     PERFORM DE-LETE.                                             DB2014.2
090700     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
090800     MOVE "MOVE-TEST-4B" TO PAR-NAME.                             DB2014.2
090900     PERFORM DE-LETE.                                             DB2014.2
091000     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
091100     MOVE "MOVE-TEST-4C" TO PAR-NAME.                             DB2014.2
091200     PERFORM DE-LETE.                                             DB2014.2
091300     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
091400 MOVE-TEST-4A.                                                    DB2014.2
091500     MOVE "MOVE-TEST-4A" TO PAR-NAME.                             DB2014.2
091600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
091700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
091800     MOVE LINE-1 TO COMPUTED-A.                                   DB2014.2
091900     PERFORM INSPT.                                               DB2014.2
092000     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
092100 MOVE-TEST-4B.                                                    DB2014.2
092200     MOVE "MOVE-TEST-4B" TO PAR-NAME.                             DB2014.2
092300     IF UNQUAL-NAME-1 IS EQUAL TO "ID-2"                          DB2014.2
092400         PERFORM PASS                                             DB2014.2
092500     ELSE PERFORM FAIL                                            DB2014.2
092600         MOVE "ID-2" TO CORRECT-A                                 DB2014.2
092700         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
092800     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
092900     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
093000 MOVE-TEST-4C.                                                    DB2014.2
093100     MOVE "MOVE-TEST-4C" TO PAR-NAME.                             DB2014.2
093200     IF CONTENTS-1 IS EQUAL TO "02"                               DB2014.2
093300         PERFORM PASS                                             DB2014.2
093400     ELSE PERFORM FAIL                                            DB2014.2
093500         MOVE "02" TO CORRECT-A                                   DB2014.2
093600         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
093700     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
093800     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
093900 MOVE-TEST-5-INIT.                                                DB2014.2
094000     MOVE 2 TO ID-1A.                                             DB2014.2
094100     MOVE SPACES TO ITEM-1.                                       DB2014.2
094200     MOVE 0 TO KEY-1                                              DB2014.2
094300     MOVE "SRC OPR/NOT ALL REF" TO FEATURE.                       DB2014.2
094400     MOVE "MOVE-TEST-5" TO PAR-NAME.                              DB2014.2
094500 MOVE-TEST-5.                                                     DB2014.2
094600     MOVE ID-1 TO ID-3.                                           DB2014.2
094700     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
094800         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
094900         PERFORM PASS                                             DB2014.2
095000     ELSE PERFORM FAIL                                            DB2014.2
095100         MOVE "DEBUG PROC EXECUTED" TO RE-MARK.                   DB2014.2
095200     PERFORM MOVE-TEST-WRITE.                                     DB2014.2
095300     GO TO ADD-TEST-1-INIT.                                       DB2014.2
095400 MOVE-TEST-5-DELETE.                                              DB2014.2
095500     PERFORM DE-LETE.                                             DB2014.2
095600 MOVE-TEST-WRITE.                                                 DB2014.2
095700     PERFORM PRINT-DETAIL.                                        DB2014.2
095800 ADD-TEST-1-INIT.                                                 DB2014.2
095900     MOVE 1 TO ID-2A.                                             DB2014.2
096000     MOVE SPACES TO ITEM-1.                                       DB2014.2
096100     MOVE 0 TO KEY-1  ID-3.                                       DB2014.2
096200     MOVE "MULT SRC OPR/ALL REF" TO FEATURE.                      DB2014.2
096300     MOVE "ADD-TEST-1" TO PAR-NAME.                               DB2014.2
096400******************************************************************DB2014.2
096500*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2014.2
096600*    "ADD-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT      *DB2014.2
096700*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS,             *DB2014.2
096800*    "ADD  ID-2  ID-2  ID-2  ID-2    TO    ID-3.".               *DB2014.2
096900******************************************************************DB2014.2
097000 ADD-TEST-1.                                                      DB2014.2
097100     ADD  ID-2  ID-2  ID-2  ID-2    TO    ID-3.                   DB2014.2
097200     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
097300         PERFORM PASS                                             DB2014.2
097400         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
097500     ELSE  PERFORM FAIL                                           DB2014.2
097600         MOVE "NO. OF TIMES DEBUG EXECUTED" TO RE-MARK            DB2014.2
097700         MOVE KEY-1 TO COMPUTED-18V0                              DB2014.2
097800         MOVE 1 TO CORRECT-18V0.                                  DB2014.2
097900     PERFORM ADD-TEST-WRITE.                                      DB2014.2
098000     GO TO ADD-TEST-1A.                                           DB2014.2
098100 ADD-TEST-1-DELETE.                                               DB2014.2
098200     PERFORM DE-LETE.                                             DB2014.2
098300     PERFORM ADD-TEST-WRITE.                                      DB2014.2
098400     PERFORM DELETE-ADD-1-SUBTESTS.                               DB2014.2
098500     GO TO ADD-TEST-2-INIT.                                       DB2014.2
098600 DELETE-ADD-1-SUBTESTS.                                           DB2014.2
098700     MOVE "ADD-TEST-1A" TO PAR-NAME.                              DB2014.2
098800     PERFORM DE-LETE.                                             DB2014.2
098900     PERFORM ADD-TEST-WRITE.                                      DB2014.2
099000     MOVE "ADD-TEST-1B" TO PAR-NAME.                              DB2014.2
099100     PERFORM DE-LETE.                                             DB2014.2
099200     PERFORM ADD-TEST-WRITE.                                      DB2014.2
099300     MOVE "ADD-TEST-1C" TO PAR-NAME.                              DB2014.2
099400     PERFORM DE-LETE.                                             DB2014.2
099500     PERFORM ADD-TEST-WRITE.                                      DB2014.2
099600 ADD-TEST-1A.                                                     DB2014.2
099700     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
099800         PERFORM DELETE-ADD-1-SUBTESTS                            DB2014.2
099900         GO TO ADD-TEST-2-INIT.                                   DB2014.2
100000     MOVE "ADD-TEST-1A" TO PAR-NAME.                              DB2014.2
100100     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
100200     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
100300     MOVE LINE-1 TO COMPUTED-A.                                   DB2014.2
100400     PERFORM INSPT.                                               DB2014.2
100500     PERFORM ADD-TEST-WRITE.                                      DB2014.2
100600 ADD-TEST-1B.                                                     DB2014.2
100700     MOVE "ADD-TEST-1B" TO PAR-NAME.                              DB2014.2
100800     IF UNQUAL-NAME-1 IS EQUAL TO "ID-2"                          DB2014.2
100900         PERFORM PASS                                             DB2014.2
101000     ELSE PERFORM FAIL                                            DB2014.2
101100         MOVE "ID-2" TO CORRECT-A                                 DB2014.2
101200         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
101300     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
101400     PERFORM ADD-TEST-WRITE.                                      DB2014.2
101500 ADD-TEST-1C.                                                     DB2014.2
101600     MOVE "ADD-TEST-1C" TO PAR-NAME.                              DB2014.2
101700     IF CONTENTS-1 IS EQUAL TO "01"                               DB2014.2
101800         PERFORM PASS                                             DB2014.2
101900     ELSE  PERFORM FAIL                                           DB2014.2
102000         MOVE "01" TO CORRECT-A                                   DB2014.2
102100         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
102200     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
102300     PERFORM ADD-TEST-WRITE.                                      DB2014.2
102400 ADD-TEST-2-INIT.                                                 DB2014.2
102500     MOVE 1 TO ID-1A.                                             DB2014.2
102600     MOVE SPACES TO ITEM-1.                                       DB2014.2
102700     MOVE 0 TO  KEY-1  ID-3.                                      DB2014.2
102800     MOVE "MULT SRC OPR/NOT ALL" TO FEATURE.                      DB2014.2
102900     MOVE "ADD-TEST-2" TO PAR-NAME.                               DB2014.2
103000 ADD-TEST-2.                                                      DB2014.2
103100     ADD  ID-1  ID-1  ID-1  ID-1    TO    ID-3.                   DB2014.2
103200     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
103300         PERFORM PASS                                             DB2014.2
103400         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
103500     ELSE  PERFORM FAIL                                           DB2014.2
103600         MOVE "NO. OF TIMES DEBUG EXECUTED" TO RE-MARK            DB2014.2
103700         MOVE KEY-1 TO COMPUTED-A                                 DB2014.2
103800         MOVE "0" TO CORRECT-A.                                   DB2014.2
103900     PERFORM ADD-TEST-WRITE.                                      DB2014.2
104000     GO TO ADD-TEST-3-INIT.                                       DB2014.2
104100 ADD-TEST-2-DELETE.                                               DB2014.2
104200     PERFORM DE-LETE.                                             DB2014.2
104300     PERFORM ADD-TEST-WRITE.                                      DB2014.2
104400 ADD-TEST-3-INIT.                                                 DB2014.2
104500     MOVE 1 TO ID-2A.                                             DB2014.2
104600     MOVE SPACES TO ITEM-1.                                       DB2014.2
104700     MOVE 0 TO KEY-1.                                             DB2014.2
104800     MOVE "SRC-RCV OPR/ALL REF" TO FEATURE.                       DB2014.2
104900     MOVE "ADD-TEST-3" TO PAR-NAME.                               DB2014.2
105000 ADD-TEST-3.                                                      DB2014.2
105100     ADD  ID-2  ID-2  ID-2  ID-2    TO    ID-2.                   DB2014.2
105200     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
105300         PERFORM PASS                                             DB2014.2
105400         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
105500         PERFORM ADD-TEST-WRITE                                   DB2014.2
105600         GO TO ADD-TEST-3A                                        DB2014.2
105700     ELSE PERFORM FAIL                                            DB2014.2
105800         MOVE "NO. OF TIMES DEBUG EXECUTED" TO RE-MARK            DB2014.2
105900         MOVE KEY-1 TO COMPUTED-18V0                              DB2014.2
106000         MOVE 1 TO CORRECT-18V0.                                  DB2014.2
106100     PERFORM ADD-TEST-WRITE.                                      DB2014.2
106200     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
106300         PERFORM DELETE-ADD-TEST-3-SUBTEST                        DB2014.2
106400         GO TO ADD-TEST-4-INIT                                    DB2014.2
106500     ELSE GO TO ADD-TEST-3A.                                      DB2014.2
106600 ADD-TEST-3-DELETE.                                               DB2014.2
106700     PERFORM DE-LETE.                                             DB2014.2
106800     PERFORM ADD-TEST-WRITE.                                      DB2014.2
106900     PERFORM DELETE-ADD-TEST-3-SUBTEST.                           DB2014.2
107000     GO TO ADD-TEST-4-INIT.                                       DB2014.2
107100 DELETE-ADD-TEST-3-SUBTEST.                                       DB2014.2
107200     MOVE "ADD-TEST-3A" TO PAR-NAME.                              DB2014.2
107300     PERFORM DE-LETE.                                             DB2014.2
107400     PERFORM ADD-TEST-WRITE.                                      DB2014.2
107500 ADD-TEST-3A.                                                     DB2014.2
107600     MOVE "ADD-TEST-3A" TO PAR-NAME.                              DB2014.2
107700     IF CONTENTS-1 IS EQUAL TO "05"                               DB2014.2
107800         PERFORM PASS                                             DB2014.2
107900     ELSE PERFORM FAIL                                            DB2014.2
108000         MOVE "05" TO CORRECT-A                                   DB2014.2
108100         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
108200     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
108300     PERFORM ADD-TEST-WRITE.                                      DB2014.2
108400 ADD-TEST-4-INIT.                                                 DB2014.2
108500     MOVE 1 TO ID-1A.                                             DB2014.2
108600     MOVE SPACES TO ITEM-1.                                       DB2014.2
108700     MOVE 0 TO KEY-1.                                             DB2014.2
108800     MOVE "SRC-RCV OPR/NOT ALL" TO FEATURE.                       DB2014.2
108900     MOVE "ADD-TEST-4" TO PAR-NAME.                               DB2014.2
109000 ADD-TEST-4.                                                      DB2014.2
109100     ADD  ID-1  ID-1  ID-1  ID-1    TO    ID-1.                   DB2014.2
109200     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
109300         PERFORM PASS                                             DB2014.2
109400         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
109500         PERFORM ADD-TEST-WRITE                                   DB2014.2
109600         GO TO ADD-TEST-4A                                        DB2014.2
109700     ELSE  PERFORM FAIL                                           DB2014.2
109800         MOVE "NO. OF TIMES DEBUG EXECUTED" TO RE-MARK            DB2014.2
109900         MOVE KEY-1 TO COMPUTED-18V0                              DB2014.2
110000         MOVE 1 TO CORRECT-18V0.                                  DB2014.2
110100     PERFORM ADD-TEST-WRITE.                                      DB2014.2
110200     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
110300         PERFORM DELETE-ADD-TEST-4-SUBTEST                        DB2014.2
110400         GO TO SUBC-TEST-1-INIT                                   DB2014.2
110500     ELSE  GO TO ADD-TEST-4A.                                     DB2014.2
110600 ADD-TEST-4-DELETE.                                               DB2014.2
110700     PERFORM DE-LETE.                                             DB2014.2
110800     PERFORM ADD-TEST-WRITE.                                      DB2014.2
110900     PERFORM DELETE-ADD-TEST-4-SUBTEST.                           DB2014.2
111000     GO TO SUBC-TEST-1-INIT.                                      DB2014.2
111100 DELETE-ADD-TEST-4-SUBTEST.                                       DB2014.2
111200     MOVE "ADD-TEST-4A" TO PAR-NAME.                              DB2014.2
111300     PERFORM DE-LETE.                                             DB2014.2
111400     PERFORM ADD-TEST-WRITE.                                      DB2014.2
111500 ADD-TEST-4A.                                                     DB2014.2
111600     MOVE "ADD-TEST-4A" TO PAR-NAME.                              DB2014.2
111700     IF CONTENTS-1 IS EQUAL TO "05"                               DB2014.2
111800         PERFORM PASS                                             DB2014.2
111900     ELSE  PERFORM FAIL                                           DB2014.2
112000         MOVE "05" TO CORRECT-A                                   DB2014.2
112100         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
112200     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
112300 ADD-TEST-WRITE.                                                  DB2014.2
112400     PERFORM PRINT-DETAIL.                                        DB2014.2
112500 SUBC-TEST-1-INIT.                                                DB2014.2
112600     MOVE "SUBSC-TEST-1" TO PAR-NAME.                             DB2014.2
112700     MOVE "ONE-DIM SUBSCRIPT" TO FEATURE.                         DB2014.2
112800     MOVE SPACE TO ITEM-1.                                        DB2014.2
112900     MOVE 0 TO KEY-1.                                             DB2014.2
113000     SET I TO 5.                                                  DB2014.2
113100******************************************************************DB2014.2
113200*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2014.2
113300*    "SUBSC-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT    *DB2014.2
113400*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS, "MOVE       *DB2014.2
113500*    "ABCDE" TO B-LEVEL1 (I).".                                  *DB2014.2
113600******************************************************************DB2014.2
113700 SUBSC-TEST-1.                                                    DB2014.2
113800     MOVE "ABCDE" TO B-LEVEL-1 (I).                               DB2014.2
113900     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
114000         PERFORM PASS                                             DB2014.2
114100         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2014.2
114200         PERFORM SUBSC-TEST-WRITE                                 DB2014.2
114300         GO TO SUBSC-TEST-1A                                      DB2014.2
114400     ELSE  PERFORM FAIL                                           DB2014.2
114500         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
114600         PERFORM SUBSC-TEST-WRITE                                 DB2014.2
114700         PERFORM DELETE-SUBSC-TEST-1-SUBTESTS                     DB2014.2
114800         GO TO SUBSC-TEST-2-INIT.                                 DB2014.2
114900 SUBSC-TEST-1-DELETE.                                             DB2014.2
115000     PERFORM DE-LETE.                                             DB2014.2
115100     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
115200     PERFORM DELETE-SUBSC-TEST-1-SUBTESTS.                        DB2014.2
115300     GO TO SUBSC-TEST-2-INIT.                                     DB2014.2
115400 DELETE-SUBSC-TEST-1-SUBTESTS.                                    DB2014.2
115500     MOVE "SUBSC-TEST-1A" TO PAR-NAME.                            DB2014.2
115600     PERFORM DE-LETE.                                             DB2014.2
115700     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
115800     MOVE "SUBSC-TEST-1B" TO PAR-NAME.                            DB2014.2
115900     PERFORM DE-LETE.                                             DB2014.2
116000     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
116100     MOVE "SUBSC-TEST-1C" TO PAR-NAME.                            DB2014.2
116200     PERFORM DE-LETE.                                             DB2014.2
116300     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
116400     MOVE "SUBSC-TEST-1D" TO PAR-NAME.                            DB2014.2
116500     PERFORM DE-LETE.                                             DB2014.2
116600     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
116700     MOVE "SUBSC-TEST-1E" TO PAR-NAME.                            DB2014.2
116800     PERFORM DE-LETE.                                             DB2014.2
116900     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
117000     MOVE "SUBSC-TEST-1F" TO PAR-NAME.                            DB2014.2
117100     PERFORM DE-LETE.                                             DB2014.2
117200     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
117300 SUBSC-TEST-1A.                                                   DB2014.2
117400     MOVE "SUBSC-TEST-1A" TO PAR-NAME.                            DB2014.2
117500     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
117600     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
117700     MOVE LINE-1 TO COMPUTED-A.                                   DB2014.2
117800     PERFORM INSPT.                                               DB2014.2
117900     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
118000 SUBSC-TEST-1B.                                                   DB2014.2
118100     MOVE "SUBSC-TEST-1B" TO PAR-NAME.                            DB2014.2
118200     IF UNQUAL-NAME-1 IS EQUAL TO "B-LEVEL-1"                     DB2014.2
118300         PERFORM PASS                                             DB2014.2
118400     ELSE PERFORM FAIL                                            DB2014.2
118500         MOVE "B-LEVEL-1" TO CORRECT-A                            DB2014.2
118600         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
118700     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
118800     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
118900 SUBSC-TEST-1C.                                                   DB2014.2
119000     MOVE "SUBSC-TEST-1C" TO PAR-NAME.                            DB2014.2
119100     IF CONTENTS-1 IS EQUAL TO "ABCDE"                            DB2014.2
119200         PERFORM PASS                                             DB2014.2
119300     ELSE  PERFORM FAIL                                           DB2014.2
119400         MOVE "ABCDE" TO CORRECT-A                                DB2014.2
119500         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
119600     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
119700     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
119800 SUBSC-TEST-1D.                                                   DB2014.2
119900     MOVE "SUBSC-TEST-1D" TO PAR-NAME.                            DB2014.2
120000     IF SUB-1-1 IS EQUAL TO "0005"                                DB2014.2
120100         PERFORM PASS                                             DB2014.2
120200     ELSE  PERFORM FAIL                                           DB2014.2
120300         MOVE "0005" TO CORRECT-A                                 DB2014.2
120400         MOVE SUB-1-1 TO COMPUTED-A.                              DB2014.2
120500     MOVE "DEBUG-SUB-1" TO RE-MARK.                               DB2014.2
120600     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
120700 SUBSC-TEST-1E.                                                   DB2014.2
120800     MOVE "SUBSC-TEST-1E" TO PAR-NAME.                            DB2014.2
120900     PERFORM DE-LETE.                                             DB2014.2
121000     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
121100 SUBSC-TEST-1F.                                                   DB2014.2
121200     MOVE "SUBSC-TEST-1F" TO PAR-NAME.                            DB2014.2
121300     PERFORM DE-LETE.                                             DB2014.2
121400     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
121500 SUBSC-TEST-2-INIT.                                               DB2014.2
121600     MOVE "SUBSC-TEST-2" TO PAR-NAME.                             DB2014.2
121700     MOVE "THR-DIM SUBSCRIPT" TO FEATURE.                         DB2014.2
121800     MOVE SPACES TO ITEM-1.                                       DB2014.2
121900     MOVE 0 TO KEY-1.                                             DB2014.2
122000     SET I TO 4                                                   DB2014.2
122100     SET J TO 6.                                                  DB2014.2
122200     SET K TO 8.                                                  DB2014.2
122300 SUBSC-TEST-2.                                                    DB2014.2
122400     MOVE "Z" TO B-LEVEL-3 (I, J, K).                             DB2014.2
122500     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
122600         PERFORM PASS                                             DB2014.2
122700         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2014.2
122800         PERFORM SUBSC-TEST-WRITE                                 DB2014.2
122900         GO TO SUBSC-TEST-2A                                      DB2014.2
123000     ELSE  PERFORM FAIL                                           DB2014.2
123100         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
123200         PERFORM SUBSC-TEST-WRITE                                 DB2014.2
123300         PERFORM DELETE-SUBSC-TEST-2-SUBTESTS                     DB2014.2
123400         GO TO QUAL-TEST-1-INIT.                                  DB2014.2
123500  SUBSC-TEST-2-DELETE.                                            DB2014.2
123600     PERFORM DE-LETE.                                             DB2014.2
123700     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
123800     PERFORM DELETE-SUBSC-TEST-2-SUBTESTS.                        DB2014.2
123900     GO TO QUAL-TEST-1-INIT.                                      DB2014.2
124000 DELETE-SUBSC-TEST-2-SUBTESTS.                                    DB2014.2
124100     MOVE "SUBSC-TEST-2A" TO PAR-NAME.                            DB2014.2
124200     PERFORM DE-LETE.                                             DB2014.2
124300     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
124400     MOVE "SUBSC-TEST-2B" TO PAR-NAME.                            DB2014.2
124500     PERFORM DE-LETE.                                             DB2014.2
124600     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
124700     MOVE "SUBSC-TEST-2C" TO PAR-NAME.                            DB2014.2
124800     PERFORM DE-LETE.                                             DB2014.2
124900     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
125000     MOVE "SUBSC-TEST-2D" TO PAR-NAME.                            DB2014.2
125100     PERFORM DE-LETE.                                             DB2014.2
125200     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
125300 SUBSC-TEST-2A.                                                   DB2014.2
125400     MOVE "SUBSC-TEST-2A" TO PAR-NAME.                            DB2014.2
125500     IF CONTENTS-1 IS EQUAL TO "Z"                                DB2014.2
125600         PERFORM PASS                                             DB2014.2
125700     ELSE  PERFORM FAIL                                           DB2014.2
125800         MOVE "Z" TO CORRECT-A                                    DB2014.2
125900         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
126000     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
126100     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
126200 SUBSC-TEST-2B.                                                   DB2014.2
126300     MOVE "SUBSC-TEST-2B" TO PAR-NAME.                            DB2014.2
126400     IF SUB-1-1 IS EQUAL TO "0004"                                DB2014.2
126500         PERFORM PASS                                             DB2014.2
126600     ELSE  PERFORM FAIL                                           DB2014.2
126700         MOVE "0004" TO CORRECT-A                                 DB2014.2
126800         MOVE SUB-1-1 TO COMPUTED-A.                              DB2014.2
126900     MOVE "DEBUG-SUB-1" TO RE-MARK.                               DB2014.2
127000     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
127100 SUBSC-TEST-2C.                                                   DB2014.2
127200     MOVE "SUBSC-TEST-2C" TO PAR-NAME.                            DB2014.2
127300     IF SUB-2-1 IS EQUAL TO "0006"                                DB2014.2
127400         PERFORM PASS                                             DB2014.2
127500     ELSE  PERFORM FAIL                                           DB2014.2
127600         MOVE "0006" TO CORRECT-A                                 DB2014.2
127700         MOVE SUB-2-1 TO COMPUTED-A.                              DB2014.2
127800     MOVE "DEBUG-SUB-2" TO RE-MARK.                               DB2014.2
127900     PERFORM SUBSC-TEST-WRITE.                                    DB2014.2
128000 SUBSC-TEST-2D.                                                   DB2014.2
128100     MOVE "SUBSC-TEST-2D" TO PAR-NAME.                            DB2014.2
128200     IF SUB-3-1 IS EQUAL TO "0008"                                DB2014.2
128300         PERFORM PASS                                             DB2014.2
128400     ELSE  PERFORM FAIL                                           DB2014.2
128500         MOVE "0008" TO CORRECT-A                                 DB2014.2
128600         MOVE SUB-3-1 TO COMPUTED-A.                              DB2014.2
128700     MOVE "DEBUG-SUB-3" TO RE-MARK.                               DB2014.2
128800 SUBSC-TEST-WRITE.                                                DB2014.2
128900     PERFORM PRINT-DETAIL.                                        DB2014.2
129000 QUAL-TEST-1-INIT.                                                DB2014.2
129100     MOVE "QUAL-TEST-1" TO PAR-NAME.                              DB2014.2
129200     MOVE "1-LEVEL QUALIFICATN" TO FEATURE.                       DB2014.2
129300     MOVE SPACES TO ITEM-1.                                       DB2014.2
129400     MOVE 0 TO KEY-1.                                             DB2014.2
129500******************************************************************DB2014.2
129600*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2014.2
129700*    "QUAL-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT     *DB2014.2
129800*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS,             *DB2014.2
129900*    "MOVE "XY" TO  AB2 OF A2.".                                 *DB2014.2
130000******************************************************************DB2014.2
130100 QUAL-TEST-1.                                                     DB2014.2
130200     MOVE "XY" TO  AB2 OF A2.                                     DB2014.2
130300     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
130400         PERFORM PASS                                             DB2014.2
130500         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
130600             PERFORM QUAL-TEST-WRITE                              DB2014.2
130700         GO TO QUAL-TEST-1A                                       DB2014.2
130800     ELSE  PERFORM FAIL                                           DB2014.2
130900         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
131000         PERFORM QUAL-TEST-WRITE                                  DB2014.2
131100         PERFORM DELETE-QUAL-TEST-1-SUBTESTS                      DB2014.2
131200         GO TO QUAL-TEST-2-INIT.                                  DB2014.2
131300 QUAL-TEST-1-DELETE.                                              DB2014.2
131400     PERFORM DE-LETE.                                             DB2014.2
131500     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
131600     PERFORM DELETE-QUAL-TEST-1-SUBTESTS.                         DB2014.2
131700     GO TO QUAL-TEST-2-INIT.                                      DB2014.2
131800 DELETE-QUAL-TEST-1-SUBTESTS.                                     DB2014.2
131900     MOVE "QUAL-TEST-1A" TO PAR-NAME.                             DB2014.2
132000     PERFORM DE-LETE                                              DB2014.2
132100     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
132200     MOVE "QUAL-TEST-1B" TO PAR-NAME.                             DB2014.2
132300     PERFORM DE-LETE.                                             DB2014.2
132400     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
132500     MOVE "QUAL-TEST-1C" TO PAR-NAME.                             DB2014.2
132600     PERFORM DE-LETE.                                             DB2014.2
132700     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
132800 QUAL-TEST-1A.                                                    DB2014.2
132900     MOVE "QUAL-TEST-1A" TO PAR-NAME.                             DB2014.2
133000     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2014.2
133100     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2014.2
133200     MOVE LINE-1 TO COMPUTED-A.                                   DB2014.2
133300     PERFORM INSPT.                                               DB2014.2
133400     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
133500 QUAL-TEST-1B.                                                    DB2014.2
133600         MOVE "QUAL-TEST-1B" TO PAR-NAME.                         DB2014.2
133700         IF NAME-1 IS EQUAL TO "AB2 OF A2"   OR                   DB2014.2
133800            NAME-1 IS EQUAL TO "AB2 IN A2"                        DB2014.2
133900         PERFORM PASS                                             DB2014.2
134000     ELSE  PERFORM FAIL                                           DB2014.2
134100         MOVE "AB2 OF(IN) A2" TO CORRECT-A                        DB2014.2
134200         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
134300     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
134400     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
134500 QUAL-TEST-1C.                                                    DB2014.2
134600     MOVE "QUAL-TEST-1C" TO PAR-NAME.                             DB2014.2
134700     IF CONTENTS-1 IS EQUAL TO "XY"                               DB2014.2
134800         PERFORM PASS                                             DB2014.2
134900     ELSE  PERFORM FAIL                                           DB2014.2
135000         MOVE "XY" TO CORRECT-A                                   DB2014.2
135100         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
135200     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
135300     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
135400 QUAL-TEST-2-INIT.                                                DB2014.2
135500     MOVE "QUAL-TEST-2" TO PAR-NAME.                              DB2014.2
135600     MOVE "1-LEVEL QUALIFICATN" TO FEATURE.                       DB2014.2
135700     MOVE SPACES TO ITEM-1.                                       DB2014.2
135800     MOVE 0 TO KEY-1.                                             DB2014.2
135900 QUAL-TEST-2.                                                     DB2014.2
136000     MOVE "CD" TO AB2 OF A1.                                      DB2014.2
136100     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
136200         PERFORM PASS                                             DB2014.2
136300         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
136400     ELSE  PERFORM FAIL                                           DB2014.2
136500         MOVE "DEBUG PROC EXECUTED" TO RE-MARK.                   DB2014.2
136600     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
136700     GO TO QUAL-TEST-3-INIT.                                      DB2014.2
136800 QUAL-TEST-2-DELETE.                                              DB2014.2
136900     PERFORM DE-LETE.                                             DB2014.2
137000     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
137100 QUAL-TEST-3-INIT.                                                DB2014.2
137200     MOVE "QUAL-TEST-3" TO PAR-NAME.                              DB2014.2
137300     MOVE "1-LEVEL QUALIFICATN" TO FEATURE.                       DB2014.2
137400     MOVE SPACES TO ITEM-1.                                       DB2014.2
137500     MOVE 0 TO KEY-1.                                             DB2014.2
137600 QUAL-TEST-3.                                                     DB2014.2
137700     MOVE "Q" TO ABC2 OF AB2 OF A2.                               DB2014.2
137800     IF KEY-1 IS EQUAL TO 0                                       DB2014.2
137900         PERFORM PASS                                             DB2014.2
138000         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
138100     ELSE  PERFORM FAIL                                           DB2014.2
138200         MOVE "DEBUG PROC EXECUTED" TO RE-MARK.                   DB2014.2
138300     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
138400     GO TO QUAL-TEST-4-INIT.                                      DB2014.2
138500 QUAL-TEST-3-DELETE.                                              DB2014.2
138600     PERFORM DE-LETE.                                             DB2014.2
138700     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
138800 QUAL-TEST-4-INIT.                                                DB2014.2
138900     MOVE "QUAL-TEST-4" TO PAR-NAME.                              DB2014.2
139000     MOVE "2-LEVEL QUALIFICATN" TO FEATURE.                       DB2014.2
139100     MOVE SPACES TO ITEM-1.                                       DB2014.2
139200     MOVE 0 TO KEY-1.                                             DB2014.2
139300 QUAL-TEST-4.                                                     DB2014.2
139400     MOVE "G" TO ABC1 OF AB2 OF A1.                               DB2014.2
139500     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
139600         PERFORM PASS                                             DB2014.2
139700         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
139800         PERFORM QUAL-TEST-WRITE                                  DB2014.2
139900         GO TO QUAL-TEST-4A                                       DB2014.2
140000     ELSE  PERFORM FAIL                                           DB2014.2
140100         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
140200         PERFORM QUAL-TEST-WRITE                                  DB2014.2
140300         PERFORM DELETE-QUAL-TEST-4-SUBTESTS                      DB2014.2
140400         GO TO QUAL-TEST-5-INIT.                                  DB2014.2
140500 QUAL-TEST-4-DELETE.                                              DB2014.2
140600     PERFORM DE-LETE.                                             DB2014.2
140700     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
140800     PERFORM DELETE-QUAL-TEST-4-SUBTESTS.                         DB2014.2
140900     GO TO QUAL-TEST-5-INIT.                                      DB2014.2
141000 DELETE-QUAL-TEST-4-SUBTESTS.                                     DB2014.2
141100     MOVE "QUAL-TEST-4A" TO PAR-NAME.                             DB2014.2
141200     PERFORM DE-LETE.                                             DB2014.2
141300     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
141400     MOVE "QUAL-TEST-4B" TO PAR-NAME.                             DB2014.2
141500     PERFORM DE-LETE.                                             DB2014.2
141600     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
141700 QUAL-TEST-4A.                                                    DB2014.2
141800     MOVE "QUAL-TEST-4A" TO PAR-NAME.                             DB2014.2
141900     IF NAME-1 IS EQUAL TO "ABC1 OF AB2 OF A1"  OR                DB2014.2
142000        NAME-1 IS EQUAL TO "ABC1 IN AB2 IN A1"                    DB2014.2
142100         PERFORM PASS                                             DB2014.2
142200     ELSE PERFORM FAIL                                            DB2014.2
142300         MOVE "ABC1 OF AB2 OF A1" TO CORRECT-A                    DB2014.2
142400         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
142500     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
142600     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
142700 QUAL-TEST-4B.                                                    DB2014.2
142800     MOVE "QUAL-TEST-4B" TO PAR-NAME.                             DB2014.2
142900     IF CONTENTS-1 IS EQUAL TO "G"                                DB2014.2
143000         PERFORM PASS                                             DB2014.2
143100     ELSE  PERFORM FAIL                                           DB2014.2
143200         MOVE "G" TO CORRECT-A                                    DB2014.2
143300         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
143400     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
143500     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
143600 QUAL-TEST-5-INIT.                                                DB2014.2
143700     MOVE "QUAL-TEST-5" TO PAR-NAME.                              DB2014.2
143800     MOVE "QUALIFIED SUBSC ITEM" TO FEATURE.                      DB2014.2
143900     MOVE SPACES TO ITEM-1.                                       DB2014.2
144000     MOVE 0 TO KEY-1.                                             DB2014.2
144100 QUAL-TEST-5.                                                     DB2014.2
144200     MOVE "F" TO AB1 OF A1 (3).                                   DB2014.2
144300     IF KEY-1 IS EQUAL TO 1                                       DB2014.2
144400         PERFORM PASS                                             DB2014.2
144500         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2014.2
144600         PERFORM QUAL-TEST-WRITE                                  DB2014.2
144700         GO TO QUAL-TEST-5A                                       DB2014.2
144800     ELSE  PERFORM FAIL                                           DB2014.2
144900         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2014.2
145000         PERFORM QUAL-TEST-WRITE                                  DB2014.2
145100         PERFORM DELETE-QUAL-TEST-5-SUBTESTS                      DB2014.2
145200         GO TO END-OF-DB201A.                                     DB2014.2
145300 QUAL-TEST-5-DELETE.                                              DB2014.2
145400     PERFORM DE-LETE.                                             DB2014.2
145500     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
145600     PERFORM DELETE-QUAL-TEST-5-SUBTESTS.                         DB2014.2
145700     GO TO END-OF-DB201A.                                         DB2014.2
145800 DELETE-QUAL-TEST-5-SUBTESTS.                                     DB2014.2
145900     MOVE "QUAL-TEST-5A" TO PAR-NAME.                             DB2014.2
146000     PERFORM DE-LETE.                                             DB2014.2
146100     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
146200     MOVE "QUAL-TEST-5B" TO PAR-NAME.                             DB2014.2
146300     PERFORM DE-LETE.                                             DB2014.2
146400     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
146500     MOVE "QUAL-TEST-5C" TO PAR-NAME.                             DB2014.2
146600     PERFORM DE-LETE.                                             DB2014.2
146700     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
146800     MOVE "QUAL-TEST-5D" TO PAR-NAME.                             DB2014.2
146900     PERFORM DE-LETE.                                             DB2014.2
147000     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
147100     MOVE "QUAL-TEST-5E" TO PAR-NAME.                             DB2014.2
147200     PERFORM DE-LETE.                                             DB2014.2
147300     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
147400 QUAL-TEST-5A.                                                    DB2014.2
147500     MOVE "QUAL-TEST-5A" TO PAR-NAME.                             DB2014.2
147600     IF NAME-1 IS EQUAL TO "AB1 OF A1"  OR                        DB2014.2
147700        NAME-1 IS EQUAL TO "AB1 IN A1"                            DB2014.2
147800         PERFORM PASS                                             DB2014.2
147900     ELSE  PERFORM FAIL                                           DB2014.2
148000         MOVE "AB1 OF(IN) A1" TO CORRECT-A                        DB2014.2
148100         MOVE NAME-1 TO COMPUTED-A.                               DB2014.2
148200     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2014.2
148300     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
148400 QUAL-TEST-5B.                                                    DB2014.2
148500     MOVE "QUAL-TEST-5B" TO PAR-NAME.                             DB2014.2
148600     IF CONTENTS-1 IS EQUAL TO "F"                                DB2014.2
148700         PERFORM PASS                                             DB2014.2
148800     ELSE  PERFORM FAIL                                           DB2014.2
148900         MOVE "F" TO CORRECT-A                                    DB2014.2
149000         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2014.2
149100     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2014.2
149200     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
149300 QUAL-TEST-5C.                                                    DB2014.2
149400     MOVE "QUAL-TEST-5C" TO PAR-NAME.                             DB2014.2
149500     IF SUB-1-1 IS EQUAL TO "0003"                                DB2014.2
149600         PERFORM PASS                                             DB2014.2
149700     ELSE  PERFORM FAIL                                           DB2014.2
149800         MOVE "0003" TO CORRECT-A                                 DB2014.2
149900         MOVE SUB-1-1 TO COMPUTED-A.                              DB2014.2
150000     MOVE "DEBUG-SUB-1" TO RE-MARK.                               DB2014.2
150100     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
150200 QUAL-TEST-5D.                                                    DB2014.2
150300     MOVE "QUAL-TEST-5D" TO PAR-NAME.                             DB2014.2
150400     PERFORM DE-LETE.                                             DB2014.2
150500     PERFORM QUAL-TEST-WRITE.                                     DB2014.2
150600 QUAL-TEST-5E.                                                    DB2014.2
150700     MOVE "QUAL-TEST-5E" TO PAR-NAME.                             DB2014.2
150800     PERFORM DE-LETE.                                             DB2014.2
150900 QUAL-TEST-WRITE.                                                 DB2014.2
151000     PERFORM PRINT-DETAIL.                                        DB2014.2
151100 END-OF-DB201A.                                                   DB2014.2
151200     EXIT.                                                        DB2014.2
151300 CCVS-EXIT SECTION.                                               DB2014.2
151400 CCVS-999999.                                                     DB2014.2
151500     GO TO CLOSE-FILES.                                           DB2014.2
      *END-OF,DB201A                                                                  
