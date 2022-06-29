      *HEADER,COBOL,SG105A                                                            
000100 IDENTIFICATION DIVISION.                                         SG1054.2
000200 PROGRAM-ID.                                                      SG1054.2
000300     SG105A.                                                      SG1054.2
000400 AUTHOR.                                                          SG1054.2
000500     FEDERAL COMPILER TESTING CENTER.                             SG1054.2
000600 INSTALLATION.                                                    SG1054.2
000700     GENERAL SERVICES ADMINISTRATION                              SG1054.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                SG1054.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 SG1054.2
001000     5203 LEESBURG PIKE  SUITE 1100                               SG1054.2
001100     FALLS CHURCH VIRGINIA 22041.                                 SG1054.2
001200                                                                  SG1054.2
001300     PHONE   (703) 756-6153                                       SG1054.2
001400                                                                  SG1054.2
001500     " HIGH       ".                                              SG1054.2
001600 DATE-WRITTEN.                                                    SG1054.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           SG1054.2
001800     CREATION DATE     /    VALIDATION DATE                       SG1054.2
001900     "4.2 ".                                                      SG1054.2
002000 SECURITY.                                                        SG1054.2
002100     NONE.                                                        SG1054.2
002200     SG105A IS A COMPLETELY SELF-CONTAINED PROGRAM.  THE INPUT    SG1054.2
002300     PROCEDURE BUILDS THE EIGHT-RECORD FILE SHOWN BELOW. THE      SG1054.2
002400     OUTPUT PROCEDURE CHECKS THE SORTED FILE AND GENERATES THE    SG1054.2
002500     REPORT.                                                      SG1054.2
002600     SORT    SORT    SORT    SORT    SORT   SORT     SORT    SORT SG1054.2
002700     KEY-1   KEY-2   KEY-3   KEY-4   KEY-5  KEY-6    KEY-7   KEY-8SG1054.2
002800     S9(6)   A(5)    SV9(16) X(10)   A(20)  X(10)    999     S99  SG1054.2
002900     USAGE   JUST            JUST                            USAGESG1054.2
003000     COMP    RIGHT           RIGHT                           COMP SG1054.2
003100                                                                  SG1054.2
003200     +123456     BBB -.1234   BBBBBB A      AAAAAAAA 501     +99  SG1054.2
003300     -054321       X -.1234   BBBBBB A      AAAAAAAA 501     +99  SG1054.2
003400     -054321     BBB +.6      BBBBBB A      AAAAAAAA 501     +99  SG1054.2
003500     -054321     BBB -.1234        X A      AAAAAAAA 501     +99  SG1054.2
003600     -054321     BBB -.1234   BBBBBB Z      AAAAAAAA 501     +99  SG1054.2
003700     -054321     BBB -.1234   BBBBBB A      Z        501     +99  SG1054.2
003800     -054321     BBB -.1234   BBBBBB A      AAAAAAAA 418     +99  SG1054.2
003900     -054321     BBB -.1234   BBBBBB A      AAAAAAAA 501     -14  SG1054.2
004000                                                                  SG1054.2
004100     THIS PROGRAM CHECKS THE COMPILER"S ABILITY TO HANDLE EIGHT   SG1054.2
004200     ASCENDING KEYS IN ONE FILE.                                  SG1054.2
004300                                                                  SG1054.2
004400 ENVIRONMENT DIVISION.                                            SG1054.2
004500 CONFIGURATION SECTION.                                           SG1054.2
004600 SOURCE-COMPUTER.                                                 SG1054.2
004700     XXXXX082.                                                    SG1054.2
004800 OBJECT-COMPUTER.                                                 SG1054.2
004900     XXXXX083.                                                    SG1054.2
005000 INPUT-OUTPUT SECTION.                                            SG1054.2
005100 FILE-CONTROL.                                                    SG1054.2
005200     SELECT PRINT-FILE ASSIGN TO                                  SG1054.2
005300     XXXXX055.                                                    SG1054.2
005400     SELECT   SORTFILE-1H ASSIGN TO                               SG1054.2
005500     XXXXX027.                                                    SG1054.2
005600 DATA DIVISION.                                                   SG1054.2
005700 FILE SECTION.                                                    SG1054.2
005800 FD  PRINT-FILE                                                   SG1054.2
005900     LABEL RECORDS                                                SG1054.2
006000     XXXXX084                                                     SG1054.2
006100     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       SG1054.2
006200 01  PRINT-REC PICTURE X(120).                                    SG1054.2
006300 01  DUMMY-RECORD PICTURE X(120).                                 SG1054.2
006400 SD  SORTFILE-1H                                                  SG1054.2
006500     DATA RECORD IS SORTFILE-REC.                                 SG1054.2
006600 01  SORTFILE-REC.                                                SG1054.2
006700     02 SORTKEY-8       PICTURE S99 COMPUTATIONAL.                SG1054.2
006800     02 SORTKEY-1       PICTURE S9(6) COMPUTATIONAL.              SG1054.2
006900     02 SORTKEY-7       PICTURE 999.                              SG1054.2
007000     02 SORTKEY-3       PICTURE SV9(16).                          SG1054.2
007100     02 FILLER          PICTURE XX.                               SG1054.2
007200     02 SORTKEY-4       PICTURE X(10) JUSTIFIED RIGHT.            SG1054.2
007300     02 SORTKEY-6       PICTURE X(10).                            SG1054.2
007400     02 SORTKEY-2       PICTURE A(05) JUSTIFIED RIGHT.            SG1054.2
007500     02 SORTKEY-5       PICTURE A(20).                            SG1054.2
007600     02 FILLER          PICTURE XXX.                              SG1054.2
007700 WORKING-STORAGE SECTION.                                         SG1054.2
007800 77  UTIL-CTR           PICTURE S99999.                           SG1054.2
007900 77  SPAC-E             PICTURE X VALUE " ".                      SG1054.2
008000 01  TEST-RESULTS.                                                SG1054.2
008100     02 FILLER                    PICTURE X VALUE SPACE.          SG1054.2
008200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SG1054.2
008300     02 FILLER                    PICTURE X VALUE SPACE.          SG1054.2
008400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SG1054.2
008500     02 FILLER                    PICTURE X  VALUE SPACE.         SG1054.2
008600     02  PAR-NAME.                                                SG1054.2
008700       03 FILLER PICTURE X(12) VALUE SPACE.                       SG1054.2
008800       03  PARDOT-X PICTURE X  VALUE SPACE.                       SG1054.2
008900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SG1054.2
009000       03 FILLER PIC X(5) VALUE SPACE.                            SG1054.2
009100     02 FILLER PIC X(10) VALUE SPACE.                             SG1054.2
009200     02 RE-MARK PIC X(61).                                        SG1054.2
009300 01  TEST-COMPUTED.                                               SG1054.2
009400     02 FILLER PIC X(30) VALUE SPACE.                             SG1054.2
009500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SG1054.2
009600     02 COMPUTED-X.                                               SG1054.2
009700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SG1054.2
009800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SG1054.2
009900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SG1054.2
010000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SG1054.2
010100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SG1054.2
010200     03       CM-18V0 REDEFINES COMPUTED-A.                       SG1054.2
010300         04 COMPUTED-18V0                   PICTURE -9(18).       SG1054.2
010400         04 FILLER                          PICTURE X.            SG1054.2
010500     03 FILLER PIC X(50) VALUE SPACE.                             SG1054.2
010600 01  TEST-CORRECT.                                                SG1054.2
010700     02 FILLER PIC X(30) VALUE SPACE.                             SG1054.2
010800     02 FILLER PIC X(17) VALUE "       CORRECT =".                SG1054.2
010900     02 CORRECT-X.                                                SG1054.2
011000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SG1054.2
011100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SG1054.2
011200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SG1054.2
011300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SG1054.2
011400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SG1054.2
011500     03      CR-18V0 REDEFINES CORRECT-A.                         SG1054.2
011600         04 CORRECT-18V0                    PICTURE -9(18).       SG1054.2
011700         04 FILLER                          PICTURE X.            SG1054.2
011800     03 FILLER PIC X(50) VALUE SPACE.                             SG1054.2
011900 01  CCVS-C-1.                                                    SG1054.2
012000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASG1054.2
012100-    "SS  PARAGRAPH-NAME                                          SG1054.2
012200-    "        REMARKS".                                           SG1054.2
012300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SG1054.2
012400 01  CCVS-C-2.                                                    SG1054.2
012500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1054.2
012600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SG1054.2
012700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SG1054.2
012800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SG1054.2
012900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SG1054.2
013000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SG1054.2
013100 01  REC-CT PICTURE 99 VALUE ZERO.                                SG1054.2
013200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SG1054.2
013300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SG1054.2
013400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SG1054.2
013500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SG1054.2
013600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SG1054.2
013700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SG1054.2
013800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SG1054.2
013900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SG1054.2
014000 01  CCVS-H-1.                                                    SG1054.2
014100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SG1054.2
014200     02 FILLER PICTURE X(67) VALUE                                SG1054.2
014300     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  SG1054.2
014400-    " SYSTEM".                                                   SG1054.2
014500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SG1054.2
014600 01  CCVS-H-2.                                                    SG1054.2
014700     02 FILLER PICTURE X(52) VALUE IS                             SG1054.2
014800     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   SG1054.2
014900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SG1054.2
015000     02 TEST-ID PICTURE IS X(9).                                  SG1054.2
015100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SG1054.2
015200 01  CCVS-H-3.                                                    SG1054.2
015300     02  FILLER PICTURE X(34) VALUE                               SG1054.2
015400     " FOR OFFICIAL USE ONLY    ".                                SG1054.2
015500     02  FILLER PICTURE X(58) VALUE                               SG1054.2
015600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SG1054.2
015700     02  FILLER PICTURE X(28) VALUE                               SG1054.2
015800     "  COPYRIGHT   1974 ".                                       SG1054.2
015900 01  CCVS-E-1.                                                    SG1054.2
016000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SG1054.2
016100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SG1054.2
016200     02 ID-AGAIN PICTURE IS X(9).                                 SG1054.2
016300     02 FILLER PICTURE X(45) VALUE IS                             SG1054.2
016400     " NTIS DISTRIBUTION COBOL 74".                               SG1054.2
016500 01  CCVS-E-2.                                                    SG1054.2
016600     02  FILLER                   PICTURE X(31)  VALUE            SG1054.2
016700     SPACE.                                                       SG1054.2
016800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SG1054.2
016900     02 CCVS-E-2-2.                                               SG1054.2
017000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SG1054.2
017100         03 FILLER PICTURE IS X VALUE IS SPACE.                   SG1054.2
017200         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      SG1054.2
017300 01  CCVS-E-3.                                                    SG1054.2
017400     02  FILLER PICTURE X(22) VALUE                               SG1054.2
017500     " FOR OFFICIAL USE ONLY".                                    SG1054.2
017600     02  FILLER PICTURE X(12) VALUE SPACE.                        SG1054.2
017700     02  FILLER PICTURE X(58) VALUE                               SG1054.2
017800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SG1054.2
017900     02  FILLER PICTURE X(13) VALUE SPACE.                        SG1054.2
018000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 SG1054.2
018100 01  CCVS-E-4.                                                    SG1054.2
018200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SG1054.2
018300     02 FILLER PIC XXXX VALUE " OF ".                             SG1054.2
018400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SG1054.2
018500     02 FILLER PIC X(40) VALUE                                    SG1054.2
018600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SG1054.2
018700 01  XXINFO.                                                      SG1054.2
018800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SG1054.2
018900     02 INFO-TEXT.                                                SG1054.2
019000     04 FILLER PIC X(20) VALUE SPACE.                             SG1054.2
019100     04 XXCOMPUTED PIC X(20).                                     SG1054.2
019200     04 FILLER PIC X(5) VALUE SPACE.                              SG1054.2
019300     04 XXCORRECT PIC X(20).                                      SG1054.2
019400 01  HYPHEN-LINE.                                                 SG1054.2
019500     02 FILLER PICTURE IS X VALUE IS SPACE.                       SG1054.2
019600     02 FILLER PICTURE IS X(65) VALUE IS "************************SG1054.2
019700-    "*****************************************".                 SG1054.2
019800     02 FILLER PICTURE IS X(54) VALUE IS "************************SG1054.2
019900-    "******************************".                            SG1054.2
020000 01  CCVS-PGM-ID PIC X(6) VALUE                                   SG1054.2
020100     "SG105A".                                                    SG1054.2
020200 PROCEDURE DIVISION.                                              SG1054.2
020300 SORT-PARA SECTION 73.                                            SG1054.2
020400 SORT-PARAGRAPH.                                                  SG1054.2
020500     SORT     SORTFILE-1H ON                                      SG1054.2
020600              ASCENDING KEY SORTKEY-1                             SG1054.2
020700              ASCENDING SORTKEY-2                                 SG1054.2
020800              ASCENDING SORTKEY-3                                 SG1054.2
020900              ASCENDING SORTKEY-4                                 SG1054.2
021000              ASCENDING SORTKEY-5                                 SG1054.2
021100              ASCENDING SORTKEY-6                                 SG1054.2
021200              ASCENDING SORTKEY-7                                 SG1054.2
021300              ASCENDING SORTKEY-8                                 SG1054.2
021400              INPUT PROCEDURE INPROC                              SG1054.2
021500              OUTPUT PROCEDURE OUTPROC THRU OUTPROC-EXIT.         SG1054.2
021600     STOP     RUN.                                                SG1054.2
021700 INPROC SECTION 20.                                               SG1054.2
021800 BUILD-FILE.                                                      SG1054.2
021900     PERFORM  BUILD-RECORD.                                       SG1054.2
022000     MOVE     +123456             TO SORTKEY-1.                   SG1054.2
022100     PERFORM  RELEASE-RECORD.                                     SG1054.2
022200     PERFORM  BUILD-RECORD.                                       SG1054.2
022300     MOVE     "X"                 TO SORTKEY-2.                   SG1054.2
022400     PERFORM  RELEASE-RECORD.                                     SG1054.2
022500     PERFORM  BUILD-RECORD.                                       SG1054.2
022600     MOVE     +.6                 TO SORTKEY-3.                   SG1054.2
022700     PERFORM  RELEASE-RECORD.                                     SG1054.2
022800     PERFORM  BUILD-RECORD.                                       SG1054.2
022900     MOVE     "X"                 TO SORTKEY-4.                   SG1054.2
023000     PERFORM  RELEASE-RECORD.                                     SG1054.2
023100     PERFORM  BUILD-RECORD.                                       SG1054.2
023200     MOVE     "Z"                 TO SORTKEY-5.                   SG1054.2
023300     PERFORM  RELEASE-RECORD.                                     SG1054.2
023400     PERFORM  BUILD-RECORD.                                       SG1054.2
023500     MOVE     "Z"                 TO SORTKEY-6.                   SG1054.2
023600     PERFORM  RELEASE-RECORD.                                     SG1054.2
023700     PERFORM  BUILD-RECORD.                                       SG1054.2
023800     MOVE     +418                TO SORTKEY-7.                   SG1054.2
023900     PERFORM  RELEASE-RECORD.                                     SG1054.2
024000     PERFORM  BUILD-RECORD.                                       SG1054.2
024100     MOVE     -14                 TO SORTKEY-8.                   SG1054.2
024200     PERFORM  RELEASE-RECORD.                                     SG1054.2
024300     GO       TO BUILD-EXIT.                                      SG1054.2
024400 BUILD-RECORD.                                                    SG1054.2
024500     MOVE     -054321             TO SORTKEY-1.                   SG1054.2
024600     MOVE     "BBB"               TO SORTKEY-2.                   SG1054.2
024700     MOVE     -.1234567890123456  TO SORTKEY-3.                   SG1054.2
024800     MOVE     "BBBBBB"            TO SORTKEY-4.                   SG1054.2
024900     MOVE     "A"                 TO SORTKEY-5.                   SG1054.2
025000     MOVE     "AAAAAAAA"          TO SORTKEY-6.                   SG1054.2
025100     MOVE     -501                TO SORTKEY-7.                   SG1054.2
025200*       NOTE THIS ITEM IS INTENTIONALLY MOVED TO AN UNSIGNED      SG1054.2
025300*             FIELD.                                              SG1054.2
025400     MOVE     +99                 TO SORTKEY-8.                   SG1054.2
025500 RELEASE-RECORD.                                                  SG1054.2
025600     RELEASE  SORTFILE-REC.                                       SG1054.2
025700 BUILD-EXIT.                                                      SG1054.2
025800     EXIT.                                                        SG1054.2
025900 OUTPROC SECTION 00.                                              SG1054.2
026000 OPEN-FILES.                                                      SG1054.2
026100     OPEN     OUTPUT PRINT-FILE.                                  SG1054.2
026200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SG1054.2
026300     MOVE    SPACE TO TEST-RESULTS.                               SG1054.2
026400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SG1054.2
026500     IF       SPAC-E IS LESS THAN "B"                             SG1054.2
026600              GO TO SPACE-IS-LESS-THAN-B.                         SG1054.2
026700 B-IS-LESS-THAN-SPACE SECTION 00.                                 SG1054.2
026800 SORT-INIT-A.                                                     SG1054.2
026900     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    SG1054.2
027000*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     SG1054.2
027100*             ORDER --- 8 7 6 5 4 3 1 2 --- THAT IS,              SG1054.2
027200*             THE 8TH RECORD SORTS UP TO THE 1ST POSITION,        SG1054.2
027300*             THE 7TH RECORD SORTS UP TO THE 2ND POSITION, ETC.   SG1054.2
027400 SORT-TEST-1.                                                     SG1054.2
027500     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
027600     IF       SORTKEY-7 EQUAL TO 418                              SG1054.2
027700              PERFORM PASS GO TO SORT-WRITE-1.                    SG1054.2
027800 SORT-FAIL-1.                                                     SG1054.2
027900     PERFORM  FAIL.                                               SG1054.2
028000     MOVE     SORTKEY-7 TO COMPUTED-N.                            SG1054.2
028100     MOVE     418 TO CORRECT-N.                                   SG1054.2
028200 SORT-WRITE-1.                                                    SG1054.2
028300     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         SG1054.2
028400     PERFORM  PRINT-DETAIL.                                       SG1054.2
028500 SORT-TEST-2.                                                     SG1054.2
028600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
028700     IF       SORTKEY-8 EQUAL TO -14                              SG1054.2
028800              PERFORM PASS GO TO SORT-WRITE-2.                    SG1054.2
028900 SORT-FAIL-2.                                                     SG1054.2
029000     PERFORM  FAIL.                                               SG1054.2
029100     MOVE     SORTKEY-8 TO COMPUTED-N.                            SG1054.2
029200     MOVE     -14 TO CORRECT-N.                                   SG1054.2
029300 SORT-WRITE-2.                                                    SG1054.2
029400     MOVE     "SORT-TEST-2 " TO PAR-NAME.                         SG1054.2
029500     PERFORM  PRINT-DETAIL.                                       SG1054.2
029600 SORT-TEST-3.                                                     SG1054.2
029700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
029800     IF       SORTKEY-6 EQUAL TO "Z         "                     SG1054.2
029900              PERFORM PASS GO TO SORT-WRITE-3.                    SG1054.2
030000 SORT-FAIL-3.                                                     SG1054.2
030100     PERFORM  FAIL.                                               SG1054.2
030200     MOVE     SORTKEY-6 TO COMPUTED-A.                            SG1054.2
030300     MOVE     "Z         "           TO CORRECT-A.                SG1054.2
030400 SORT-WRITE-3.                                                    SG1054.2
030500     MOVE     "SORT-TEST-3 " TO PAR-NAME.                         SG1054.2
030600     PERFORM  PRINT-DETAIL.                                       SG1054.2
030700 SORT-TEST-4.                                                     SG1054.2
030800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
030900     IF       SORTKEY-5 EQUAL TO "Z                   "           SG1054.2
031000              PERFORM PASS GO TO SORT-WRITE-4.                    SG1054.2
031100 SORT-FAIL-4.                                                     SG1054.2
031200     PERFORM  FAIL.                                               SG1054.2
031300     MOVE     SORTKEY-5 TO COMPUTED-A.                            SG1054.2
031400     MOVE     "Z                   " TO CORRECT-A.                SG1054.2
031500 SORT-WRITE-4.                                                    SG1054.2
031600     MOVE     "SORT-TEST-4 " TO PAR-NAME.                         SG1054.2
031700     PERFORM  PRINT-DETAIL.                                       SG1054.2
031800 SORT-TEST-5.                                                     SG1054.2
031900     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
032000     IF       SORTKEY-4 EQUAL TO "         X"                     SG1054.2
032100              PERFORM PASS GO TO SORT-WRITE-5.                    SG1054.2
032200 SORT-FAIL-5.                                                     SG1054.2
032300     PERFORM  FAIL.                                               SG1054.2
032400     MOVE     SORTKEY-4 TO COMPUTED-A.                            SG1054.2
032500     MOVE     "         X"           TO CORRECT-A.                SG1054.2
032600 SORT-WRITE-5.                                                    SG1054.2
032700     MOVE     "SORT-TEST-5 " TO PAR-NAME.                         SG1054.2
032800     PERFORM  PRINT-DETAIL.                                       SG1054.2
032900 SORT-TEST-6.                                                     SG1054.2
033000     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
033100     IF       SORTKEY-3 EQUAL TO +.6000000000000000               SG1054.2
033200              PERFORM PASS GO TO SORT-WRITE-6.                    SG1054.2
033300 SORT-FAIL-6.                                                     SG1054.2
033400     PERFORM  FAIL.                                               SG1054.2
033500     MOVE     SORTKEY-3 TO COMPUTED-0V18.                         SG1054.2
033600     MOVE     +.6000000000000000     TO CORRECT-0V18.             SG1054.2
033700 SORT-WRITE-6.                                                    SG1054.2
033800     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         SG1054.2
033900     PERFORM  PRINT-DETAIL.                                       SG1054.2
034000 SORT-TEST-7.                                                     SG1054.2
034100     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
034200     IF       SORTKEY-2 EQUAL TO "    X"                          SG1054.2
034300              PERFORM PASS GO TO SORT-WRITE-7.                    SG1054.2
034400 SORT-FAIL-7.                                                     SG1054.2
034500     PERFORM  FAIL.                                               SG1054.2
034600     MOVE     SORTKEY-2 TO COMPUTED-A.                            SG1054.2
034700     MOVE     "    X" TO CORRECT-A.                               SG1054.2
034800 SORT-WRITE-7.                                                    SG1054.2
034900     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         SG1054.2
035000     PERFORM  PRINT-DETAIL.                                       SG1054.2
035100 SORT-TEST-8.                                                     SG1054.2
035200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
035300     IF       SORTKEY-1 EQUAL TO +123456                          SG1054.2
035400              PERFORM PASS GO TO SORT-WRITE-8.                    SG1054.2
035500 SORT-FAIL-8.                                                     SG1054.2
035600     PERFORM  FAIL.                                               SG1054.2
035700     MOVE     SORTKEY-1 TO COMPUTED-N.                            SG1054.2
035800     MOVE     +123456 TO CORRECT-N.                               SG1054.2
035900 SORT-WRITE-8.                                                    SG1054.2
036000     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         SG1054.2
036100     PERFORM  PRINT-DETAIL.                                       SG1054.2
036200 SORT-REMARK-A.                                                   SG1054.2
036300     MOVE     SPACE TO FEATURE.                                   SG1054.2
036400     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                SG1054.2
036500     PERFORM  PRINT-DETAIL.                                       SG1054.2
036600     MOVE     "RENDERS TESTS 9 THRU 16" TO RE-MARK.               SG1054.2
036700     PERFORM  PRINT-DETAIL.                                       SG1054.2
036800     MOVE     "UNNECESSARY." TO RE-MARK.                          SG1054.2
036900     PERFORM  PRINT-DETAIL.                                       SG1054.2
037000     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    SG1054.2
037100     GO       TO CONTINUE-TESTING.                                SG1054.2
037200 SPACE-IS-LESS-THAN-B SECTION 00.                                 SG1054.2
037300 SORT-REMARK-B.                                                   SG1054.2
037400     MOVE     "THE COLLATING SEQUENCE" TO RE-MARK.                SG1054.2
037500     PERFORM  PRINT-DETAIL.                                       SG1054.2
037600     MOVE     "RENDERS TESTS 1 THRU 8" TO RE-MARK.                SG1054.2
037700     PERFORM  PRINT-DETAIL.                                       SG1054.2
037800     MOVE     "UNNECESSARY." TO RE-MARK.                          SG1054.2
037900     PERFORM  PRINT-DETAIL.                                       SG1054.2
038000     MOVE     "SORT - 8 ASC. KEYS" TO FEATURE.                    SG1054.2
038100*        NOTE THE RECORDS SHOULD BE SORTED INTO THE FOLLOWING     SG1054.2
038200*             ORDER --- 8 1 7 2 6 5 3 4 --- THAT IS,              SG1054.2
038300*             THE 1ST RECORD IS SORTED DOWN TO THE 8TH POSITION,  SG1054.2
038400*             THE 2ND RECORD SORTS UP TO THE 1ST POSITION, ETC.   SG1054.2
038500 SORT-TEST-9.                                                     SG1054.2
038600     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
038700     IF       SORTKEY-2 EQUAL TO "    X"                          SG1054.2
038800              PERFORM PASS GO TO SORT-WRITE-9.                    SG1054.2
038900 SORT-FAIL-9.                                                     SG1054.2
039000     PERFORM  FAIL.                                               SG1054.2
039100     MOVE     SORTKEY-2 TO COMPUTED-A.                            SG1054.2
039200     MOVE     "    X" TO CORRECT-A.                               SG1054.2
039300 SORT-WRITE-9.                                                    SG1054.2
039400     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         SG1054.2
039500     PERFORM  PRINT-DETAIL.                                       SG1054.2
039600 SORT-TEST-10.                                                    SG1054.2
039700     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
039800     IF       SORTKEY-4 EQUAL TO "         X"                     SG1054.2
039900              PERFORM PASS GO TO SORT-WRITE-10.                   SG1054.2
040000 SORT-FAIL-10.                                                    SG1054.2
040100     PERFORM  FAIL.                                               SG1054.2
040200     MOVE     SORTKEY-4 TO COMPUTED-A.                            SG1054.2
040300     MOVE     "         X" TO CORRECT-A.                          SG1054.2
040400 SORT-WRITE-10.                                                   SG1054.2
040500     MOVE     "SORT-TEST-10" TO PAR-NAME.                         SG1054.2
040600     PERFORM  PRINT-DETAIL.                                       SG1054.2
040700 SORT-TEST-11.                                                    SG1054.2
040800     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
040900     IF       SORTKEY-7 EQUAL TO 418                              SG1054.2
041000              PERFORM PASS GO TO SORT-WRITE-11.                   SG1054.2
041100 SORT-FAIL-11.                                                    SG1054.2
041200     PERFORM  FAIL.                                               SG1054.2
041300     MOVE     SORTKEY-7 TO COMPUTED-N                             SG1054.2
041400     MOVE     418 TO CORRECT-N.                                   SG1054.2
041500 SORT-WRITE-11.                                                   SG1054.2
041600     MOVE     "SORT-TEST-11" TO PAR-NAME.                         SG1054.2
041700     PERFORM  PRINT-DETAIL.                                       SG1054.2
041800 SORT-TEST-12.                                                    SG1054.2
041900     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
042000     IF       SORTKEY-8 EQUAL TO -14                              SG1054.2
042100              PERFORM PASS GO TO SORT-WRITE-12.                   SG1054.2
042200 SORT-FAIL-12.                                                    SG1054.2
042300     PERFORM  FAIL.                                               SG1054.2
042400     MOVE     SORTKEY-8 TO COMPUTED-N.                            SG1054.2
042500     MOVE     -14 TO CORRECT-N.                                   SG1054.2
042600 SORT-WRITE-12.                                                   SG1054.2
042700     MOVE     "SORT-TEST-12" TO PAR-NAME.                         SG1054.2
042800     PERFORM  PRINT-DETAIL.                                       SG1054.2
042900 SORT-TEST-13.                                                    SG1054.2
043000     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
043100     IF       SORTKEY-6 EQUAL TO "Z         "                     SG1054.2
043200              PERFORM PASS GO TO SORT-WRITE-13.                   SG1054.2
043300 SORT-FAIL-13.                                                    SG1054.2
043400     PERFORM  FAIL.                                               SG1054.2
043500     MOVE     SORTKEY-6 TO COMPUTED-A.                            SG1054.2
043600     MOVE     "Z         "           TO CORRECT-A.                SG1054.2
043700 SORT-WRITE-13.                                                   SG1054.2
043800     MOVE     "SORT-TEST-13" TO PAR-NAME.                         SG1054.2
043900     PERFORM  PRINT-DETAIL.                                       SG1054.2
044000 SORT-TEST-14.                                                    SG1054.2
044100     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
044200     IF       SORTKEY-5 EQUAL TO "Z                   "           SG1054.2
044300              PERFORM PASS GO TO SORT-WRITE-14.                   SG1054.2
044400 SORT-FAIL-14.                                                    SG1054.2
044500     PERFORM  FAIL.                                               SG1054.2
044600     MOVE     SORTKEY-5 TO COMPUTED-A.                            SG1054.2
044700     MOVE     "Z                   " TO CORRECT-A.                SG1054.2
044800 SORT-WRITE-14.                                                   SG1054.2
044900     MOVE     "SORT-TEST-14" TO PAR-NAME.                         SG1054.2
045000     PERFORM  PRINT-DETAIL.                                       SG1054.2
045100 SORT-TEST-15.                                                    SG1054.2
045200     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
045300     IF       SORTKEY-3 EQUAL TO +.6000000000000000               SG1054.2
045400              PERFORM PASS GO TO SORT-WRITE-15.                   SG1054.2
045500 SORT-FAIL-15.                                                    SG1054.2
045600     PERFORM  FAIL.                                               SG1054.2
045700     MOVE     SORTKEY-3 TO COMPUTED-18V0.                         SG1054.2
045800     MOVE     +.6000000000000000     TO CORRECT-18V0.             SG1054.2
045900 SORT-WRITE-15.                                                   SG1054.2
046000     MOVE     "SORT-TEST-15" TO PAR-NAME.                         SG1054.2
046100     PERFORM  PRINT-DETAIL.                                       SG1054.2
046200 SORT-TEST-16.                                                    SG1054.2
046300     RETURN   SORTFILE-1H AT END GO TO RETURN-ERROR.              SG1054.2
046400     IF       SORTKEY-1 EQUAL TO +123456                          SG1054.2
046500              PERFORM PASS GO TO SORT-WRITE-16.                   SG1054.2
046600 SORT-FAIL-16.                                                    SG1054.2
046700     PERFORM  FAIL.                                               SG1054.2
046800     MOVE     SORTKEY-1 TO COMPUTED-N.                            SG1054.2
046900     MOVE     +123456 TO CORRECT-N.                               SG1054.2
047000 SORT-WRITE-16.                                                   SG1054.2
047100     MOVE     "SORT-TEST-16" TO PAR-NAME.                         SG1054.2
047200     PERFORM  PRINT-DETAIL.                                       SG1054.2
047300 CONTINUE-TESTING SECTION 00.                                     SG1054.2
047400 SORT-TEST-17.                                                    SG1054.2
047500     RETURN   SORTFILE-1H AT END                                  SG1054.2
047600              PERFORM PASS GO TO SORT-WRITE-17.                   SG1054.2
047700 SORT-FAIL-17.                                                    SG1054.2
047800     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 SG1054.2
047900     PERFORM  FAIL.                                               SG1054.2
048000 SORT-WRITE-17.                                                   SG1054.2
048100     MOVE     "SORT-TEST-17" TO PAR-NAME.                         SG1054.2
048200     PERFORM  PRINT-DETAIL.                                       SG1054.2
048300     GO       TO OUTPROC-EXIT.                                    SG1054.2
048400 RETURN-ERROR.                                                    SG1054.2
048500     MOVE     "RETURN-ERROR" TO PAR-NAME.                         SG1054.2
048600     PERFORM  FAIL.                                               SG1054.2
048700     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 SG1054.2
048800     PERFORM  PRINT-DETAIL.                                       SG1054.2
048900     GO TO CCVS1-EXIT.                                            SG1054.2
049000 CLOSE-FILES.                                                     SG1054.2
049100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SG1054.2
049200 TERMINATE-CCVS.                                                  SG1054.2
049300*    EXIT PROGRAM.                                                SG1054.2
049400*TERMINATE-CALL.                                                  SG1054.2
049500     STOP     RUN.                                                SG1054.2
049600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SG1054.2
049700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SG1054.2
049800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SG1054.2
049900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SG1054.2
050000     MOVE "****TEST DELETED****" TO RE-MARK.                      SG1054.2
050100 PRINT-DETAIL.                                                    SG1054.2
050200     IF REC-CT NOT EQUAL TO ZERO                                  SG1054.2
050300             MOVE "." TO PARDOT-X                                 SG1054.2
050400             MOVE REC-CT TO DOTVALUE.                             SG1054.2
050500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SG1054.2
050600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SG1054.2
050700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SG1054.2
050800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SG1054.2
050900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SG1054.2
051000     MOVE SPACE TO CORRECT-X.                                     SG1054.2
051100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SG1054.2
051200     MOVE     SPACE TO RE-MARK.                                   SG1054.2
051300 HEAD-ROUTINE.                                                    SG1054.2
051400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1054.2
051500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SG1054.2
051600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SG1054.2
051700 COLUMN-NAMES-ROUTINE.                                            SG1054.2
051800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1054.2
051900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1054.2
052000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SG1054.2
052100 END-ROUTINE.                                                     SG1054.2
052200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SG1054.2
052300 END-RTN-EXIT.                                                    SG1054.2
052400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SG1054.2
052500 END-ROUTINE-1.                                                   SG1054.2
052600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SG1054.2
052700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SG1054.2
052800      ADD PASS-COUNTER TO ERROR-HOLD.                             SG1054.2
052900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SG1054.2
053000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SG1054.2
053100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SG1054.2
053200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SG1054.2
053300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SG1054.2
053400  END-ROUTINE-12.                                                 SG1054.2
053500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SG1054.2
053600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SG1054.2
053700         MOVE "NO " TO ERROR-TOTAL                                SG1054.2
053800         ELSE                                                     SG1054.2
053900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SG1054.2
054000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SG1054.2
054100     PERFORM WRITE-LINE.                                          SG1054.2
054200 END-ROUTINE-13.                                                  SG1054.2
054300     IF DELETE-CNT IS EQUAL TO ZERO                               SG1054.2
054400         MOVE "NO " TO ERROR-TOTAL  ELSE                          SG1054.2
054500         MOVE DELETE-CNT TO ERROR-TOTAL.                          SG1054.2
054600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SG1054.2
054700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1054.2
054800      IF   INSPECT-COUNTER EQUAL TO ZERO                          SG1054.2
054900          MOVE "NO " TO ERROR-TOTAL                               SG1054.2
055000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SG1054.2
055100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SG1054.2
055200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SG1054.2
055300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SG1054.2
055400 WRITE-LINE.                                                      SG1054.2
055500     ADD 1 TO RECORD-COUNT.                                       SG1054.2
055600*    IF RECORD-COUNT GREATER 50                                   SG1054.2
055700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SG1054.2
055800*        MOVE SPACE TO DUMMY-RECORD                               SG1054.2
055900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SG1054.2
056000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SG1054.2
056100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SG1054.2
056200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SG1054.2
056300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SG1054.2
056400*        MOVE ZERO TO RECORD-COUNT.                               SG1054.2
056500     PERFORM WRT-LN.                                              SG1054.2
056600 WRT-LN.                                                          SG1054.2
056700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SG1054.2
056800     MOVE SPACE TO DUMMY-RECORD.                                  SG1054.2
056900 BLANK-LINE-PRINT.                                                SG1054.2
057000     PERFORM WRT-LN.                                              SG1054.2
057100 FAIL-ROUTINE.                                                    SG1054.2
057200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SG1054.2
057300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SG1054.2
057400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SG1054.2
057500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1054.2
057600     GO TO FAIL-ROUTINE-EX.                                       SG1054.2
057700 FAIL-ROUTINE-WRITE.                                              SG1054.2
057800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SG1054.2
057900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SG1054.2
058000 FAIL-ROUTINE-EX. EXIT.                                           SG1054.2
058100 BAIL-OUT.                                                        SG1054.2
058200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SG1054.2
058300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SG1054.2
058400 BAIL-OUT-WRITE.                                                  SG1054.2
058500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SG1054.2
058600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SG1054.2
058700 BAIL-OUT-EX. EXIT.                                               SG1054.2
058800 CCVS1-EXIT.                                                      SG1054.2
058900     EXIT.                                                        SG1054.2
059000 OUTPROC-EXIT SECTION 00.                                         SG1054.2
059100 EXIT-ONLY.                                                       SG1054.2
059200     PERFORM  CLOSE-FILES.                                        SG1054.2
      *END-OF,SG105A                                                                  
