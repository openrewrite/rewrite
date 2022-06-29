      *HEADER,COBOL,SQ121A                                                            
000100 IDENTIFICATION DIVISION.                                         SQ1214.2
000200 PROGRAM-ID.                                                      SQ1214.2
000300     SQ121A.                                                      SQ1214.2
000400****************************************************************  SQ1214.2
000500*                                                              *  SQ1214.2
000600*    VALIDATION FOR:-                                          *  SQ1214.2
000700*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1214.2
000800*                                                              *  SQ1214.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ1214.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1214.2
001100*                                                              *  SQ1214.2
001200****************************************************************  SQ1214.2
001300                                                                  SQ1214.2
001400*         THE ROUTINE SQ121A TESTS THE USE OF THE USE AFTER ERROR SQ1214.2
001500*    PROCEDURE ON I-O.  SQ121A IS BASICALLY A REWRITE OF SQ115A   SQ1214.2
001600*    WITH THE ADDITION OF THE USE PROCEDURE.                      SQ1214.2
001700*        THIS ROUTINE CREATES A MASS STORAGE FILE CONTAINING      SQ1214.2
001800*    550 RECORDS.  EACH RECORD CONTAINS 126 CHARACTERS. THE       SQ1214.2
001900*    FILE IS CLOSED AND OPENED AS AN INPUT-OUTPUT FILE. EVERY     SQ1214.2
002000*    TENTH RECORD IS REWRITTEN.  THE FILE IS CLOSED AND OPENED    SQ1214.2
002100*    AGAIN AS AN INPUT FILE.  FIELDS IN EACH RECORD ARE CHECKED   SQ1214.2
002200*    TO ENSURE THAT THE RECORDS REWRITTEN ARE CORRECT AND THAT    SQ1214.2
002300*    THE RECORDS WHICH WERE NOT UPDATED WERE NOT CHANGED.         SQ1214.2
002400*    THE READ STATEMENT WITHIN THE REWRITE SECTION OF SQ121A DOES SQ1214.2
002500*    NOT HAVE AN AT END CLAUSE.  EOF PROCESSING IS HANDLED BY     SQ1214.2
002600*    SETTING AN EOF-FLAG IN THE DECLARATIVE SECTION.  ANY         SQ1214.2
002700*    PERMANENT ERRORS ENCOUNTERED DURING THE REWRITE OF SQ-FS5    SQ1214.2
002800*    ARE TREATED AS INFORMATION ITEMS.                            SQ1214.2
002900*                                                                 SQ1214.2
003000*    USED X-CARDS:                                                SQ1214.2
003100*         XXXXX014                                                SQ1214.2
003200*         XXXXX055                                                SQ1214.2
003300*     P   XXXXX062                                                SQ1214.2
003400*         XXXXX082                                                SQ1214.2
003500*         XXXXX083                                                SQ1214.2
003600*     C   XXXXX084                                                SQ1214.2
003700*                                                                 SQ1214.2
003800*                                                                 SQ1214.2
003900 ENVIRONMENT DIVISION.                                            SQ1214.2
004000 CONFIGURATION SECTION.                                           SQ1214.2
004100 SOURCE-COMPUTER.                                                 SQ1214.2
004200     XXXXX082.                                                    SQ1214.2
004300 OBJECT-COMPUTER.                                                 SQ1214.2
004400     XXXXX083.                                                    SQ1214.2
004500 INPUT-OUTPUT SECTION.                                            SQ1214.2
004600 FILE-CONTROL.                                                    SQ1214.2
004700*    SELECT RAW-DATA   ASSIGN TO                                  SQ1214.2
004800*    XXXXX062                                                     SQ1214.2
004900*           ORGANIZATION IS INDEXED                               SQ1214.2
005000*           ACCESS MODE IS RANDOM                                 SQ1214.2
005100*           RECORD KEY IS RAW-DATA-KEY.                           SQ1214.2
005200     SELECT PRINT-FILE ASSIGN TO                                  SQ1214.2
005300     XXXXX055.                                                    SQ1214.2
005400     SELECT SQ-FS5 ASSIGN                                         SQ1214.2
005500     XXXXX014                                                     SQ1214.2
005600     ORGANIZATION SEQUENTIAL                                      SQ1214.2
005700     ACCESS MODE SEQUENTIAL                                       SQ1214.2
005800     FILE STATUS IS STAT-GROUP.                                   SQ1214.2
005900 DATA DIVISION.                                                   SQ1214.2
006000 FILE SECTION.                                                    SQ1214.2
006100*                                                                 SQ1214.2
006200*FD  RAW-DATA.                                                    SQ1214.2
006300*                                                                 SQ1214.2
006400*01  RAW-DATA-SATZ.                                               SQ1214.2
006500*    05  RAW-DATA-KEY        PIC X(6).                            SQ1214.2
006600*    05  C-DATE              PIC 9(6).                            SQ1214.2
006700*    05  C-TIME              PIC 9(8).                            SQ1214.2
006800*    05  C-NO-OF-TESTS       PIC 99.                              SQ1214.2
006900*    05  C-OK                PIC 999.                             SQ1214.2
007000*    05  C-ALL               PIC 999.                             SQ1214.2
007100*    05  C-FAIL              PIC 999.                             SQ1214.2
007200*    05  C-DELETED           PIC 999.                             SQ1214.2
007300*    05  C-INSPECT           PIC 999.                             SQ1214.2
007400*    05  C-NOTE              PIC X(13).                           SQ1214.2
007500*    05  C-INDENT            PIC X.                               SQ1214.2
007600*    05  C-ABORT             PIC X(8).                            SQ1214.2
007700 FD  PRINT-FILE                                                   SQ1214.2
007800C    LABEL RECORDS                                                SQ1214.2
007900C    XXXXX084                                                     SQ1214.2
008000C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ1214.2
008100               .                                                  SQ1214.2
008200 01  PRINT-REC PICTURE X(120).                                    SQ1214.2
008300 01  DUMMY-RECORD PICTURE X(120).                                 SQ1214.2
008400 FD  SQ-FS5                                                       SQ1214.2
008500C    LABEL RECORD STANDARD                                        SQ1214.2
008600               .                                                  SQ1214.2
008700 01  SQ-FS5R1-F-G-126.                                            SQ1214.2
008800     02  SQ-FS5-120  PICTURE X(120).                              SQ1214.2
008900     02  SQ-FS5-UPDATE PICTURE X(6).                              SQ1214.2
009000 WORKING-STORAGE SECTION.                                         SQ1214.2
009100 01  COUNT-OF-RECORDS PIC S9(5) COMPUTATIONAL.                    SQ1214.2
009200 01  RECORDS-IN-ERROR PIC S9(5) COMP VALUE ZERO.                  SQ1214.2
009300 01  ERROR-FLAG PIC 9.                                            SQ1214.2
009400 01  STAT-GROUP.                                                  SQ1214.2
009500     02 INPUT-STAT1     PIC X.                                    SQ1214.2
009600     02 INPUT-STAT2     PIC X.                                    SQ1214.2
009700 01  EOF-FLAG          PIC 9 VALUE 0.                             SQ1214.2
009800 01  PERM-ERRORS       PIC 9 VALUE 0.                             SQ1214.2
009900 01  LOOP-COUNT PIC 99.                                           SQ1214.2
010000 01  FILE-RECORD-INFORMATION-REC.                                 SQ1214.2
010100     03 FILE-RECORD-INFO-SKELETON.                                SQ1214.2
010200        05 FILLER                 PICTURE X(48)       VALUE       SQ1214.2
010300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ1214.2
010400        05 FILLER                 PICTURE X(46)       VALUE       SQ1214.2
010500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ1214.2
010600        05 FILLER                 PICTURE X(26)       VALUE       SQ1214.2
010700             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ1214.2
010800        05 FILLER                 PICTURE X(37)       VALUE       SQ1214.2
010900             ",RECKEY=                             ".             SQ1214.2
011000        05 FILLER                 PICTURE X(38)       VALUE       SQ1214.2
011100             ",ALTKEY1=                             ".            SQ1214.2
011200        05 FILLER                 PICTURE X(38)       VALUE       SQ1214.2
011300             ",ALTKEY2=                             ".            SQ1214.2
011400        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ1214.2
011500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ1214.2
011600        05 FILE-RECORD-INFO-P1-120.                               SQ1214.2
011700           07 FILLER              PIC X(5).                       SQ1214.2
011800           07 XFILE-NAME           PIC X(6).                      SQ1214.2
011900           07 FILLER              PIC X(8).                       SQ1214.2
012000           07 XRECORD-NAME         PIC X(6).                      SQ1214.2
012100           07 FILLER              PIC X(1).                       SQ1214.2
012200           07 REELUNIT-NUMBER     PIC 9(1).                       SQ1214.2
012300           07 FILLER              PIC X(7).                       SQ1214.2
012400           07 XRECORD-NUMBER       PIC 9(6).                      SQ1214.2
012500           07 FILLER              PIC X(6).                       SQ1214.2
012600           07 UPDATE-NUMBER       PIC 9(2).                       SQ1214.2
012700           07 FILLER              PIC X(5).                       SQ1214.2
012800           07 ODO-NUMBER          PIC 9(4).                       SQ1214.2
012900           07 FILLER              PIC X(5).                       SQ1214.2
013000           07 XPROGRAM-NAME        PIC X(5).                      SQ1214.2
013100           07 FILLER              PIC X(7).                       SQ1214.2
013200           07 XRECORD-LENGTH       PIC 9(6).                      SQ1214.2
013300           07 FILLER              PIC X(7).                       SQ1214.2
013400           07 CHARS-OR-RECORDS    PIC X(2).                       SQ1214.2
013500           07 FILLER              PIC X(1).                       SQ1214.2
013600           07 XBLOCK-SIZE          PIC 9(4).                      SQ1214.2
013700           07 FILLER              PIC X(6).                       SQ1214.2
013800           07 RECORDS-IN-FILE     PIC 9(6).                       SQ1214.2
013900           07 FILLER              PIC X(5).                       SQ1214.2
014000           07 XFILE-ORGANIZATION   PIC X(2).                      SQ1214.2
014100           07 FILLER              PIC X(6).                       SQ1214.2
014200           07 XLABEL-TYPE          PIC X(1).                      SQ1214.2
014300        05 FILE-RECORD-INFO-P121-240.                             SQ1214.2
014400           07 FILLER              PIC X(8).                       SQ1214.2
014500           07 XRECORD-KEY          PIC X(29).                     SQ1214.2
014600           07 FILLER              PIC X(9).                       SQ1214.2
014700           07 ALTERNATE-KEY1      PIC X(29).                      SQ1214.2
014800           07 FILLER              PIC X(9).                       SQ1214.2
014900           07 ALTERNATE-KEY2      PIC X(29).                      SQ1214.2
015000           07 FILLER              PIC X(7).                       SQ1214.2
015100 01  TEST-RESULTS.                                                SQ1214.2
015200     02 FILLER                    PICTURE X VALUE SPACE.          SQ1214.2
015300     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ1214.2
015400     02 FILLER                    PICTURE X VALUE SPACE.          SQ1214.2
015500     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ1214.2
015600     02 FILLER                    PICTURE X  VALUE SPACE.         SQ1214.2
015700     02  PAR-NAME.                                                SQ1214.2
015800       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ1214.2
015900       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ1214.2
016000       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ1214.2
016100       03 FILLER PIC X(5) VALUE SPACE.                            SQ1214.2
016200     02 FILLER PIC X(10) VALUE SPACE.                             SQ1214.2
016300     02 RE-MARK PIC X(61).                                        SQ1214.2
016400 01  TEST-COMPUTED.                                               SQ1214.2
016500     02 FILLER PIC X(30) VALUE SPACE.                             SQ1214.2
016600     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ1214.2
016700     02 COMPUTED-X.                                               SQ1214.2
016800     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ1214.2
016900     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ1214.2
017000     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ1214.2
017100     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ1214.2
017200     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ1214.2
017300     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ1214.2
017400         04 COMPUTED-18V0                   PICTURE -9(18).       SQ1214.2
017500         04 FILLER                          PICTURE X.            SQ1214.2
017600     03 FILLER PIC X(50) VALUE SPACE.                             SQ1214.2
017700 01  TEST-CORRECT.                                                SQ1214.2
017800     02 FILLER PIC X(30) VALUE SPACE.                             SQ1214.2
017900     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ1214.2
018000     02 CORRECT-X.                                                SQ1214.2
018100     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ1214.2
018200     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ1214.2
018300     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ1214.2
018400     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ1214.2
018500     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ1214.2
018600     03      CR-18V0 REDEFINES CORRECT-A.                         SQ1214.2
018700         04 CORRECT-18V0                    PICTURE -9(18).       SQ1214.2
018800         04 FILLER                          PICTURE X.            SQ1214.2
018900     03 FILLER PIC X(50) VALUE SPACE.                             SQ1214.2
019000 01  CCVS-C-1.                                                    SQ1214.2
019100     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ1214.2
019200-    "SS  PARAGRAPH-NAME                                          SQ1214.2
019300-    "        REMARKS".                                           SQ1214.2
019400     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ1214.2
019500 01  CCVS-C-2.                                                    SQ1214.2
019600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1214.2
019700     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ1214.2
019800     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ1214.2
019900     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ1214.2
020000     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ1214.2
020100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ1214.2
020200 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ1214.2
020300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ1214.2
020400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ1214.2
020500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ1214.2
020600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ1214.2
020700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ1214.2
020800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ1214.2
020900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ1214.2
021000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ1214.2
021100 01  CCVS-H-1.                                                    SQ1214.2
021200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ1214.2
021300     02 FILLER PICTURE X(67) VALUE                                SQ1214.2
021400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ1214.2
021500-    " SYSTEM".                                                   SQ1214.2
021600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ1214.2
021700 01  CCVS-H-2.                                                    SQ1214.2
021800     02 FILLER PICTURE X(52) VALUE IS                             SQ1214.2
021900     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ1214.2
022000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ1214.2
022100     02 TEST-ID PICTURE IS X(9).                                  SQ1214.2
022200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ1214.2
022300 01  CCVS-H-3.                                                    SQ1214.2
022400     02  FILLER PICTURE X(34) VALUE                               SQ1214.2
022500     " FOR OFFICIAL USE ONLY    ".                                SQ1214.2
022600     02  FILLER PICTURE X(58) VALUE                               SQ1214.2
022700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ1214.2
022800     02  FILLER PICTURE X(28) VALUE                               SQ1214.2
022900     "  COPYRIGHT   1985 ".                                       SQ1214.2
023000 01  CCVS-E-1.                                                    SQ1214.2
023100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ1214.2
023200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ1214.2
023300     02 ID-AGAIN PICTURE IS X(9).                                 SQ1214.2
023400     02 FILLER PICTURE X(45) VALUE IS                             SQ1214.2
023500     " NTIS DISTRIBUTION COBOL 85".                               SQ1214.2
023600 01  CCVS-E-2.                                                    SQ1214.2
023700     02  FILLER                   PICTURE X(31)  VALUE            SQ1214.2
023800     SPACE.                                                       SQ1214.2
023900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ1214.2
024000     02 CCVS-E-2-2.                                               SQ1214.2
024100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ1214.2
024200         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ1214.2
024300         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ1214.2
024400 01  CCVS-E-3.                                                    SQ1214.2
024500     02  FILLER PICTURE X(22) VALUE                               SQ1214.2
024600     " FOR OFFICIAL USE ONLY".                                    SQ1214.2
024700     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ1214.2
024800     02  FILLER PICTURE X(58) VALUE                               SQ1214.2
024900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ1214.2
025000     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ1214.2
025100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ1214.2
025200 01  CCVS-E-4.                                                    SQ1214.2
025300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ1214.2
025400     02 FILLER PIC XXXX VALUE " OF ".                             SQ1214.2
025500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ1214.2
025600     02 FILLER PIC X(40) VALUE                                    SQ1214.2
025700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ1214.2
025800 01  XXINFO.                                                      SQ1214.2
025900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ1214.2
026000     02 INFO-TEXT.                                                SQ1214.2
026100     04 FILLER PIC X(20) VALUE SPACE.                             SQ1214.2
026200     04 XXCOMPUTED PIC X(20).                                     SQ1214.2
026300     04 FILLER PIC X(5) VALUE SPACE.                              SQ1214.2
026400     04 XXCORRECT PIC X(20).                                      SQ1214.2
026500 01  HYPHEN-LINE.                                                 SQ1214.2
026600     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ1214.2
026700     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ1214.2
026800-    "*****************************************".                 SQ1214.2
026900     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ1214.2
027000-    "******************************".                            SQ1214.2
027100 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ1214.2
027200     "SQ121A".                                                    SQ1214.2
027300 PROCEDURE DIVISION.                                              SQ1214.2
027400 DECLARATIVES.                                                    SQ1214.2
027500 SECT-SQ121A-0001 SECTION.                                        SQ1214.2
027600     USE AFTER STANDARD ERROR PROCEDURE ON I-O.                   SQ1214.2
027700 I-O-ERROR-PROCESS.                                               SQ1214.2
027800     IF INPUT-STAT1 EQUAL TO "1"                                  SQ1214.2
027900          MOVE 1 TO EOF-FLAG.                                     SQ1214.2
028000     IF INPUT-STAT1 GREATER THAN "1"                              SQ1214.2
028100          MOVE 1 TO PERM-ERRORS.                                  SQ1214.2
028200 END DECLARATIVES.                                                SQ1214.2
028300 CCVS1 SECTION.                                                   SQ1214.2
028400 OPEN-FILES.                                                      SQ1214.2
028500*    OPEN I-O RAW-DATA.                                           SQ1214.2
028600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1214.2
028700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ1214.2
028800*    MOVE "ABORTED " TO C-ABORT.                                  SQ1214.2
028900*    ADD 1 TO C-NO-OF-TESTS.                                      SQ1214.2
029000*    ACCEPT C-DATE  FROM DATE.                                    SQ1214.2
029100*    ACCEPT C-TIME  FROM TIME.                                    SQ1214.2
029200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ1214.2
029300*END-E-1.                                                         SQ1214.2
029400*    CLOSE RAW-DATA.                                              SQ1214.2
029500     OPEN     OUTPUT PRINT-FILE.                                  SQ1214.2
029600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ1214.2
029700     MOVE    SPACE TO TEST-RESULTS.                               SQ1214.2
029800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ1214.2
029900     MOVE ZERO TO REC-SKL-SUB.                                    SQ1214.2
030000     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ1214.2
030100 CCVS-INIT-FILE.                                                  SQ1214.2
030200     ADD 1 TO REC-SKL-SUB.                                        SQ1214.2
030300     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ1214.2
030400                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ1214.2
030500 CCVS-INIT-EXIT.                                                  SQ1214.2
030600     GO TO CCVS1-EXIT.                                            SQ1214.2
030700 CLOSE-FILES.                                                     SQ1214.2
030800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ1214.2
030900*    OPEN I-O RAW-DATA.                                           SQ1214.2
031000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ1214.2
031100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ1214.2
031200*    MOVE "OK.     " TO C-ABORT.                                  SQ1214.2
031300*    MOVE PASS-COUNTER TO C-OK.                                   SQ1214.2
031400*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ1214.2
031500*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ1214.2
031600*    MOVE DELETE-CNT TO C-DELETED.                                SQ1214.2
031700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ1214.2
031800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ1214.2
031900*END-E-2.                                                         SQ1214.2
032000*    CLOSE RAW-DATA.                                              SQ1214.2
032100 TERMINATE-CCVS.                                                  SQ1214.2
032200*    EXIT PROGRAM.                                                SQ1214.2
032300*TERMINATE-CALL.                                                  SQ1214.2
032400     STOP     RUN.                                                SQ1214.2
032500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ1214.2
032600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ1214.2
032700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ1214.2
032800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ1214.2
032900     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ1214.2
033000 PRINT-DETAIL.                                                    SQ1214.2
033100     IF REC-CT NOT EQUAL TO ZERO                                  SQ1214.2
033200             MOVE "." TO PARDOT-X                                 SQ1214.2
033300             MOVE REC-CT TO DOTVALUE.                             SQ1214.2
033400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ1214.2
033500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ1214.2
033600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ1214.2
033700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ1214.2
033800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ1214.2
033900     MOVE SPACE TO CORRECT-X.                                     SQ1214.2
034000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ1214.2
034100     MOVE     SPACE TO RE-MARK.                                   SQ1214.2
034200 HEAD-ROUTINE.                                                    SQ1214.2
034300     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1214.2
034400     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ1214.2
034500     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ1214.2
034600 COLUMN-NAMES-ROUTINE.                                            SQ1214.2
034700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1214.2
034800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1214.2
034900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ1214.2
035000 END-ROUTINE.                                                     SQ1214.2
035100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ1214.2
035200 END-RTN-EXIT.                                                    SQ1214.2
035300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ1214.2
035400 END-ROUTINE-1.                                                   SQ1214.2
035500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ1214.2
035600      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ1214.2
035700      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ1214.2
035800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ1214.2
035900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ1214.2
036000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ1214.2
036100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ1214.2
036200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ1214.2
036300  END-ROUTINE-12.                                                 SQ1214.2
036400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ1214.2
036500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ1214.2
036600         MOVE "NO " TO ERROR-TOTAL                                SQ1214.2
036700         ELSE                                                     SQ1214.2
036800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ1214.2
036900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ1214.2
037000     PERFORM WRITE-LINE.                                          SQ1214.2
037100 END-ROUTINE-13.                                                  SQ1214.2
037200     IF DELETE-CNT IS EQUAL TO ZERO                               SQ1214.2
037300         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ1214.2
037400         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ1214.2
037500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ1214.2
037600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1214.2
037700      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ1214.2
037800          MOVE "NO " TO ERROR-TOTAL                               SQ1214.2
037900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ1214.2
038000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ1214.2
038100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ1214.2
038200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ1214.2
038300 WRITE-LINE.                                                      SQ1214.2
038400     ADD 1 TO RECORD-COUNT.                                       SQ1214.2
038500*    IF RECORD-COUNT GREATER 50                                   SQ1214.2
038600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ1214.2
038700*        MOVE SPACE TO DUMMY-RECORD                               SQ1214.2
038800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ1214.2
038900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ1214.2
039000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ1214.2
039100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ1214.2
039200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ1214.2
039300*        MOVE ZERO TO RECORD-COUNT.                               SQ1214.2
039400     PERFORM WRT-LN.                                              SQ1214.2
039500 WRT-LN.                                                          SQ1214.2
039600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ1214.2
039700     MOVE SPACE TO DUMMY-RECORD.                                  SQ1214.2
039800 BLANK-LINE-PRINT.                                                SQ1214.2
039900     PERFORM WRT-LN.                                              SQ1214.2
040000 FAIL-ROUTINE.                                                    SQ1214.2
040100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ1214.2
040200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ1214.2
040300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ1214.2
040400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1214.2
040500     GO TO FAIL-ROUTINE-EX.                                       SQ1214.2
040600 FAIL-ROUTINE-WRITE.                                              SQ1214.2
040700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ1214.2
040800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ1214.2
040900 FAIL-ROUTINE-EX. EXIT.                                           SQ1214.2
041000 BAIL-OUT.                                                        SQ1214.2
041100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ1214.2
041200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ1214.2
041300 BAIL-OUT-WRITE.                                                  SQ1214.2
041400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ1214.2
041500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ1214.2
041600 BAIL-OUT-EX. EXIT.                                               SQ1214.2
041700 CCVS1-EXIT.                                                      SQ1214.2
041800     EXIT.                                                        SQ1214.2
041900 SECT-SQ-115-0001 SECTION.                                        SQ1214.2
042000 SEQ-INIT-013.                                                    SQ1214.2
042100     MOVE "SQ-FS5" TO XFILE-NAME (1).                             SQ1214.2
042200     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ1214.2
042300     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       SQ1214.2
042400     MOVE 000126 TO XRECORD-LENGTH (1).                           SQ1214.2
042500     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ1214.2
042600     MOVE 0001 TO XBLOCK-SIZE (1).                                SQ1214.2
042700     MOVE 000550 TO RECORDS-IN-FILE (1).                          SQ1214.2
042800     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         SQ1214.2
042900     MOVE "S" TO XLABEL-TYPE (1).                                 SQ1214.2
043000     MOVE 000001 TO XRECORD-NUMBER (1).                           SQ1214.2
043100     OPEN OUTPUT SQ-FS5.                                          SQ1214.2
043200     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1214.2
043300 SEQ-TEST-013.                                                    SQ1214.2
043400     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS5-120.              SQ1214.2
043500     MOVE "FIRST " TO SQ-FS5-UPDATE.                              SQ1214.2
043600     WRITE SQ-FS5R1-F-G-126.                                      SQ1214.2
043700     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1214.2
043800     IF COUNT-OF-RECORDS EQUAL TO 550                             SQ1214.2
043900          GO TO SEQ-WRITE-013.                                    SQ1214.2
044000     ADD 1 TO XRECORD-NUMBER (1).                                 SQ1214.2
044100     GO TO SEQ-TEST-013.                                          SQ1214.2
044200 SEQ-WRITE-013.                                                   SQ1214.2
044300     MOVE "CREATE FILE SQ-FS5" TO FEATURE.                        SQ1214.2
044400     MOVE "SEQ-TEST-013" TO PAR-NAME.                             SQ1214.2
044500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ1214.2
044600     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       SQ1214.2
044700     PERFORM PRINT-DETAIL.                                        SQ1214.2
044800     CLOSE SQ-FS5.                                                SQ1214.2
044900*         A SEQUENTIAL MASS STORAGE FILE WITH 126 CHARACTER       SQ1214.2
045000*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 550 RECORDS.    SQ1214.2
045100 SEQ-INIT-014.                                                    SQ1214.2
045200     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1214.2
045300*        THIS TEST READS AND CHECKS THE FILE CREATED              SQ1214.2
045400*    IN SEQ-TEST-013.                                             SQ1214.2
045500     OPEN INPUT SQ-FS5.                                           SQ1214.2
045600 SEQ-TEST-014.                                                    SQ1214.2
045700     READ SQ-FS5 AT END                                           SQ1214.2
045800          GO TO SEQ-TEST-014-1.                                   SQ1214.2
045900     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1214.2
046000     MOVE SQ-FS5-120 TO FILE-RECORD-INFO-P1-120 (1).              SQ1214.2
046100     IF COUNT-OF-RECORDS GREATER THAN 550                         SQ1214.2
046200         MOVE "MORE THAN 550 RECORDS" TO RE-MARK                  SQ1214.2
046300         GO TO SEQ-FAIL-014.                                      SQ1214.2
046400     IF COUNT-OF-RECORDS NOT EQUAL TO XRECORD-NUMBER (1)          SQ1214.2
046500         ADD 1 TO RECORDS-IN-ERROR                                SQ1214.2
046600         GO TO SEQ-TEST-014.                                      SQ1214.2
046700     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS5"                      SQ1214.2
046800         ADD 1 TO RECORDS-IN-ERROR                                SQ1214.2
046900         GO TO SEQ-TEST-014.                                      SQ1214.2
047000     IF UPDATE-NUMBER (1) NOT EQUAL TO ZERO                       SQ1214.2
047100         ADD 1 TO RECORDS-IN-ERROR                                SQ1214.2
047200         GO TO SEQ-TEST-014.                                      SQ1214.2
047300     IF SQ-FS5-UPDATE EQUAL TO "FIRST "                           SQ1214.2
047400         GO TO SEQ-TEST-014.                                      SQ1214.2
047500     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1214.2
047600     GO TO SEQ-TEST-014.                                          SQ1214.2
047700 SEQ-TEST-014-1.                                                  SQ1214.2
047800     IF RECORDS-IN-ERROR EQUAL TO ZERO                            SQ1214.2
047900         GO TO SEQ-PASS-014.                                      SQ1214.2
048000     MOVE "ERRORS IN READING SQ-FS5" TO RE-MARK.                  SQ1214.2
048100 SEQ-FAIL-014.                                                    SQ1214.2
048200     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     SQ1214.2
048300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       SQ1214.2
048400     PERFORM FAIL.                                                SQ1214.2
048500     GO TO SEQ-WRITE-014.                                         SQ1214.2
048600 SEQ-PASS-014.                                                    SQ1214.2
048700     PERFORM PASS.                                                SQ1214.2
048800     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   SQ1214.2
048900     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       SQ1214.2
049000 SEQ-WRITE-014.                                                   SQ1214.2
049100     MOVE "SEQ-TEST-014" TO PAR-NAME.                             SQ1214.2
049200     MOVE "VERIFY FILE SQ-FS5" TO FEATURE.                        SQ1214.2
049300     PERFORM PRINT-DETAIL.                                        SQ1214.2
049400 SEQ-CLOSE-014.                                                   SQ1214.2
049500     CLOSE SQ-FS5.                                                SQ1214.2
049600 REWRITE-INIT-GF-01.                                              SQ1214.2
049700     OPEN I-O SQ-FS5.                                             SQ1214.2
049800     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1214.2
049900     MOVE ZERO TO EOF-FLAG.                                       SQ1214.2
050000*        THIS TEST REWRITES EVERY TENTH RECORD                    SQ1214.2
050100*    OF THE FILE SQ-FS5.                                          SQ1214.2
050200 REWRITE-TEST-GF-01.                                              SQ1214.2
050300     PERFORM READ-SQ-FS5 THRU READ-SQ-FS5-EXIT 10 TIMES.          SQ1214.2
050400     IF EOF-FLAG EQUAL TO 1                                       SQ1214.2
050500          GO TO REWRITE-TEST-GF-01-1.                             SQ1214.2
050600     MOVE SQ-FS5-120 TO FILE-RECORD-INFO-P1-120 (1).              SQ1214.2
050700     ADD 1 TO UPDATE-NUMBER (1).                                  SQ1214.2
050800     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS5-120.              SQ1214.2
050900     MOVE "SECOND" TO SQ-FS5-UPDATE.                              SQ1214.2
051000     REWRITE SQ-FS5R1-F-G-126.                                    SQ1214.2
051100     GO TO REWRITE-TEST-GF-01.                                    SQ1214.2
051200 READ-SQ-FS5.                                                     SQ1214.2
051300     IF EOF-FLAG EQUAL TO 1                                       SQ1214.2
051400          GO TO READ-SQ-FS5-EXIT.                                 SQ1214.2
051500     READ SQ-FS5 RECORD.                                          SQ1214.2
051600     IF EOF-FLAG EQUAL TO 1                                       SQ1214.2
051700          GO TO READ-SQ-FS5-EXIT.                                 SQ1214.2
051800     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1214.2
051900 READ-SQ-FS5-EXIT.                                                SQ1214.2
052000     EXIT.                                                        SQ1214.2
052100 REWRITE-TEST-GF-01-1.                                            SQ1214.2
052200     IF COUNT-OF-RECORDS EQUAL TO 550                             SQ1214.2
052300         GO TO REWRITE-PASS-GF-01.                                SQ1214.2
052400 REWRITE-FAIL-GF-01.                                              SQ1214.2
052500     MOVE "VII-48 4.5.2                              " TO RE-MARK.SQ1214.2
052600     PERFORM FAIL.                                                SQ1214.2
052700     MOVE "550 RECORDS SHOULD BE READ" TO RE-MARK.                SQ1214.2
052800     MOVE "RECORDS READ =" TO COMPUTED-A.                         SQ1214.2
052900     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       SQ1214.2
053000     GO TO REWRITE-WRITE-GF-01.                                   SQ1214.2
053100 REWRITE-PASS-GF-01.                                              SQ1214.2
053200     PERFORM PASS.                                                SQ1214.2
053300 REWRITE-WRITE-GF-01.                                             SQ1214.2
053400     MOVE "RWRT-TEST-GF-01" TO PAR-NAME.                          SQ1214.2
053500     MOVE "REWRITE FILE SQ-FS5" TO FEATURE.                       SQ1214.2
053600     PERFORM PRINT-DETAIL.                                        SQ1214.2
053700     IF PERM-ERRORS EQUAL TO 1                                    SQ1214.2
053800     MOVE "PERMANENT ERRORS ENCOUNTERED ON PREVIOUS I-O OPERATION"SQ1214.2
053900          TO PRINT-REC                                            SQ1214.2
054000          PERFORM WRITE-LINE.                                     SQ1214.2
054100 REWRITE-CLOSE-GF-01.                                             SQ1214.2
054200     CLOSE SQ-FS5.                                                SQ1214.2
054300 REWRITE-INIT-GF-02.                                              SQ1214.2
054400     MOVE ZERO TO COUNT-OF-RECORDS.                               SQ1214.2
054500     MOVE ZERO TO EOF-FLAG.                                       SQ1214.2
054600     OPEN INPUT SQ-FS5.                                           SQ1214.2
054700*        THIS TEST READS AND CHECKS THE FILE WHICH WAS            SQ1214.2
054800*    REWRITTEN IN REWRITE-TEST-01.                                SQ1214.2
054900     MOVE ZERO TO RECORDS-IN-ERROR.                               SQ1214.2
055000     MOVE ZERO TO LOOP-COUNT.                                     SQ1214.2
055100 REWRITE-TEST-GF-02.                                              SQ1214.2
055200     READ SQ-FS5  END                                             SQ1214.2
055300          MOVE 1 TO EOF-FLAG                                      SQ1214.2
055400          GO TO REWRITE-TEST-GF-02-2.                             SQ1214.2
055500     ADD 1 TO COUNT-OF-RECORDS.                                   SQ1214.2
055600     IF COUNT-OF-RECORDS GREATER THAN 550                         SQ1214.2
055700          MOVE "MORE THAN 550 RECORDS" TO RE-MARK                 SQ1214.2
055800          GO TO REWRITE-FAIL-GF-02.                               SQ1214.2
055900     ADD 1 TO LOOP-COUNT.                                         SQ1214.2
056000     MOVE SQ-FS5-120 TO FILE-RECORD-INFO-P1-120 (1).              SQ1214.2
056100     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS5"                      SQ1214.2
056200          ADD 1 TO RECORDS-IN-ERROR                               SQ1214.2
056300          GO TO REWRITE-TEST-GF-02.                               SQ1214.2
056400     IF LOOP-COUNT EQUAL TO 10                                    SQ1214.2
056500          MOVE ZERO TO LOOP-COUNT                                 SQ1214.2
056600          GO TO REWRITE-TEST-GF-02-1.                             SQ1214.2
056700     IF UPDATE-NUMBER (1) NOT EQUAL TO ZERO                       SQ1214.2
056800          ADD 1 TO RECORDS-IN-ERROR                               SQ1214.2
056900          GO TO REWRITE-TEST-GF-02.                               SQ1214.2
057000     IF SQ-FS5-UPDATE EQUAL TO "FIRST "                           SQ1214.2
057100          GO TO REWRITE-TEST-GF-02.                               SQ1214.2
057200     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1214.2
057300     GO TO REWRITE-TEST-GF-02.                                    SQ1214.2
057400 REWRITE-TEST-GF-02-1.                                            SQ1214.2
057500     IF UPDATE-NUMBER (1) NOT EQUAL TO 1                          SQ1214.2
057600          ADD 1 TO RECORDS-IN-ERROR                               SQ1214.2
057700          GO TO REWRITE-TEST-GF-02.                               SQ1214.2
057800     IF SQ-FS5-UPDATE EQUAL TO "SECOND"                           SQ1214.2
057900          GO TO REWRITE-TEST-GF-02.                               SQ1214.2
058000     ADD 1 TO RECORDS-IN-ERROR.                                   SQ1214.2
058100     GO TO REWRITE-TEST-GF-02.                                    SQ1214.2
058200 REWRITE-TEST-GF-02-2.                                            SQ1214.2
058300     IF COUNT-OF-RECORDS NOT EQUAL TO 550                         SQ1214.2
058400         MOVE "LESS THAN 550 RECORDS" TO RE-MARK                  SQ1214.2
058500         MOVE "RECORDS READ =" TO COMPUTED-A                      SQ1214.2
058600         MOVE COUNT-OF-RECORDS TO CORRECT-18V0                    SQ1214.2
058700         GO TO REWRITE-FAIL-GF-02.                                SQ1214.2
058800     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        SQ1214.2
058900         MOVE "ERRORS IN READING SQ-FS5" TO RE-MARK               SQ1214.2
059000         MOVE "RECORDS IN ERROR =" TO COMPUTED-A                  SQ1214.2
059100         MOVE RECORDS-IN-ERROR TO CORRECT-18V0                    SQ1214.2
059200         GO TO REWRITE-FAIL-GF-02.                                SQ1214.2
059300 REWRITE-PASS-GF-02.                                              SQ1214.2
059400     PERFORM PASS.                                                SQ1214.2
059500     GO TO REWRITE-WRITE-GF-02.                                   SQ1214.2
059600 REWRITE-FAIL-GF-02.                                              SQ1214.2
059700     PERFORM FAIL.                                                SQ1214.2
059800 REWRITE-WRITE-GF-02.                                             SQ1214.2
059900     MOVE "RWRT-TEST-GF-02" TO PAR-NAME.                          SQ1214.2
060000     MOVE "VERIFY FILE SQ-FS5" TO FEATURE.                        SQ1214.2
060100     PERFORM PRINT-DETAIL.                                        SQ1214.2
060200 REWRITE-CLOSE-GF-02.                                             SQ1214.2
060300     CLOSE SQ-FS5.                                                SQ1214.2
060400 TERMINATE-ROUTINE.                                               SQ1214.2
060500     EXIT.                                                        SQ1214.2
060600 CCVS-EXIT SECTION.                                               SQ1214.2
060700 CCVS-999999.                                                     SQ1214.2
060800     GO TO CLOSE-FILES.                                           SQ1214.2
      *END-OF,SQ121A                                                                  
