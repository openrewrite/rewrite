      *HEADER,COBOL,SG204A                                                            
000100 IDENTIFICATION DIVISION.                                         SG2044.2
000200 PROGRAM-ID.                                                      SG2044.2
000300     SG204A.                                                      SG2044.2
000400 AUTHOR.                                                          SG2044.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG2044.2
000600 INSTALLATION.                                                    SG2044.2
000700     GENERAL SERVICES ADMINISTRATION                              SG2044.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG2044.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG2044.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG2044.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG2044.2
001200                                                                  SG2044.2
001300     PHONE   (703) 756-6153                                       SG2044.2
001400                                                                  SG2044.2
001500     " HIGH       ".                                              SG2044.2
001600 DATE-WRITTEN.                                                    SG2044.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG2044.2
001800     CREATION DATE     /    VALIDATION DATE                       SG2044.2
001900     "4.2 ".                                                      SG2044.2
002000 SECURITY.                                                        SG2044.2
002100     NONE.                                                        SG2044.2
002200     THIS PROGRAM CONTAINS 3 SORTS USING NUMERIC OR ALPHABETIC    SG2044.2
002300     KEYS - BUT NOT BOTH IN THE SAME KEY  DUE TO DIFFERING        SG2044.2
002400     COLLATING SEQUENCES AMONG COMPUTERS. EXTERNAL FILES ARE      SG2044.2
002500     GENERATED INTERNALLY FOR SUBSEQUENT USE. THE SELECT CLAUSE   SG2044.2
002600     IS HIGHLY DEPENDENT ON HARDWARE. THE USER SHOULD EXERCISE THESG2044.2
002700     VARIOUS OPTIONS OF HARDWARE ASSIGNMENTS TO THE EXTENT THEY   SG2044.2
002800     ARE AVAILABLE. THE SORT OF A MULTI-REEL FILE IS EXERCISED    SG2044.2
002900     IN PROGRAM ST202. HOWEVER THE EXERCISE OF THE "FOR MULTIPLE  SG2044.2
003000     REEL-UNIT" OF THE GIVING OPTION IS NOT DUE TO THE INDETER-   SG2044.2
003100     MINATE LENGTH OF SUCH A FILE (E.G. RECORDING DENSITY OR SIZE SG2044.2
003200     OF UNIT) AND PROCESSING COST. SORT INPUT-OUTPUT OPTIONS      SG2044.2
003300     WILL BE EXERCISED AS FOLLOWS.                                SG2044.2
003400         SORT 1  USING        GIVING                              SG2044.2
003500         SORT 2  INPUT PROC   GIVING                              SG2044.2
003600         SORT 3  INPUT PROC   OUTPUT PROC                         SG2044.2
003700     THIS PROGRAM ALSO EXERCISES THE "SAME RECORD AREA" CLAUSE.   SG2044.2
003800                                                                  SG2044.2
003900 ENVIRONMENT DIVISION.                                            SG2044.2
004000 CONFIGURATION SECTION.                                           SG2044.2
004100 SOURCE-COMPUTER.                                                 SG2044.2
004200     XXXXX082.                                                    SG2044.2
004300 OBJECT-COMPUTER.                                                 SG2044.2
004400     XXXXX083.                                                    SG2044.2
004500 INPUT-OUTPUT SECTION.                                            SG2044.2
004600 FILE-CONTROL.                                                    SG2044.2
004700     SELECT PRINT-FILE ASSIGN TO                                  SG2044.2
004800     XXXXX055.                                                    SG2044.2
004900     SELECT SORT1 ASSIGN TO                                       SG2044.2
005000     XXXXX027.                                                    SG2044.2
005100     SELECT SORT2 ASSIGN TO                                       SG2044.2
005200     XXXXX028.                                                    SG2044.2
005300     SELECT SORT3 ASSIGN TO                                       SG2044.2
005400     XXXXX029.                                                    SG2044.2
005500     SELECT FILE1 ASSIGN TO                                       SG2044.2
005600     XXXXX001.                                                    SG2044.2
005700     SELECT FILE2 ASSIGN TO                                       SG2044.2
005800     XXXXX014                                                     SG2044.2
005900         RESERVE 1 AREA.                                          SG2044.2
006000     SELECT FILE3 ASSIGN TO                                       SG2044.2
006100     XXXXX015                                                     SG2044.2
006200         RESERVE 4 AREAS.                                         SG2044.2
006300 I-O-CONTROL.                                                     SG2044.2
006400     SAME RECORD AREA FOR SORT1 SORT2                             SG2044.2
006500     SAME RECORD AREA FOR SORT3 FILE3.                            SG2044.2
006600 DATA DIVISION.                                                   SG2044.2
006700 FILE SECTION.                                                    SG2044.2
006800 FD  PRINT-FILE                                                   SG2044.2
006900     LABEL RECORDS                                                SG2044.2
007000     XXXXX084                                                     SG2044.2
007100     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG2044.2
007200 01  PRINT-REC PICTURE X(120).                                    SG2044.2
007300 01  DUMMY-RECORD PICTURE X(120).                                 SG2044.2
007400 FD  FILE1                                                        SG2044.2
007500     LABEL RECORDS ARE STANDARD                                   SG2044.2
007600     VALUE OF                                                     SG2044.2
007700     XXXXX074                                                     SG2044.2
007800     IS                                                           SG2044.2
007900     XXXXX075                                                     SG2044.2
008000     BLOCK CONTAINS 10 RECORDS                                    SG2044.2
008100     DATA RECORD R1.                                              SG2044.2
008200 01  R1.                                                          SG2044.2
008300     02  FILLER PICTURE X(120).                                   SG2044.2
008400 FD  FILE2                                                        SG2044.2
008500     LABEL RECORDS ARE STANDARD                                   SG2044.2
008600     VALUE OF                                                     SG2044.2
008700     XXXXX074                                                     SG2044.2
008800     IS                                                           SG2044.2
008900     XXXXX076                                                     SG2044.2
009000     BLOCK CONTAINS 10 RECORDS                                    SG2044.2
009100     DATA RECORD R2.                                              SG2044.2
009200 01  R2.                                                          SG2044.2
009300     02  R2-KEYS.                                                 SG2044.2
009400         03  R2-1 PICTURE 999.                                    SG2044.2
009500         03  R2-2 PICTURE AA.                                     SG2044.2
009600         03  R2-3 PICTURE AA.                                     SG2044.2
009700     02  FILLER PICTURE X(113).                                   SG2044.2
009800 FD  FILE3                                                        SG2044.2
009900     BLOCK CONTAINS 10 RECORDS                                    SG2044.2
010000     LABEL RECORDS ARE STANDARD                                   SG2044.2
010100     VALUE OF                                                     SG2044.2
010200     XXXXX074                                                     SG2044.2
010300     IS                                                           SG2044.2
010400     XXXXX077                                                     SG2044.2
010500     DATA RECORD IS R3.                                           SG2044.2
010600 01  R3.                                                          SG2044.2
010700     02  R3-KEYS.                                                 SG2044.2
010800         03  R3-1 PICTURE 999.                                    SG2044.2
010900         03  R3-2 PICTURE AA.                                     SG2044.2
011000         03  R3-3 PICTURE AA.                                     SG2044.2
011100         03  R3-4 PICTURE 9999.                                   SG2044.2
011200     02  FILLER PICTURE X(109).                                   SG2044.2
011300 SD  SORT1                                                        SG2044.2
011400     RECORD CONTAINS 120 CHARACTERS                               SG2044.2
011500     DATA RECORD IS S1.                                           SG2044.2
011600 01  S1.                                                          SG2044.2
011700     02  S1-KEYS.                                                 SG2044.2
011800         03  S1-1 PICTURE 999.                                    SG2044.2
011900         03  S1-2 PICTURE AA.                                     SG2044.2
012000     02  FILLER PICTURE X(115).                                   SG2044.2
012100 SD  SORT2                                                        SG2044.2
012200     RECORD 120                                                   SG2044.2
012300     DATA RECORD IS S2.                                           SG2044.2
012400 01  S2.                                                          SG2044.2
012500     02  S2-KEYS.                                                 SG2044.2
012600         03  S2-1 PICTURE 999.                                    SG2044.2
012700         03  S2-2 PICTURE AA.                                     SG2044.2
012800         03  S2-3 PICTURE AA.                                     SG2044.2
012900     02  FILLER PICTURE X(113).                                   SG2044.2
013000 SD  SORT3                                                        SG2044.2
013100     RECORD 120 CHARACTERS                                        SG2044.2
013200     DATA RECORD S3.                                              SG2044.2
013300 01  S3.                                                          SG2044.2
013400     02  S3-KEYS.                                                 SG2044.2
013500         03  S3-1 PICTURE 999.                                    SG2044.2
013600         03  S3-2 PICTURE AA.                                     SG2044.2
013700         03  S3-3 PICTURE AA.                                     SG2044.2
013800         03  S3-4 PICTURE 9999.                                   SG2044.2
013900     02  FILLER PICTURE X(109).                                   SG2044.2
014000 WORKING-STORAGE SECTION.                                         SG2044.2
014100 77  SUBSCRIPT-1 PICTURE  99 COMPUTATIONAL VALUE ZERO.            SG2044.2
014200 77  C0 PICTURE  99 COMPUTATIONAL VALUE ZERO.                     SG2044.2
014300 77  C1 PICTURE  99 COMPUTATIONAL VALUE 1.                        SG2044.2
014400 77  CA PICTURE A VALUE "A".                                      SG2044.2
014500 77  CB PICTURE A VALUE "B".                                      SG2044.2
014600 01  ALPHA-TABLE.                                                 SG2044.2
014700     02  ALPHA-TAB PICTURE IS A(25) VALUE IS "ABCDEFGHIJKLMNPQRSTUSG2044.2
014800-    "VWXYZ".                                                     SG2044.2
014900     02  ALPHA-TBL REDEFINES ALPHA-TAB PICTURE A OCCURS 25 TIMES. SG2044.2
015000 01  W-KEYS.                                                      SG2044.2
015100     02  W-S3-KEYS.                                               SG2044.2
015200         03  W-S2-KEYS.                                           SG2044.2
015300             04  W-S1-KEYS.                                       SG2044.2
015400                 05  S1-1W PICTURE 999 VALUE 567.                 SG2044.2
015500                 05  S1-2W.                                       SG2044.2
015600                     06  S1-2W-A PICTURE A.                       SG2044.2
015700                     06  S1-2W-B PICTURE A.                       SG2044.2
015800             04  S2-3W.                                           SG2044.2
015900                 05  S2-3W-A PICTURE A.                           SG2044.2
016000                 05  S2-3W-B PICTURE A.                           SG2044.2
016100         03  S3-4W PICTURE 9999 VALUE 7051.                       SG2044.2
016200 01  TEST-RESULTS.                                                SG2044.2
016300     02 FILLER                    PICTURE X VALUE SPACE.          SG2044.2
016400     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG2044.2
016500     02 FILLER                    PICTURE X VALUE SPACE.          SG2044.2
016600     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG2044.2
016700     02 FILLER                    PICTURE X  VALUE SPACE.         SG2044.2
016800     02  PAR-NAME.                                                SG2044.2
016900       03 FILLER PICTURE X(12) VALUE SPACE.                       SG2044.2
017000       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG2044.2
017100       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG2044.2
017200       03 FILLER PIC X(5) VALUE SPACE.                            SG2044.2
017300     02 FILLER PIC X(10) VALUE SPACE.                             SG2044.2
017400     02 RE-MARK PIC X(61).                                        SG2044.2
017500 01  TEST-COMPUTED.                                               SG2044.2
017600     02 FILLER PIC X(30) VALUE SPACE.                             SG2044.2
017700     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG2044.2
017800     02 COMPUTED-X.                                               SG2044.2
017900     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG2044.2
018000     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG2044.2
018100     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG2044.2
018200     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG2044.2
018300     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG2044.2
018400     03       CM-18V0 REDEFINES COMPUTED-A.                       SG2044.2
018500         04 COMPUTED-18V0                   PICTURE -9(18).       SG2044.2
018600         04 FILLER                          PICTURE X.            SG2044.2
018700     03 FILLER PIC X(50) VALUE SPACE.                             SG2044.2
018800 01  TEST-CORRECT.                                                SG2044.2
018900     02 FILLER PIC X(30) VALUE SPACE.                             SG2044.2
019000     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG2044.2
019100     02 CORRECT-X.                                                SG2044.2
019200     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG2044.2
019300     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG2044.2
019400     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG2044.2
019500     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG2044.2
019600     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG2044.2
019700     03      CR-18V0 REDEFINES CORRECT-A.                         SG2044.2
019800         04 CORRECT-18V0                    PICTURE -9(18).       SG2044.2
019900         04 FILLER                          PICTURE X.            SG2044.2
020000     03 FILLER PIC X(50) VALUE SPACE.                             SG2044.2
020100 01  CCVS-C-1.                                                    SG2044.2
020200     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG2044.2
020300-    "SS  PARAGRAPH-NAME                                          SG2044.2
020400-    "        REMARKS".                                           SG2044.2
020500     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG2044.2
020600 01  CCVS-C-2.                                                    SG2044.2
020700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2044.2
020800     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG2044.2
020900     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG2044.2
021000     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG2044.2
021100     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG2044.2
021200 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG2044.2
021300 01  REC-CT PICTURE 99 VALUE ZERO.                                SG2044.2
021400 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG2044.2
021500 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG2044.2
021600 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG2044.2
021700 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG2044.2
021800 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG2044.2
021900 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG2044.2
022000 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG2044.2
022100 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG2044.2
022200 01  CCVS-H-1.                                                    SG2044.2
022300     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG2044.2
022400     02 FILLER PICTURE X(67) VALUE                                SG2044.2
022500     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG2044.2
022600-    " SYSTEM".                                                   SG2044.2
022700     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG2044.2
022800 01  CCVS-H-2.                                                    SG2044.2
022900     02 FILLER PICTURE X(52) VALUE IS                             SG2044.2
023000     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG2044.2
023100     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG2044.2
023200     02 TEST-ID PICTURE IS X(9).                                  SG2044.2
023300     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG2044.2
023400 01  CCVS-H-3.                                                    SG2044.2
023500     02  FILLER PICTURE X(34) VALUE                               SG2044.2
023600     " FOR OFFICIAL USE ONLY    ".                                SG2044.2
023700     02  FILLER PICTURE X(58) VALUE                               SG2044.2
023800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG2044.2
023900     02  FILLER PICTURE X(28) VALUE                               SG2044.2
024000     "  COPYRIGHT   1974 ".                                       SG2044.2
024100 01  CCVS-E-1.                                                    SG2044.2
024200     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG2044.2
024300     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG2044.2
024400     02 ID-AGAIN PICTURE IS X(9).                                 SG2044.2
024500     02 FILLER PICTURE X(45) VALUE IS                             SG2044.2
024600     " NTIS DISTRIBUTION COBOL 74".                               SG2044.2
024700 01  CCVS-E-2.                                                    SG2044.2
024800     02  FILLER                   PICTURE X(31)  VALUE            SG2044.2
024900     SPACE.                                                       SG2044.2
025000     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG2044.2
025100     02 CCVS-E-2-2.                                               SG2044.2
025200         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG2044.2
025300         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG2044.2
025400         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG2044.2
025500 01  CCVS-E-3.                                                    SG2044.2
025600     02  FILLER PICTURE X(22) VALUE                               SG2044.2
025700     " FOR OFFICIAL USE ONLY".                                    SG2044.2
025800     02  FILLER PICTURE X(12) VALUE SPACE.                        SG2044.2
025900     02  FILLER PICTURE X(58) VALUE                               SG2044.2
026000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG2044.2
026100     02  FILLER PICTURE X(13) VALUE SPACE.                        SG2044.2
026200     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG2044.2
026300 01  CCVS-E-4.                                                    SG2044.2
026400     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG2044.2
026500     02 FILLER PIC XXXX VALUE " OF ".                             SG2044.2
026600     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG2044.2
026700     02 FILLER PIC X(40) VALUE                                    SG2044.2
026800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG2044.2
026900 01  XXINFO.                                                      SG2044.2
027000     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG2044.2
027100     02 INFO-TEXT.                                                SG2044.2
027200     04 FILLER PIC X(20) VALUE SPACE.                             SG2044.2
027300     04 XXCOMPUTED PIC X(20).                                     SG2044.2
027400     04 FILLER PIC X(5) VALUE SPACE.                              SG2044.2
027500     04 XXCORRECT PIC X(20).                                      SG2044.2
027600 01  HYPHEN-LINE.                                                 SG2044.2
027700     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG2044.2
027800     02 FILLER PICTURE IS X(65) VALUE IS "************************SG2044.2
027900-    "*****************************************".                 SG2044.2
028000     02 FILLER PICTURE IS X(54) VALUE IS "************************SG2044.2
028100-    "******************************".                            SG2044.2
028200 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG2044.2
028300     "SG204A".                                                    SG2044.2
028400 PROCEDURE DIVISION.                                              SG2044.2
028500 CCVS1 SECTION.                                                   SG2044.2
028600 OPEN-FILES.                                                      SG2044.2
028700     OPEN     OUTPUT PRINT-FILE.                                  SG2044.2
028800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG2044.2
028900     MOVE    SPACE TO TEST-RESULTS.                               SG2044.2
029000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG2044.2
029100     GO TO CCVS1-EXIT.                                            SG2044.2
029200 CLOSE-FILES.                                                     SG2044.2
029300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG2044.2
029400 TERMINATE-CCVS.                                                  SG2044.2
029500*    EXIT PROGRAM.                                                SG2044.2
029600*TERMINATE-CALL.                                                  SG2044.2
029700     STOP     RUN.                                                SG2044.2
029800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG2044.2
029900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG2044.2
030000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG2044.2
030100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG2044.2
030200     MOVE "****TEST DELETED****" TO RE-MARK.                      SG2044.2
030300 PRINT-DETAIL.                                                    SG2044.2
030400     IF REC-CT NOT EQUAL TO ZERO                                  SG2044.2
030500             MOVE "." TO PARDOT-X                                 SG2044.2
030600             MOVE REC-CT TO DOTVALUE.                             SG2044.2
030700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG2044.2
030800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG2044.2
030900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG2044.2
031000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG2044.2
031100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG2044.2
031200     MOVE SPACE TO CORRECT-X.                                     SG2044.2
031300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG2044.2
031400     MOVE     SPACE TO RE-MARK.                                   SG2044.2
031500 HEAD-ROUTINE.                                                    SG2044.2
031600     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2044.2
031700     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG2044.2
031800     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG2044.2
031900 COLUMN-NAMES-ROUTINE.                                            SG2044.2
032000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2044.2
032100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2044.2
032200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG2044.2
032300 END-ROUTINE.                                                     SG2044.2
032400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG2044.2
032500 END-RTN-EXIT.                                                    SG2044.2
032600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG2044.2
032700 END-ROUTINE-1.                                                   SG2044.2
032800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG2044.2
032900      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG2044.2
033000      ADD PASS-COUNTER TO ERROR-HOLD.                             SG2044.2
033100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG2044.2
033200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG2044.2
033300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG2044.2
033400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG2044.2
033500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG2044.2
033600  END-ROUTINE-12.                                                 SG2044.2
033700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG2044.2
033800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG2044.2
033900         MOVE "NO " TO ERROR-TOTAL                                SG2044.2
034000         ELSE                                                     SG2044.2
034100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG2044.2
034200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG2044.2
034300     PERFORM WRITE-LINE.                                          SG2044.2
034400 END-ROUTINE-13.                                                  SG2044.2
034500     IF DELETE-CNT IS EQUAL TO ZERO                               SG2044.2
034600         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG2044.2
034700         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG2044.2
034800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG2044.2
034900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2044.2
035000      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG2044.2
035100          MOVE "NO " TO ERROR-TOTAL                               SG2044.2
035200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG2044.2
035300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG2044.2
035400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG2044.2
035500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG2044.2
035600 WRITE-LINE.                                                      SG2044.2
035700     ADD 1 TO RECORD-COUNT.                                       SG2044.2
035800*    IF RECORD-COUNT GREATER 50                                   SG2044.2
035900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG2044.2
036000*        MOVE SPACE TO DUMMY-RECORD                               SG2044.2
036100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG2044.2
036200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG2044.2
036300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG2044.2
036400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG2044.2
036500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG2044.2
036600*        MOVE ZERO TO RECORD-COUNT.                               SG2044.2
036700     PERFORM WRT-LN.                                              SG2044.2
036800 WRT-LN.                                                          SG2044.2
036900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG2044.2
037000     MOVE SPACE TO DUMMY-RECORD.                                  SG2044.2
037100 BLANK-LINE-PRINT.                                                SG2044.2
037200     PERFORM WRT-LN.                                              SG2044.2
037300 FAIL-ROUTINE.                                                    SG2044.2
037400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG2044.2
037500     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG2044.2
037600     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG2044.2
037700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2044.2
037800     GO TO FAIL-ROUTINE-EX.                                       SG2044.2
037900 FAIL-ROUTINE-WRITE.                                              SG2044.2
038000     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG2044.2
038100     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG2044.2
038200 FAIL-ROUTINE-EX. EXIT.                                           SG2044.2
038300 BAIL-OUT.                                                        SG2044.2
038400     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG2044.2
038500     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG2044.2
038600 BAIL-OUT-WRITE.                                                  SG2044.2
038700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG2044.2
038800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG2044.2
038900 BAIL-OUT-EX. EXIT.                                               SG2044.2
039000 CCVS1-EXIT.                                                      SG2044.2
039100     EXIT.                                                        SG2044.2
039200 P1-CREATE-F1.                                                    SG2044.2
039300     OPEN    OUTPUT FILE1.                                        SG2044.2
039400     MOVE CA TO S1-2W-A.                                          SG2044.2
039500     MOVE CB TO S2-3W-A.                                          SG2044.2
039600 P2-CREATE-F1.                                                    SG2044.2
039700     PERFORM P4-CREATE-F1 2 TIMES.                                SG2044.2
039800 P3-CREATE-F1.                                                    SG2044.2
039900     MOVE CA TO S2-3W-A.                                          SG2044.2
040000     PERFORM P4-CREATE-F1 2 TIMES.                                SG2044.2
040100     CLOSE FILE1.                                                 SG2044.2
040200     GO TO FIRST-SORT.                                            SG2044.2
040300 P4-CREATE-F1.                                                    SG2044.2
040400     MOVE C0 TO SUBSCRIPT-1.                                      SG2044.2
040500     PERFORM P5-CREATE-F1 25 TIMES.                               SG2044.2
040600 P5-CREATE-F1.                                                    SG2044.2
040700     ADD C1 TO SUBSCRIPT-1.                                       SG2044.2
040800     SUBTRACT C1 FROM S3-4W.                                      SG2044.2
040900     MOVE ALPHA-TBL (SUBSCRIPT-1) TO S1-2W-B S2-3W-B.             SG2044.2
041000     MOVE W-S3-KEYS TO R1.                                        SG2044.2
041100     WRITE R1.                                                    SG2044.2
041200 F1-NOTE.                                                         SG2044.2
041300*    NOTE.                                                        SG2044.2
041400*      KEY-1 WILL BE 567 IN ALL RECORDS.                          SG2044.2
041500*      KEY-2 WILL BE >A> IN FIRST LETTER WITH 4 OCCURRENCES OF THESG2044.2
041600*        ALPHABET IN THE SECOND LETTER.                           SG2044.2
041700*      KEY-3 WILL BE >A> OR >B> IN FIRST LETTER WITH 2 OCCURRENCESSG2044.2
041800*        OF THE ALPHABET FOR EACH IN THE SECOND LETTER.           SG2044.2
041900*      KEY-4 WILL VARY FROM 7050 THRU 6951.                       SG2044.2
042000*      THE LETTER "O" HAS BEEN OMITTED.                           SG2044.2
042100 SRT-1 SECTION 69.                                                SG2044.2
042200 FIRST-SORT.                                                      SG2044.2
042300     SORT SORT1                                                   SG2044.2
042400         ON DESCENDING KEY S1-1                                   SG2044.2
042500         ON ASCENDING KEY S1-2                                    SG2044.2
042600         USING FILE1                                              SG2044.2
042700         GIVING FILE2.                                            SG2044.2
042800*    NOTE SORT STATEMENT WITH ALL OPTIONAL WORDS.                 SG2044.2
042900*    NOTE OUTPUT WILL BE TESTED IN THE FOLLOWING INPUT PROCEDURE. SG2044.2
043000 SRT-2 SECTION 48.                                                SG2044.2
043100 SECOND-SORT.                                                     SG2044.2
043200     SORT SORT2                                                   SG2044.2
043300         ASCENDING S2-1                                           SG2044.2
043400         DESCENDING S2-2                                          SG2044.2
043500         ASCENDING S2-3                                           SG2044.2
043600         INPUT PROCEDURE SRT-2-INPUT                              SG2044.2
043700         GIVING FILE3.                                            SG2044.2
043800*      NOTE SORT STATEMENT WITH ALL OPTIONAL WORDS OMITTED.       SG2044.2
043900     GO TO SRT-3.                                                 SG2044.2
044000 SRT-2-INPUT SECTION 74.                                          SG2044.2
044100 OPEN-1.                                                          SG2044.2
044200     OPEN     INPUT FILE2.                                        SG2044.2
044300     MOVE     "SORT, INPUT PROC" TO FEATURE.                      SG2044.2
044400 SORT-TEST-1.                                                     SG2044.2
044500     PERFORM  READ-RELEASE-FILE2.                                 SG2044.2
044600     IF       W-S1-KEYS EQUAL TO "567AA"                          SG2044.2
044700              PERFORM PASS-1 GO TO SORT-WRITE-1.                  SG2044.2
044800     GO       TO SORT-FAIL-1.                                     SG2044.2
044900 SORT-DELETE-1.                                                   SG2044.2
045000     PERFORM  DE-LETE-1.                                          SG2044.2
045100     GO       TO SORT-WRITE-1.                                    SG2044.2
045200 SORT-FAIL-1.                                                     SG2044.2
045300     MOVE     W-S1-KEYS TO COMPUTED-A.                            SG2044.2
045400     MOVE     "567AA" TO CORRECT-A.                               SG2044.2
045500     PERFORM  FAIL-1.                                             SG2044.2
045600 SORT-WRITE-1.                                                    SG2044.2
045700     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         SG2044.2
045800     PERFORM  PRINT-DETAIL-1.                                     SG2044.2
045900 SORT-TEST-2.                                                     SG2044.2
046000     PERFORM  READ-RELEASE-FILE2 35 TIMES.                        SG2044.2
046100     IF       W-S1-KEYS EQUAL TO "567AI"                          SG2044.2
046200              PERFORM PASS-1 GO TO SORT-WRITE-2.                  SG2044.2
046300     GO       TO SORT-FAIL-2.                                     SG2044.2
046400 SORT-DELETE-2.                                                   SG2044.2
046500     PERFORM  DE-LETE-1.                                          SG2044.2
046600     GO       TO SORT-WRITE-2.                                    SG2044.2
046700 SORT-FAIL-2.                                                     SG2044.2
046800     MOVE     W-S1-KEYS TO COMPUTED-A.                            SG2044.2
046900     MOVE     "567AI" TO CORRECT-A.                               SG2044.2
047000     PERFORM  FAIL-1.                                             SG2044.2
047100 SORT-WRITE-2.                                                    SG2044.2
047200     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         SG2044.2
047300     PERFORM  PRINT-DETAIL-1.                                     SG2044.2
047400 SORT-TEST-3.                                                     SG2044.2
047500     PERFORM  READ-RELEASE-FILE2 35 TIMES.                        SG2044.2
047600     IF       W-S1-KEYS EQUAL TO "567AS"                          SG2044.2
047700              PERFORM PASS-1 GO TO SORT-WRITE-3.                  SG2044.2
047800     GO       TO SORT-FAIL-3.                                     SG2044.2
047900 SORT-DELETE-3.                                                   SG2044.2
048000     PERFORM  DE-LETE-1.                                          SG2044.2
048100     GO       TO SORT-WRITE-3.                                    SG2044.2
048200 SORT-FAIL-3.                                                     SG2044.2
048300     MOVE     W-S1-KEYS TO COMPUTED-A.                            SG2044.2
048400     MOVE     "567AS" TO CORRECT-A.                               SG2044.2
048500     PERFORM  FAIL-1.                                             SG2044.2
048600 SORT-WRITE-3.                                                    SG2044.2
048700     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         SG2044.2
048800     PERFORM  PRINT-DETAIL-1.                                     SG2044.2
048900 SORT-TEST-4.                                                     SG2044.2
049000     PERFORM  READ-RELEASE-FILE2 29 TIMES.                        SG2044.2
049100     IF       W-S1-KEYS EQUAL TO "567AZ"                          SG2044.2
049200              PERFORM PASS-1 GO TO SORT-WRITE-4.                  SG2044.2
049300     GO       TO SORT-FAIL-4.                                     SG2044.2
049400 SORT-DELETE-4.                                                   SG2044.2
049500     PERFORM  DE-LETE-1.                                          SG2044.2
049600     GO       TO SORT-WRITE-4.                                    SG2044.2
049700 SORT-FAIL-4.                                                     SG2044.2
049800     MOVE     W-S1-KEYS TO COMPUTED-A.                            SG2044.2
049900     MOVE     "567AZ" TO CORRECT-A.                               SG2044.2
050000     PERFORM  FAIL-1.                                             SG2044.2
050100 SORT-WRITE-4.                                                    SG2044.2
050200     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         SG2044.2
050300     PERFORM  PRINT-DETAIL-1.                                     SG2044.2
050400 CLOSE-1.                                                         SG2044.2
050500     CLOSE    FILE2.                                              SG2044.2
050600     GO       TO EXIT-1.                                          SG2044.2
050700 READ-RELEASE-FILE2.                                              SG2044.2
050800     READ     FILE2 AT END GO TO TERMINAL-1.                      SG2044.2
050900     MOVE     R2 TO W-S3-KEYS.                                    SG2044.2
051000     RELEASE  S2 FROM R2.                                         SG2044.2
051100 TERMINAL-1.                                                      SG2044.2
051200     PERFORM  FAIL-1.                                             SG2044.2
051300     MOVE     "TERMINAL-1" TO PAR-NAME.                           SG2044.2
051400     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               SG2044.2
051500     PERFORM  PRINT-DETAIL-1.                                     SG2044.2
051600     MOVE     SPACE TO FEATURE.                                   SG2044.2
051700     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              SG2044.2
051800     PERFORM  PRINT-DETAIL-1.                                     SG2044.2
051900     MOVE     "LAST SUCCESSFUL TEST." TO RE-MARK.                 SG2044.2
052000     PERFORM  PRINT-DETAIL-1.                                     SG2044.2
052100     GO       TO CLOSE-1.                                         SG2044.2
052200 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       SG2044.2
052300 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         SG2044.2
052400 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        SG2044.2
052500 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.        SG2044.2
052600     MOVE "****TEST DELETED****" TO RE-MARK.                      SG2044.2
052700 PRINT-DETAIL-1.                                                  SG2044.2
052800     IF REC-CT NOT EQUAL TO ZERO                                  SG2044.2
052900             MOVE "." TO PARDOT-X                                 SG2044.2
053000             MOVE REC-CT TO DOTVALUE.                             SG2044.2
053100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    SG2044.2
053200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             SG2044.2
053300        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             SG2044.2
053400          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             SG2044.2
053500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG2044.2
053600     MOVE SPACE TO CORRECT-X.                                     SG2044.2
053700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG2044.2
053800     MOVE     SPACE TO RE-MARK.                                   SG2044.2
053900 WRITE-LINE-1.                                                    SG2044.2
054000     ADD 1 TO RECORD-COUNT.                                       SG2044.2
054100*    IF RECORD-COUNT GREATER 50                                   SG2044.2
054200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG2044.2
054300*        MOVE SPACE TO DUMMY-RECORD                               SG2044.2
054400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG2044.2
054500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           SG2044.2
054600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   SG2044.2
054700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        SG2044.2
054800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG2044.2
054900*        MOVE ZERO TO RECORD-COUNT.                               SG2044.2
055000     PERFORM WRT-LN-1.                                            SG2044.2
055100 WRT-LN-1.                                                        SG2044.2
055200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG2044.2
055300     MOVE SPACE TO DUMMY-RECORD.                                  SG2044.2
055400 BLANK-LINE-PRINT-1.                                              SG2044.2
055500     PERFORM WRT-LN-1.                                            SG2044.2
055600 FAIL-ROUTINE-1.                                                  SG2044.2
055700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     SG2044.2
055800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      SG2044.2
055900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG2044.2
056000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   SG2044.2
056100     GO TO FAIL-ROUTINE-EX-1.                                     SG2044.2
056200 FAIL-RTN-WRITE-1.                                                SG2044.2
056300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         SG2044.2
056400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. SG2044.2
056500 FAIL-ROUTINE-EX-1. EXIT.                                         SG2044.2
056600 BAIL-OUT-1.                                                      SG2044.2
056700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     SG2044.2
056800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             SG2044.2
056900 BAIL-OUT-WRITE-1.                                                SG2044.2
057000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG2044.2
057100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   SG2044.2
057200 BAIL-OUT-EX-1. EXIT.                                             SG2044.2
057300 EXIT-1.                                                          SG2044.2
057400     EXIT.                                                        SG2044.2
057500 SRT-3 SECTION.                                                   SG2044.2
057600 THIRD-SORT.                                                      SG2044.2
057700     SORT SORT3                                                   SG2044.2
057800         ON DESCENDING KEY S3-1 S3-2 S3-3                         SG2044.2
057900         ASCENDING S3-4                                           SG2044.2
058000       INPUT PROCEDURE IS SRT3-INPUT                              SG2044.2
058100       OUTPUT PROCEDURE SRT3-OUTPUT-1 THRU SRT3-OUTPUT-2.         SG2044.2
058200 NOTE-SORT-3.                                                     SG2044.2
058300*    NOTE SORT STATEMENT WITH INCLUSION-OMISSION OF OPTIONAL      SG2044.2
058400*        WORDS AND THRU OPTION. THE OUTPUT OF SRT-2 IS TESTED     SG2044.2
058500*        IN THE INPUT PROCEDURE OF THIS (THIRD) SORT.  THE OUTPUT SG2044.2
058600*        OF THE THIRD SORT IS TESTED IN THE OUTPUT PROCEDURE      SG2044.2
058700*        WITHOUT THE GENERATION OF AN OUTPUT FILE.                SG2044.2
058800 END-FIRST-PROGRAM.                                               SG2044.2
058900     GO TO CCVS-EXIT.                                             SG2044.2
059000 SRT3-INPUT SECTION 49.                                           SG2044.2
059100 OPEN-2.                                                          SG2044.2
059200     OPEN     INPUT FILE3.                                        SG2044.2
059300     MOVE     "SORT, INPUT PROC" TO FEATURE.                      SG2044.2
059400 SORT-TEST-5.                                                     SG2044.2
059500     PERFORM  READ-RELEASE-FILE3.                                 SG2044.2
059600     MOVE     R3-KEYS TO W-S3-KEYS.                               SG2044.2
059700     IF       W-S2-KEYS EQUAL TO "567AZAZ"                        SG2044.2
059800              PERFORM PASS-2 GO TO SORT-WRITE-5.                  SG2044.2
059900     GO       TO SORT-FAIL-5.                                     SG2044.2
060000 SORT-DELETE-5.                                                   SG2044.2
060100     PERFORM  DE-LETE-2.                                          SG2044.2
060200     GO       TO SORT-WRITE-5.                                    SG2044.2
060300 SORT-FAIL-5.                                                     SG2044.2
060400     MOVE     W-S2-KEYS TO COMPUTED-A.                            SG2044.2
060500     MOVE     "567AZAZ" TO CORRECT-A.                             SG2044.2
060600     PERFORM  FAIL-2.                                             SG2044.2
060700 SORT-WRITE-5.                                                    SG2044.2
060800     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         SG2044.2
060900     PERFORM  PRINT-DETAIL-2.                                     SG2044.2
061000 SORT-TEST-6.                                                     SG2044.2
061100     PERFORM  READ-RELEASE-FILE3 35 TIMES.                        SG2044.2
061200     MOVE     R3-KEYS TO W-S3-KEYS.                               SG2044.2
061300     IF       W-S2-KEYS EQUAL TO "567ARBR"                        SG2044.2
061400              PERFORM PASS-2 GO TO SORT-WRITE-6.                  SG2044.2
061500     GO       TO SORT-FAIL-6.                                     SG2044.2
061600 SORT-DELETE-6.                                                   SG2044.2
061700     PERFORM  DE-LETE-2.                                          SG2044.2
061800     GO       TO SORT-WRITE-6.                                    SG2044.2
061900 SORT-FAIL-6.                                                     SG2044.2
062000     MOVE     W-S2-KEYS TO COMPUTED-A.                            SG2044.2
062100     MOVE     "567ARBR" TO CORRECT-A.                             SG2044.2
062200     PERFORM  FAIL-2.                                             SG2044.2
062300 SORT-WRITE-6.                                                    SG2044.2
062400     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         SG2044.2
062500     PERFORM  PRINT-DETAIL-2.                                     SG2044.2
062600 SORT-TEST-7.                                                     SG2044.2
062700     PERFORM  READ-RELEASE-FILE3 35 TIMES.                        SG2044.2
062800     MOVE     R3-KEYS TO W-S3-KEYS.                               SG2044.2
062900     IF       W-S2-KEYS EQUAL TO "567AHBH"                        SG2044.2
063000              PERFORM PASS-2 GO TO SORT-WRITE-7.                  SG2044.2
063100     GO       TO SORT-FAIL-7.                                     SG2044.2
063200 SORT-DELETE-7.                                                   SG2044.2
063300     PERFORM  DE-LETE-2.                                          SG2044.2
063400     GO       TO SORT-WRITE-7.                                    SG2044.2
063500 SORT-FAIL-7.                                                     SG2044.2
063600     MOVE     W-S2-KEYS TO COMPUTED-A.                            SG2044.2
063700     MOVE     "567AHBH" TO CORRECT-A.                             SG2044.2
063800     PERFORM  FAIL-2.                                             SG2044.2
063900 SORT-WRITE-7.                                                    SG2044.2
064000     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         SG2044.2
064100     PERFORM  PRINT-DETAIL-2.                                     SG2044.2
064200 SORT-TEST-8.                                                     SG2044.2
064300     PERFORM  READ-RELEASE-FILE3 29 TIMES.                        SG2044.2
064400     MOVE     R3-KEYS TO W-S3-KEYS.                               SG2044.2
064500     IF       W-S2-KEYS EQUAL TO "567AABA"                        SG2044.2
064600              PERFORM PASS-2 GO TO SORT-WRITE-8.                  SG2044.2
064700     GO       TO SORT-FAIL-8.                                     SG2044.2
064800 SORT-DELETE-8.                                                   SG2044.2
064900     PERFORM  DE-LETE-2.                                          SG2044.2
065000     GO       TO SORT-WRITE-8.                                    SG2044.2
065100 SORT-FAIL-8.                                                     SG2044.2
065200     MOVE     W-S2-KEYS TO COMPUTED-A.                            SG2044.2
065300     MOVE     "567AABA" TO CORRECT-A.                             SG2044.2
065400     PERFORM  FAIL-2.                                             SG2044.2
065500 SORT-WRITE-8.                                                    SG2044.2
065600     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         SG2044.2
065700     PERFORM  PRINT-DETAIL-2.                                     SG2044.2
065800 CLOSE-2.                                                         SG2044.2
065900     CLOSE    FILE3.                                              SG2044.2
066000     GO       TO EXIT-2.                                          SG2044.2
066100 READ-RELEASE-FILE3.                                              SG2044.2
066200     READ     FILE3 AT END GO TO TERMINAL-2.                      SG2044.2
066300     RELEASE  S3.                                                 SG2044.2
066400*    NOTE     THE FROM OPTION AND MOVE ARE REDUNDANT WITH SAME    SG2044.2
066500*             RECORD AREA CLAUSE IN I-O CONTROL PARAGRAPH.        SG2044.2
066600 TERMINAL-2.                                                      SG2044.2
066700     PERFORM  FAIL-2.                                             SG2044.2
066800     MOVE     "TERMINAL-2" TO PAR-NAME.                           SG2044.2
066900     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               SG2044.2
067000     PERFORM  PRINT-DETAIL-2.                                     SG2044.2
067100     MOVE     SPACE TO FEATURE.                                   SG2044.2
067200     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              SG2044.2
067300     PERFORM  PRINT-DETAIL-2.                                     SG2044.2
067400     MOVE     "LAST SUCCESSFUL TEST" TO RE-MARK.                  SG2044.2
067500     PERFORM  PRINT-DETAIL-2.                                     SG2044.2
067600     GO       TO CLOSE-2.                                         SG2044.2
067700 INSPT-2. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       SG2044.2
067800 PASS-2.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         SG2044.2
067900 FAIL-2.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        SG2044.2
068000 DE-LETE-2.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.        SG2044.2
068100     MOVE "****TEST DELETED****" TO RE-MARK.                      SG2044.2
068200 PRINT-DETAIL-2.                                                  SG2044.2
068300     IF REC-CT NOT EQUAL TO ZERO                                  SG2044.2
068400             MOVE "." TO PARDOT-X                                 SG2044.2
068500             MOVE REC-CT TO DOTVALUE.                             SG2044.2
068600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-2.    SG2044.2
068700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-2             SG2044.2
068800        PERFORM FAIL-ROUTINE-2 THRU FAIL-ROUTINE-EX-2             SG2044.2
068900          ELSE PERFORM BAIL-OUT-2 THRU BAIL-OUT-EX-2.             SG2044.2
069000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG2044.2
069100     MOVE SPACE TO CORRECT-X.                                     SG2044.2
069200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG2044.2
069300     MOVE     SPACE TO RE-MARK.                                   SG2044.2
069400 WRITE-LINE-2.                                                    SG2044.2
069500     ADD 1 TO RECORD-COUNT.                                       SG2044.2
069600*    IF RECORD-COUNT GREATER 50                                   SG2044.2
069700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG2044.2
069800*        MOVE SPACE TO DUMMY-RECORD                               SG2044.2
069900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG2044.2
070000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-2           SG2044.2
070100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-2 2 TIMES   SG2044.2
070200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-2        SG2044.2
070300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG2044.2
070400*        MOVE ZERO TO RECORD-COUNT.                               SG2044.2
070500     PERFORM WRT-LN-2.                                            SG2044.2
070600 WRT-LN-2.                                                        SG2044.2
070700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG2044.2
070800     MOVE SPACE TO DUMMY-RECORD.                                  SG2044.2
070900 BLANK-LINE-PRINT-2.                                              SG2044.2
071000     PERFORM WRT-LN-2.                                            SG2044.2
071100 FAIL-ROUTINE-2.                                                  SG2044.2
071200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.     SG2044.2
071300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.      SG2044.2
071400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG2044.2
071500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   SG2044.2
071600     GO TO FAIL-ROUTINE-EX-2.                                     SG2044.2
071700 FAIL-RTN-WRITE-2.                                                SG2044.2
071800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-2         SG2044.2
071900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-2 2 TIMES. SG2044.2
072000 FAIL-ROUTINE-EX-2. EXIT.                                         SG2044.2
072100 BAIL-OUT-2.                                                      SG2044.2
072200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-2.     SG2044.2
072300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-2.             SG2044.2
072400 BAIL-OUT-WRITE-2.                                                SG2044.2
072500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG2044.2
072600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   SG2044.2
072700 BAIL-OUT-EX-2. EXIT.                                             SG2044.2
072800 EXIT-2.                                                          SG2044.2
072900     EXIT.                                                        SG2044.2
073000 SRT3-OUTPUT-1 SECTION 25.                                        SG2044.2
073100 INIT-3.                                                          SG2044.2
073200     MOVE     "SORT, OUTPUT PROC" TO FEATURE.                     SG2044.2
073300 SORT-TEST-9.                                                     SG2044.2
073400     PERFORM  RETURN-SORT3.                                       SG2044.2
073500     IF       S3-KEYS EQUAL TO "567AZBZ7001"                      SG2044.2
073600              PERFORM PASS-3 GO TO SORT-WRITE-9.                  SG2044.2
073700     GO       TO SORT-FAIL-9.                                     SG2044.2
073800 SORT-DELETE-9.                                                   SG2044.2
073900     PERFORM  DE-LETE-3.                                          SG2044.2
074000     GO       TO SORT-WRITE-9.                                    SG2044.2
074100 SORT-FAIL-9.                                                     SG2044.2
074200     MOVE     S3-KEYS TO COMPUTED-A.                              SG2044.2
074300     MOVE     "567AZBZ7001" TO CORRECT-A.                         SG2044.2
074400     PERFORM  FAIL-3.                                             SG2044.2
074500 SORT-WRITE-9.                                                    SG2044.2
074600     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         SG2044.2
074700     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
074800 SORT-TEST-10.                                                    SG2044.2
074900     PERFORM  RETURN-SORT3.                                       SG2044.2
075000     IF       S3-KEYS EQUAL TO "567AZBZ7026"                      SG2044.2
075100              PERFORM PASS-3 GO TO SORT-WRITE-10.                 SG2044.2
075200     GO       TO SORT-FAIL-10.                                    SG2044.2
075300 SORT-DELETE-10.                                                  SG2044.2
075400     PERFORM  DE-LETE-3.                                          SG2044.2
075500     GO       TO SORT-WRITE-10.                                   SG2044.2
075600 SORT-FAIL-10.                                                    SG2044.2
075700     MOVE     S3-KEYS TO COMPUTED-A.                              SG2044.2
075800     MOVE     "567AZBZ7026" TO CORRECT-A.                         SG2044.2
075900     PERFORM  FAIL-3.                                             SG2044.2
076000 SORT-WRITE-10.                                                   SG2044.2
076100     MOVE     "SORT-TEST-10" TO PAR-NAME.                         SG2044.2
076200     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
076300 SORT-TEST-11.                                                    SG2044.2
076400     PERFORM  RETURN-SORT3 35 TIMES.                              SG2044.2
076500     IF       S3-KEYS EQUAL TO "567AQBQ7010"                      SG2044.2
076600              PERFORM PASS-3 GO TO SORT-WRITE-11.                 SG2044.2
076700     GO       TO SORT-FAIL-11.                                    SG2044.2
076800 SORT-DELETE-11.                                                  SG2044.2
076900     PERFORM  DE-LETE-3.                                          SG2044.2
077000     GO       TO SORT-WRITE-11.                                   SG2044.2
077100 SORT-FAIL-11.                                                    SG2044.2
077200     MOVE     S3-KEYS TO COMPUTED-A.                              SG2044.2
077300     MOVE     "567AQBQ7010" TO CORRECT-A.                         SG2044.2
077400     PERFORM  FAIL-3.                                             SG2044.2
077500 SORT-WRITE-11.                                                   SG2044.2
077600     MOVE     "SORT-TEST-11" TO PAR-NAME.                         SG2044.2
077700     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
077800 SORT-TEST-12.                                                    SG2044.2
077900     PERFORM  RETURN-SORT3.                                       SG2044.2
078000     IF       S3-KEYS EQUAL TO "567AQBQ7035"                      SG2044.2
078100              PERFORM PASS-3 GO TO SORT-WRITE-12.                 SG2044.2
078200     GO       TO SORT-FAIL-12.                                    SG2044.2
078300 SORT-DELETE-12.                                                  SG2044.2
078400     PERFORM  DE-LETE-3.                                          SG2044.2
078500     GO       TO SORT-WRITE-12.                                   SG2044.2
078600 SORT-FAIL-12.                                                    SG2044.2
078700     MOVE     S3-KEYS TO COMPUTED-A.                              SG2044.2
078800     MOVE     "567AQBQ7035" TO CORRECT-A.                         SG2044.2
078900     PERFORM  FAIL-3.                                             SG2044.2
079000 SORT-WRITE-12.                                                   SG2044.2
079100     MOVE     "SORT-TEST-12" TO PAR-NAME.                         SG2044.2
079200     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
079300 SORT-TEST-13.                                                    SG2044.2
079400     PERFORM  RETURN-SORT3 35 TIMES.                              SG2044.2
079500     IF       S3-KEYS EQUAL TO "567AGBG7019"                      SG2044.2
079600              PERFORM PASS-3 GO TO SORT-WRITE-13.                 SG2044.2
079700     GO       TO SORT-FAIL-13.                                    SG2044.2
079800 SORT-DELETE-13.                                                  SG2044.2
079900     PERFORM  DE-LETE-3.                                          SG2044.2
080000     GO       TO SORT-WRITE-13.                                   SG2044.2
080100 SORT-FAIL-13.                                                    SG2044.2
080200     MOVE     S3-KEYS TO COMPUTED-A.                              SG2044.2
080300     MOVE     "567AGBG7019" TO CORRECT-A.                         SG2044.2
080400     PERFORM  FAIL-3.                                             SG2044.2
080500 SORT-WRITE-13.                                                   SG2044.2
080600     MOVE     "SORT-TEST-13" TO PAR-NAME.                         SG2044.2
080700     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
080800 SORT-TEST-14.                                                    SG2044.2
080900     PERFORM  RETURN-SORT3 27 TIMES.                              SG2044.2
081000     IF       S3-KEYS EQUAL TO "567AAAA7000"                      SG2044.2
081100              PERFORM PASS-3 GO TO SORT-WRITE-14.                 SG2044.2
081200     GO       TO SORT-FAIL-14.                                    SG2044.2
081300 SORT-DELETE-14.                                                  SG2044.2
081400     PERFORM  DE-LETE-3.                                          SG2044.2
081500     GO       TO SORT-WRITE-14.                                   SG2044.2
081600 SORT-FAIL-14.                                                    SG2044.2
081700     MOVE     S3-KEYS TO COMPUTED-A.                              SG2044.2
081800     MOVE     "567AAAA7000" TO CORRECT-A.                         SG2044.2
081900     PERFORM  FAIL-3.                                             SG2044.2
082000 SORT-WRITE-14.                                                   SG2044.2
082100     MOVE     "SORT-TEST-14" TO PAR-NAME.                         SG2044.2
082200     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
082300 SORT-TEST-15.                                                    SG2044.2
082400     RETURN   SORT3 RECORD AT END                                 SG2044.2
082500              PERFORM PASS-3 GO TO SORT-WRITE-15.                 SG2044.2
082600*    NOTE     THE FOLLOWING SENTENCES SHOULD NOT BE EXECUTED.     SG2044.2
082700     PERFORM FAIL-3.                                              SG2044.2
082800     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 SG2044.2
082900     GO       TO SORT-WRITE-15.                                   SG2044.2
083000 SORT-DELETE-15.                                                  SG2044.2
083100     PERFORM  DE-LETE-3.                                          SG2044.2
083200 SORT-WRITE-15.                                                   SG2044.2
083300     MOVE     "SORT-TEST-15" TO PAR-NAME.                         SG2044.2
083400     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
083500 CLOSE-3.                                                         SG2044.2
083600     GO       TO EXIT-3.                                          SG2044.2
083700 SRT3-OUTPUT-2 SECTION 25.                                        SG2044.2
083800 RETURN-SORT3.                                                    SG2044.2
083900     RETURN   SORT3 RECORD AT END GO TO TERMINAL-3.               SG2044.2
084000*    NOTE     RETURN VERB WITH ALL OPTIONS EXCEPT INTO.           SG2044.2
084100 TERMINAL-3.                                                      SG2044.2
084200     PERFORM  FAIL-3.                                             SG2044.2
084300     MOVE     "TERMINAL-3" TO PAR-NAME.                           SG2044.2
084400     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               SG2044.2
084500     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
084600     MOVE     SPACE TO FEATURE.                                   SG2044.2
084700     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              SG2044.2
084800     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
084900     MOVE     "LAST SUCCESSFUL TEST." TO RE-MARK                  SG2044.2
085000     PERFORM  PRINT-DETAIL-3.                                     SG2044.2
085100     GO       TO CLOSE-3.                                         SG2044.2
085200 PASS-3.                                                          SG2044.2
085300     MOVE "PASS" TO P-OR-F.  ADD 1 TO PASS-COUNTER.               SG2044.2
085400 FAIL-3.                                                          SG2044.2
085500     ADD      1 TO ERROR-COUNTER.                                 SG2044.2
085600     MOVE "FAIL*" TO P-OR-F.                                      SG2044.2
085700 DE-LETE-3.                                                       SG2044.2
085800     MOVE     SPACE TO P-OR-F.                                    SG2044.2
085900     MOVE     "    ************    " TO COMPUTED-A.               SG2044.2
086000     MOVE     "    ************    " TO CORRECT-A.                SG2044.2
086100     MOVE "****TEST DELETED****" TO RE-MARK.                      SG2044.2
086200     ADD 1 TO DELETE-CNT.                                         SG2044.2
086300 PRINT-DETAIL-3.                                                  SG2044.2
086400     IF REC-CT NOT EQUAL TO ZERO                                  SG2044.2
086500             MOVE "." TO PARDOT-X                                 SG2044.2
086600             MOVE REC-CT TO DOTVALUE.                             SG2044.2
086700     MOVE     TEST-RESULTS TO PRINT-REC.                          SG2044.2
086800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG2044.2
086900     MOVE     SPACE TO P-OR-F.                                    SG2044.2
087000     MOVE     SPACE TO COMPUTED-A.                                SG2044.2
087100     MOVE SPACE TO CORRECT-A.                                     SG2044.2
087200     IF     REC-CT EQUAL TO ZERO                                  SG2044.2
087300              MOVE SPACE TO PAR-NAME.                             SG2044.2
087400     MOVE     SPACE TO RE-MARK.                                   SG2044.2
087500 EXIT-3.                                                          SG2044.2
087600     EXIT.                                                        SG2044.2
087700 END-CCVS SECTION 25.                                             SG2044.2
087800 CCVS-EXIT SECTION.                                               SG2044.2
087900 CCVS-999999.                                                     SG2044.2
088000     GO TO CLOSE-FILES.                                           SG2044.2
      *END-OF,SG204A                                                                  
