      *HEADER,COBOL,SQ202A,SUBPRG,SQ203A                                              
000100 IDENTIFICATION DIVISION.                                         SQ2034.2
000200 PROGRAM-ID.                                                      SQ2034.2
000300     SQ203A.                                                      SQ2034.2
000400****************************************************************  SQ2034.2
000500*                                                              *  SQ2034.2
000600*    VALIDATION FOR:-                                          *  SQ2034.2
000700*    " HIGH       ".                                              SQ2034.2
000800*                                                              *  SQ2034.2
000900*    CREATION DATE     /     VALIDATION DATE                   *  SQ2034.2
001000*    "4.2 ".                                                      SQ2034.2
001100*                                                              *  SQ2034.2
001200*         THE ROUTINE SQ203A TESTS THE USE OF THE OPTIONAL CLAUSE SQ2034.2
001300*    IN THE SELECT CLAUSE OF A FILE CONTROL ENTRY.  THE TEST IS   SQ2034.2
001400*    MADE WHEN THE OPTIONAL FILE IS BOTH PRESENT AND ABSENT.      SQ2034.2
001500*    THE RESERVE INTEGER AREA CLAUSE IS ALSO INCLUDE IN THIS TEST.SQ2034.2
001600 ENVIRONMENT DIVISION.                                            SQ2034.2
001700 CONFIGURATION SECTION.                                           SQ2034.2
001800 SOURCE-COMPUTER.                                                 SQ2034.2
001900     XXXXX082.                                                    SQ2034.2
002000 OBJECT-COMPUTER.                                                 SQ2034.2
002100     XXXXX083.                                                    SQ2034.2
002200 INPUT-OUTPUT SECTION.                                            SQ2034.2
002300 FILE-CONTROL.                                                    SQ2034.2
002400*    SELECT RAW-DATA   ASSIGN TO                                  SQ2034.2
002500*    XXXXX062                                                     SQ2034.2
002600*           ORGANIZATION IS INDEXED                               SQ2034.2
002700*           ACCESS MODE IS RANDOM                                 SQ2034.2
002800*           RECORD KEY IS RAW-DATA-KEY.                           SQ2034.2
002900     SELECT PRINT-FILE ASSIGN TO                                  SQ2034.2
003000     XXXXX055.                                                    SQ2034.2
003100     SELECT OPTIONAL SQ-FS1                                       SQ2034.2
003200     ASSIGN TO                                                    SQ2034.2
003300     XXXXD001                                                     SQ2034.2
003400     RESERVE 8 AREAS                                              SQ2034.2
003500     ORGANIZATION IS SEQUENTIAL                                   SQ2034.2
003600     ACCESS MODE IS SEQUENTIAL                                    SQ2034.2
003700     FILE STATUS GRP-STATUS-KEY-1.                                SQ2034.2
003800     SELECT OPTIONAL SQ-FS2                                       SQ2034.2
003900     ASSIGN TO                                                    SQ2034.2
004000     XXXXX018                                                     SQ2034.2
004100     STATUS GRP-STATUS-KEY-2.                                     SQ2034.2
004200     SELECT SQ-FS3 ASSIGN TO                                      SQ2034.2
004300     XXXXX003                                                     SQ2034.2
004400     RESERVE 7 AREA                                               SQ2034.2
004500     ORGANIZATION SEQUENTIAL                                      SQ2034.2
004600     ACCESS SEQUENTIAL                                            SQ2034.2
004700     FILE STATUS IS GRP-STATUS-KEY-3.                             SQ2034.2
004800     SELECT OPTIONAL SQ-FS4 ASSIGN TO                             SQ2034.2
004900     XXXXX017                                                     SQ2034.2
005000     ORGANIZATION IS SEQUENTIAL.                                  SQ2034.2
005100 DATA DIVISION.                                                   SQ2034.2
005200 FILE SECTION.                                                    SQ2034.2
005300*                                                                 SQ2034.2
005400*FD  RAW-DATA.                                                    SQ2034.2
005500*                                                                 SQ2034.2
005600*01  RAW-DATA-SATZ.                                               SQ2034.2
005700*    05  RAW-DATA-KEY        PIC X(6).                            SQ2034.2
005800*    05  C-DATE              PIC 9(6).                            SQ2034.2
005900*    05  C-TIME              PIC 9(8).                            SQ2034.2
006000*    05  C-NO-OF-TESTS       PIC 99.                              SQ2034.2
006100*    05  C-OK                PIC 999.                             SQ2034.2
006200*    05  C-ALL               PIC 999.                             SQ2034.2
006300*    05  C-FAIL              PIC 999.                             SQ2034.2
006400*    05  C-DELETED           PIC 999.                             SQ2034.2
006500*    05  C-INSPECT           PIC 999.                             SQ2034.2
006600*    05  C-NOTE              PIC X(13).                           SQ2034.2
006700*    05  C-INDENT            PIC X.                               SQ2034.2
006800*    05  C-ABORT             PIC X(8).                            SQ2034.2
006900 FD  PRINT-FILE                                                   SQ2034.2
007000C    LABEL RECORDS                                                SQ2034.2
007100C    XXXXX084                                                     SQ2034.2
007200C    DATA RECORD IS PRINT-REC DUMMY-RECORD                        SQ2034.2
007300               .                                                  SQ2034.2
007400 01  PRINT-REC PICTURE X(120).                                    SQ2034.2
007500 01  DUMMY-RECORD PICTURE X(120).                                 SQ2034.2
007600 FD  SQ-FS1                                                       SQ2034.2
007700C    LABEL RECORDS ARE STANDARD                                   SQ2034.2
007800     BLOCK CONTAINS 120 CHARACTERS.                               SQ2034.2
007900 01  SQ-FS1R1-F-G-120 PIC X(120).                                 SQ2034.2
008000 FD  SQ-FS2                                                       SQ2034.2
008100C    LABEL RECORDS ARE STANDARD                                   SQ2034.2
008200     BLOCK CONTAINS 120 CHARACTERS.                               SQ2034.2
008300 01  SQ-FS2R1-F-G-120 PIC X(120).                                 SQ2034.2
008400 FD  SQ-FS3                                                       SQ2034.2
008500C    LABEL RECORDS ARE STANDARD                                   SQ2034.2
008600     BLOCK CONTAINS 120 CHARACTERS.                               SQ2034.2
008700 01  SQ-FS3R1-F-G-120 PIC X(120).                                 SQ2034.2
008800 FD  SQ-FS4                                                       SQ2034.2
008900C    LABEL RECORDS ARE STANDARD                                   SQ2034.2
009000     BLOCK CONTAINS 120 CHARACTERS.                               SQ2034.2
009100 01  SQ-FS4R1-F-G-120 PIC X(120).                                 SQ2034.2
009200 WORKING-STORAGE SECTION.                                         SQ2034.2
009300 01  COUNT-OF-RECS PIC 9999.                                      SQ2034.2
009400 01  EOF-FLAG         PIC 99  VALUE 0.                            SQ2034.2
009500 01  GRP-STATUS-KEY-1.                                            SQ2034.2
009600     02 WRK-XN-00001-KEY-1   PIC X.                               SQ2034.2
009700     02 FILLER               PIC X.                               SQ2034.2
009800 01  GRP-STATUS-KEY-2.                                            SQ2034.2
009900     02 WRK-XN-00001-KEY-2   PIC X.                               SQ2034.2
010000     02 FILLER               PIC X.                               SQ2034.2
010100 01  GRP-STATUS-KEY-3.                                            SQ2034.2
010200     02 WRK-XN-00001-KEY-3   PIC X.                               SQ2034.2
010300     02 FILLER               PIC X.                               SQ2034.2
010400 01  FILE-RECORD-INFORMATION-REC.                                 SQ2034.2
010500     03 FILE-RECORD-INFO-SKELETON.                                SQ2034.2
010600        05 FILLER                 PICTURE X(48)       VALUE       SQ2034.2
010700             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  SQ2034.2
010800        05 FILLER                 PICTURE X(46)       VALUE       SQ2034.2
010900             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    SQ2034.2
011000        05 FILLER                 PICTURE X(26)       VALUE       SQ2034.2
011100             ",LFIL=000000,ORG=  ,LBLR= ".                        SQ2034.2
011200        05 FILLER                 PICTURE X(37)       VALUE       SQ2034.2
011300             ",RECKEY=                             ".             SQ2034.2
011400        05 FILLER                 PICTURE X(38)       VALUE       SQ2034.2
011500             ",ALTKEY1=                             ".            SQ2034.2
011600        05 FILLER                 PICTURE X(38)       VALUE       SQ2034.2
011700             ",ALTKEY2=                             ".            SQ2034.2
011800        05 FILLER                 PICTURE X(7)        VALUE SPACE.SQ2034.2
011900     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              SQ2034.2
012000        05 FILE-RECORD-INFO-P1-120.                               SQ2034.2
012100           07 FILLER              PIC X(5).                       SQ2034.2
012200           07 XFILE-NAME           PIC X(6).                      SQ2034.2
012300           07 FILLER              PIC X(8).                       SQ2034.2
012400           07 XRECORD-NAME         PIC X(6).                      SQ2034.2
012500           07 FILLER              PIC X(1).                       SQ2034.2
012600           07 REELUNIT-NUMBER     PIC 9(1).                       SQ2034.2
012700           07 FILLER              PIC X(7).                       SQ2034.2
012800           07 XRECORD-NUMBER       PIC 9(6).                      SQ2034.2
012900           07 FILLER              PIC X(6).                       SQ2034.2
013000           07 UPDATE-NUMBER       PIC 9(2).                       SQ2034.2
013100           07 FILLER              PIC X(5).                       SQ2034.2
013200           07 ODO-NUMBER          PIC 9(4).                       SQ2034.2
013300           07 FILLER              PIC X(5).                       SQ2034.2
013400           07 XPROGRAM-NAME        PIC X(5).                      SQ2034.2
013500           07 FILLER              PIC X(7).                       SQ2034.2
013600           07 XRECORD-LENGTH       PIC 9(6).                      SQ2034.2
013700           07 FILLER              PIC X(7).                       SQ2034.2
013800           07 CHARS-OR-RECORDS    PIC X(2).                       SQ2034.2
013900           07 FILLER              PIC X(1).                       SQ2034.2
014000           07 XBLOCK-SIZE          PIC 9(4).                      SQ2034.2
014100           07 FILLER              PIC X(6).                       SQ2034.2
014200           07 RECORDS-IN-FILE     PIC 9(6).                       SQ2034.2
014300           07 FILLER              PIC X(5).                       SQ2034.2
014400           07 XFILE-ORGANIZATION   PIC X(2).                      SQ2034.2
014500           07 FILLER              PIC X(6).                       SQ2034.2
014600           07 XLABEL-TYPE          PIC X(1).                      SQ2034.2
014700        05 FILE-RECORD-INFO-P121-240.                             SQ2034.2
014800           07 FILLER              PIC X(8).                       SQ2034.2
014900           07 XRECORD-KEY          PIC X(29).                     SQ2034.2
015000           07 FILLER              PIC X(9).                       SQ2034.2
015100           07 ALTERNATE-KEY1      PIC X(29).                      SQ2034.2
015200           07 FILLER              PIC X(9).                       SQ2034.2
015300           07 ALTERNATE-KEY2      PIC X(29).                      SQ2034.2
015400           07 FILLER              PIC X(7).                       SQ2034.2
015500 01  TEST-RESULTS.                                                SQ2034.2
015600     02 FILLER                    PICTURE X VALUE SPACE.          SQ2034.2
015700     02 FEATURE                   PICTURE X(20) VALUE SPACE.      SQ2034.2
015800     02 FILLER                    PICTURE X VALUE SPACE.          SQ2034.2
015900     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       SQ2034.2
016000     02 FILLER                    PICTURE X  VALUE SPACE.         SQ2034.2
016100     02  PAR-NAME.                                                SQ2034.2
016200       03 FILLER PICTURE X(12) VALUE SPACE.                       SQ2034.2
016300       03  PARDOT-X PICTURE X  VALUE SPACE.                       SQ2034.2
016400       03 DOTVALUE PICTURE 99  VALUE ZERO.                        SQ2034.2
016500       03 FILLER PIC X(5) VALUE SPACE.                            SQ2034.2
016600     02 FILLER PIC X(10) VALUE SPACE.                             SQ2034.2
016700     02 RE-MARK PIC X(61).                                        SQ2034.2
016800 01  TEST-COMPUTED.                                               SQ2034.2
016900     02 FILLER PIC X(30) VALUE SPACE.                             SQ2034.2
017000     02 FILLER PIC X(17) VALUE "       COMPUTED=".                SQ2034.2
017100     02 COMPUTED-X.                                               SQ2034.2
017200     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      SQ2034.2
017300     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       SQ2034.2
017400     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      SQ2034.2
017500     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  SQ2034.2
017600     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  SQ2034.2
017700     03       CM-18V0 REDEFINES COMPUTED-A.                       SQ2034.2
017800         04 COMPUTED-18V0                   PICTURE -9(18).       SQ2034.2
017900         04 FILLER                          PICTURE X.            SQ2034.2
018000     03 FILLER PIC X(50) VALUE SPACE.                             SQ2034.2
018100 01  TEST-CORRECT.                                                SQ2034.2
018200     02 FILLER PIC X(30) VALUE SPACE.                             SQ2034.2
018300     02 FILLER PIC X(17) VALUE "       CORRECT =".                SQ2034.2
018400     02 CORRECT-X.                                                SQ2034.2
018500     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      SQ2034.2
018600     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         SQ2034.2
018700     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      SQ2034.2
018800     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  SQ2034.2
018900     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  SQ2034.2
019000     03      CR-18V0 REDEFINES CORRECT-A.                         SQ2034.2
019100         04 CORRECT-18V0                    PICTURE -9(18).       SQ2034.2
019200         04 FILLER                          PICTURE X.            SQ2034.2
019300     03 FILLER PIC X(50) VALUE SPACE.                             SQ2034.2
019400 01  CCVS-C-1.                                                    SQ2034.2
019500     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PASQ2034.2
019600-    "SS  PARAGRAPH-NAME                                          SQ2034.2
019700-    "        REMARKS".                                           SQ2034.2
019800     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   SQ2034.2
019900 01  CCVS-C-2.                                                    SQ2034.2
020000     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2034.2
020100     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 SQ2034.2
020200     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   SQ2034.2
020300     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   SQ2034.2
020400     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   SQ2034.2
020500 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         SQ2034.2
020600 01  REC-CT PICTURE 99 VALUE ZERO.                                SQ2034.2
020700 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        SQ2034.2
020800 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  SQ2034.2
020900 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          SQ2034.2
021000 01  PASS-COUNTER PIC 999 VALUE ZERO.                             SQ2034.2
021100 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              SQ2034.2
021200 01  ERROR-HOLD PIC 999 VALUE ZERO.                               SQ2034.2
021300 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           SQ2034.2
021400 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            SQ2034.2
021500 01  CCVS-H-1.                                                    SQ2034.2
021600     02  FILLER   PICTURE X(27)  VALUE SPACE.                     SQ2034.2
021700     02 FILLER PICTURE X(67) VALUE                                SQ2034.2
021800     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  SQ2034.2
021900-    " SYSTEM".                                                   SQ2034.2
022000     02  FILLER     PICTURE X(26)  VALUE SPACE.                   SQ2034.2
022100 01  CCVS-H-2.                                                    SQ2034.2
022200     02 FILLER PICTURE X(52) VALUE IS                             SQ2034.2
022300     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   SQ2034.2
022400     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   SQ2034.2
022500     02 TEST-ID PICTURE IS X(9).                                  SQ2034.2
022600     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   SQ2034.2
022700 01  CCVS-H-3.                                                    SQ2034.2
022800     02  FILLER PICTURE X(34) VALUE                               SQ2034.2
022900     " FOR OFFICIAL USE ONLY    ".                                SQ2034.2
023000     02  FILLER PICTURE X(58) VALUE                               SQ2034.2
023100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".SQ2034.2
023200     02  FILLER PICTURE X(28) VALUE                               SQ2034.2
023300     "  COPYRIGHT   1985 ".                                       SQ2034.2
023400 01  CCVS-E-1.                                                    SQ2034.2
023500     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   SQ2034.2
023600     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        SQ2034.2
023700     02 ID-AGAIN PICTURE IS X(9).                                 SQ2034.2
023800     02 FILLER PICTURE X(45) VALUE IS                             SQ2034.2
023900     " NTIS DISTRIBUTION COBOL 85".                               SQ2034.2
024000 01  CCVS-E-2.                                                    SQ2034.2
024100     02  FILLER                   PICTURE X(31)  VALUE            SQ2034.2
024200     SPACE.                                                       SQ2034.2
024300     02  FILLER                   PICTURE X(21)  VALUE SPACE.     SQ2034.2
024400     02 CCVS-E-2-2.                                               SQ2034.2
024500         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            SQ2034.2
024600         03 FILLER PICTURE IS X VALUE IS SPACE.                   SQ2034.2
024700         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      SQ2034.2
024800 01  CCVS-E-3.                                                    SQ2034.2
024900     02  FILLER PICTURE X(22) VALUE                               SQ2034.2
025000     " FOR OFFICIAL USE ONLY".                                    SQ2034.2
025100     02  FILLER PICTURE X(12) VALUE SPACE.                        SQ2034.2
025200     02  FILLER PICTURE X(58) VALUE                               SQ2034.2
025300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".SQ2034.2
025400     02  FILLER PICTURE X(13) VALUE SPACE.                        SQ2034.2
025500     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 SQ2034.2
025600 01  CCVS-E-4.                                                    SQ2034.2
025700     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           SQ2034.2
025800     02 FILLER PIC XXXX VALUE " OF ".                             SQ2034.2
025900     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           SQ2034.2
026000     02 FILLER PIC X(40) VALUE                                    SQ2034.2
026100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       SQ2034.2
026200 01  XXINFO.                                                      SQ2034.2
026300     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    SQ2034.2
026400     02 INFO-TEXT.                                                SQ2034.2
026500     04 FILLER PIC X(20) VALUE SPACE.                             SQ2034.2
026600     04 XXCOMPUTED PIC X(20).                                     SQ2034.2
026700     04 FILLER PIC X(5) VALUE SPACE.                              SQ2034.2
026800     04 XXCORRECT PIC X(20).                                      SQ2034.2
026900 01  HYPHEN-LINE.                                                 SQ2034.2
027000     02 FILLER PICTURE IS X VALUE IS SPACE.                       SQ2034.2
027100     02 FILLER PICTURE IS X(65) VALUE IS "************************SQ2034.2
027200-    "*****************************************".                 SQ2034.2
027300     02 FILLER PICTURE IS X(54) VALUE IS "************************SQ2034.2
027400-    "******************************".                            SQ2034.2
027500 01  CCVS-PGM-ID PIC X(6) VALUE                                   SQ2034.2
027600     "SQ203A".                                                    SQ2034.2
027700 PROCEDURE DIVISION.                                              SQ2034.2
027800 DECLARATIVES.                                                    SQ2034.2
027900 USE-1 SECTION.                                                   SQ2034.2
028000     USE AFTER STANDARD EXCEPTION PROCEDURE ON INPUT.             SQ2034.2
028100 USE-1-PROCEDURE.                                                 SQ2034.2
028200     IF WRK-XN-00001-KEY-2 EQUAL TO "1"                           SQ2034.2
028300          MOVE 1 TO EOF-FLAG.                                     SQ2034.2
028400 END DECLARATIVES.                                                SQ2034.2
028500 CCVS1 SECTION.                                                   SQ2034.2
028600 OPEN-FILES.                                                      SQ2034.2
028700*    OPEN I-O RAW-DATA.                                           SQ2034.2
028800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2034.2
028900*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     SQ2034.2
029000*    MOVE "ABORTED " TO C-ABORT.                                  SQ2034.2
029100*    ADD 1 TO C-NO-OF-TESTS.                                      SQ2034.2
029200*    ACCEPT C-DATE  FROM DATE.                                    SQ2034.2
029300*    ACCEPT C-TIME  FROM TIME.                                    SQ2034.2
029400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             SQ2034.2
029500*END-E-1.                                                         SQ2034.2
029600*    CLOSE RAW-DATA.                                              SQ2034.2
029700     OPEN     OUTPUT PRINT-FILE.                                  SQ2034.2
029800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   SQ2034.2
029900     MOVE    SPACE TO TEST-RESULTS.                               SQ2034.2
030000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             SQ2034.2
030100     MOVE ZERO TO REC-SKL-SUB.                                    SQ2034.2
030200     PERFORM CCVS-INIT-FILE 9 TIMES.                              SQ2034.2
030300 CCVS-INIT-FILE.                                                  SQ2034.2
030400     ADD 1 TO REC-SKL-SUB.                                        SQ2034.2
030500     MOVE FILE-RECORD-INFO-SKELETON TO                            SQ2034.2
030600                  FILE-RECORD-INFO (REC-SKL-SUB).                 SQ2034.2
030700 CCVS-INIT-EXIT.                                                  SQ2034.2
030800     GO TO CCVS1-EXIT.                                            SQ2034.2
030900 CLOSE-FILES.                                                     SQ2034.2
031000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   SQ2034.2
031100*    OPEN I-O RAW-DATA.                                           SQ2034.2
031200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            SQ2034.2
031300*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     SQ2034.2
031400*    MOVE "OK.     " TO C-ABORT.                                  SQ2034.2
031500*    MOVE PASS-COUNTER TO C-OK.                                   SQ2034.2
031600*    MOVE ERROR-HOLD   TO C-ALL.                                  SQ2034.2
031700*    MOVE ERROR-COUNTER TO C-FAIL.                                SQ2034.2
031800*    MOVE DELETE-CNT TO C-DELETED.                                SQ2034.2
031900*    MOVE INSPECT-COUNTER TO C-INSPECT.                           SQ2034.2
032000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             SQ2034.2
032100*END-E-2.                                                         SQ2034.2
032200*    CLOSE RAW-DATA.                                              SQ2034.2
032300 TERMINATE-CCVS.                                                  SQ2034.2
032400*    EXIT PROGRAM.                                                SQ2034.2
032500*TERMINATE-CALL.                                                  SQ2034.2
032600     STOP     RUN.                                                SQ2034.2
032700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         SQ2034.2
032800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           SQ2034.2
032900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          SQ2034.2
033000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          SQ2034.2
033100     MOVE "****TEST DELETED****" TO RE-MARK.                      SQ2034.2
033200 PRINT-DETAIL.                                                    SQ2034.2
033300     IF REC-CT NOT EQUAL TO ZERO                                  SQ2034.2
033400             MOVE "." TO PARDOT-X                                 SQ2034.2
033500             MOVE REC-CT TO DOTVALUE.                             SQ2034.2
033600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      SQ2034.2
033700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               SQ2034.2
033800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 SQ2034.2
033900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 SQ2034.2
034000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              SQ2034.2
034100     MOVE SPACE TO CORRECT-X.                                     SQ2034.2
034200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         SQ2034.2
034300     MOVE     SPACE TO RE-MARK.                                   SQ2034.2
034400 HEAD-ROUTINE.                                                    SQ2034.2
034500     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2034.2
034600     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   SQ2034.2
034700     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   SQ2034.2
034800 COLUMN-NAMES-ROUTINE.                                            SQ2034.2
034900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2034.2
035000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2034.2
035100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        SQ2034.2
035200 END-ROUTINE.                                                     SQ2034.2
035300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.SQ2034.2
035400 END-RTN-EXIT.                                                    SQ2034.2
035500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   SQ2034.2
035600 END-ROUTINE-1.                                                   SQ2034.2
035700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      SQ2034.2
035800      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   SQ2034.2
035900      ADD PASS-COUNTER TO ERROR-HOLD.                             SQ2034.2
036000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   SQ2034.2
036100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            SQ2034.2
036200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              SQ2034.2
036300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                SQ2034.2
036400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           SQ2034.2
036500  END-ROUTINE-12.                                                 SQ2034.2
036600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        SQ2034.2
036700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      SQ2034.2
036800         MOVE "NO " TO ERROR-TOTAL                                SQ2034.2
036900         ELSE                                                     SQ2034.2
037000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       SQ2034.2
037100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           SQ2034.2
037200     PERFORM WRITE-LINE.                                          SQ2034.2
037300 END-ROUTINE-13.                                                  SQ2034.2
037400     IF DELETE-CNT IS EQUAL TO ZERO                               SQ2034.2
037500         MOVE "NO " TO ERROR-TOTAL  ELSE                          SQ2034.2
037600         MOVE DELETE-CNT TO ERROR-TOTAL.                          SQ2034.2
037700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   SQ2034.2
037800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2034.2
037900      IF   INSPECT-COUNTER EQUAL TO ZERO                          SQ2034.2
038000          MOVE "NO " TO ERROR-TOTAL                               SQ2034.2
038100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   SQ2034.2
038200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            SQ2034.2
038300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          SQ2034.2
038400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           SQ2034.2
038500 WRITE-LINE.                                                      SQ2034.2
038600     ADD 1 TO RECORD-COUNT.                                       SQ2034.2
038700*    IF RECORD-COUNT GREATER 50                                   SQ2034.2
038800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          SQ2034.2
038900*        MOVE SPACE TO DUMMY-RECORD                               SQ2034.2
039000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  SQ2034.2
039100*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             SQ2034.2
039200*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     SQ2034.2
039300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          SQ2034.2
039400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          SQ2034.2
039500*        MOVE ZERO TO RECORD-COUNT.                               SQ2034.2
039600     PERFORM WRT-LN.                                              SQ2034.2
039700 WRT-LN.                                                          SQ2034.2
039800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               SQ2034.2
039900     MOVE SPACE TO DUMMY-RECORD.                                  SQ2034.2
040000 BLANK-LINE-PRINT.                                                SQ2034.2
040100     PERFORM WRT-LN.                                              SQ2034.2
040200 FAIL-ROUTINE.                                                    SQ2034.2
040300     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   SQ2034.2
040400     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    SQ2034.2
040500     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    SQ2034.2
040600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2034.2
040700     GO TO FAIL-ROUTINE-EX.                                       SQ2034.2
040800 FAIL-ROUTINE-WRITE.                                              SQ2034.2
040900     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           SQ2034.2
041000     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   SQ2034.2
041100 FAIL-ROUTINE-EX. EXIT.                                           SQ2034.2
041200 BAIL-OUT.                                                        SQ2034.2
041300     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       SQ2034.2
041400     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               SQ2034.2
041500 BAIL-OUT-WRITE.                                                  SQ2034.2
041600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  SQ2034.2
041700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     SQ2034.2
041800 BAIL-OUT-EX. EXIT.                                               SQ2034.2
041900 CCVS1-EXIT.                                                      SQ2034.2
042000     EXIT.                                                        SQ2034.2
042100 SECT-SQ203A-0001 SECTION.                                        SQ2034.2
042200 READ-INIT-GF-01.                                                 SQ2034.2
042300*             THIS IS A TEST FOR SELECT OPTIONAL SQ-FS1. IN       SQ2034.2
042400*             THIS TEST THE FILE IS PRESENT THEREFORE IT SHOULD   SQ2034.2
042500*             OPEN AND HAVE THE FIRST RECORD READ CORRECTLY       SQ2034.2
042600*             WITHOUT TRANSFERING CONTROL TO THE AT END CONDITION.SQ2034.2
042700     MOVE "READ-TEST-GF-01" TO PAR-NAME.                          SQ2034.2
042800     MOVE "SELECT OPTIONAL F-N" TO FEATURE.                       SQ2034.2
042900     MOVE "FILE PRESENT" TO RE-MARK.                              SQ2034.2
043000 READ-TEST-GF-01.                                                 SQ2034.2
043100     OPEN INPUT SQ-FS1.                                           SQ2034.2
043200     READ SQ-FS1 ; AT END GO TO READ-FAIL-GF-01.                  SQ2034.2
043300     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        SQ2034.2
043400     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      SQ2034.2
043500              MOVE "INVALID XFILE-NAME" TO RE-MARK                SQ2034.2
043600              GO TO READ-FAIL-GF-01.                              SQ2034.2
043700     IF XRECORD-NUMBER (1) NOT EQUAL TO 1                         SQ2034.2
043800              MOVE "INVALID RECORD NUMBER" TO RE-MARK             SQ2034.2
043900              GO TO READ-FAIL-GF-01.                              SQ2034.2
044000     GO TO READ-PASS-GF-01.                                       SQ2034.2
044100 READ-DELETE-GF-01.                                               SQ2034.2
044200     PERFORM DE-LETE.                                             SQ2034.2
044300     GO TO READ-WRITE-GF-01.                                      SQ2034.2
044400 READ-FAIL-GF-01.                                                 SQ2034.2
044500     MOVE "VII-7 2.3.2; VII-8 2.3.4 (2); GR (4) B, (10)"          SQ2034.2
044600        TO RE-MARK.                                               SQ2034.2
044700     PERFORM FAIL.                                                SQ2034.2
044800     CLOSE SQ-FS1.                                                SQ2034.2
044900     GO TO READ-WRITE-GF-01.                                      SQ2034.2
045000 READ-PASS-GF-01.                                                 SQ2034.2
045100     PERFORM PASS.                                                SQ2034.2
045200     CLOSE SQ-FS1.                                                SQ2034.2
045300 READ-WRITE-GF-01.                                                SQ2034.2
045400     PERFORM PRINT-DETAIL.                                        SQ2034.2
045500 READ-INIT-GF-02.                                                 SQ2034.2
045600*             THIS IS A TEST FOR SELECT OPTIONAL SQ-FS4     IN    SQ2034.2
045700*             WHICH THE FIRST READ STATEMENT HAS AN AT END PHRASE.SQ2034.2
045800*             IN THIS TEST THE SELECTED FILE IS NOT PRESENT.      SQ2034.2
045900     MOVE "READ-TEST-GF-02" TO PAR-NAME.                          SQ2034.2
046000     MOVE "SELECT OPTIONAL F-N" TO FEATURE.                       SQ2034.2
046100     MOVE "FILE NOT PRESENT" TO RE-MARK.                          SQ2034.2
046200 READ-TEST-GF-02.                                                 SQ2034.2
046300     OPEN INPUT SQ-FS4.                                           SQ2034.2
046400     READ SQ-FS4 ; AT END GO TO READ-PASS-GF-02.                  SQ2034.2
046500     GO TO READ-FAIL-GF-02.                                       SQ2034.2
046600 READ-DELETE-GF-02.                                               SQ2034.2
046700     PERFORM DE-LETE.                                             SQ2034.2
046800     GO TO READ-WRITE-GF-02.                                      SQ2034.2
046900 READ-FAIL-GF-02.                                                 SQ2034.2
047000     MOVE "VII-7 2.3.2; VII-8 2.3.4 (2); GR (4) B, (10)"          SQ2034.2
047100        TO RE-MARK.                                               SQ2034.2
047200     PERFORM FAIL.                                                SQ2034.2
047300     CLOSE SQ-FS4.                                                SQ2034.2
047400     GO TO READ-WRITE-GF-02.                                      SQ2034.2
047500 READ-PASS-GF-02.                                                 SQ2034.2
047600     PERFORM PASS.                                                SQ2034.2
047700     CLOSE SQ-FS4.                                                SQ2034.2
047800 READ-WRITE-GF-02.                                                SQ2034.2
047900     PERFORM PRINT-DETAIL.                                        SQ2034.2
048000 READ-INIT-GF-03.                                                 SQ2034.2
048100*             THIS IS A TEST FOR SELECT OPTIONAL SQ-FS2     IN    SQ2034.2
048200*             WHICH THE FIRST READ STATEMENT DOES NOT HAVE AN AT  SQ2034.2
048300*             END PHRASE.  INSTEAD A USE STATEMENT IS SPECIFIED.  SQ2034.2
048400*             IN THIS TEST THE SELECTED FILE IS NOT PRESENT.      SQ2034.2
048500     MOVE "READ-TEST-GF-03" TO PAR-NAME.                          SQ2034.2
048600     MOVE "SELECT OPTIONAL F-N" TO FEATURE.                       SQ2034.2
048700     MOVE "FILE NOT PRESENT" TO RE-MARK.                          SQ2034.2
048800 READ-TEST-GF-03.                                                 SQ2034.2
048900     OPEN INPUT SQ-FS2.                                           SQ2034.2
049000     READ SQ-FS2.                                                 SQ2034.2
049100     IF EOF-FLAG EQUAL TO 1                                       SQ2034.2
049200              GO TO READ-PASS-GF-03.                              SQ2034.2
049300     GO TO READ-FAIL-GF-03.                                       SQ2034.2
049400 READ-DELETE-GF-03.                                               SQ2034.2
049500     PERFORM DE-LETE.                                             SQ2034.2
049600     GO TO READ-WRITE-GF-03.                                      SQ2034.2
049700 READ-FAIL-GF-03.                                                 SQ2034.2
049800     MOVE "VII-7 2.3.2; VII-8 2.3.4 (2); GR (4) B, (10)"          SQ2034.2
049900        TO RE-MARK.                                               SQ2034.2
050000     PERFORM FAIL.                                                SQ2034.2
050100     MOVE WRK-XN-00001-KEY-2 TO COMPUTED-A.                       SQ2034.2
050200     MOVE "1" TO CORRECT-A.                                       SQ2034.2
050300     CLOSE SQ-FS2.                                                SQ2034.2
050400     GO TO READ-WRITE-GF-03.                                      SQ2034.2
050500 READ-PASS-GF-03.                                                 SQ2034.2
050600     PERFORM PASS.                                                SQ2034.2
050700     MOVE WRK-XN-00001-KEY-2 TO COMPUTED-A.                       SQ2034.2
050800     MOVE "1" TO CORRECT-A.                                       SQ2034.2
050900     CLOSE SQ-FS2.                                                SQ2034.2
051000 READ-WRITE-GF-03.                                                SQ2034.2
051100     PERFORM PRINT-DETAIL.                                        SQ2034.2
051200 READ-INIT-GF-04.                                                 SQ2034.2
051300*             THIS TEST IS USED TO CHECK OUT THE RESERVE INTEGER  SQ2034.2
051400*             AREA CLAUSE IN THE FILE-CONTROL ENTRY.              SQ2034.2
051500     MOVE "READ-TEST-GF-04" TO PAR-NAME.                          SQ2034.2
051600     MOVE "RESERVE INTEGER AREA" TO FEATURE.                      SQ2034.2
051700     MOVE "SQ-FS3" TO XFILE-NAME (1).                             SQ2034.2
051800     MOVE "R1-F-G" TO XRECORD-NAME (1).                           SQ2034.2
051900     MOVE "SQ203A" TO XPROGRAM-NAME (1).                          SQ2034.2
052000     MOVE 000120  TO XRECORD-LENGTH (1).                          SQ2034.2
052100     MOVE "RC" TO CHARS-OR-RECORDS (1).                           SQ2034.2
052200     MOVE 0001    TO XBLOCK-SIZE (1).                             SQ2034.2
052300     MOVE 000750  TO RECORDS-IN-FILE (1).                         SQ2034.2
052400     MOVE "SQ"    TO XFILE-ORGANIZATION (1).                      SQ2034.2
052500     MOVE "S"     TO XLABEL-TYPE (1).                             SQ2034.2
052600     MOVE 000001  TO XRECORD-NUMBER (1).                          SQ2034.2
052700 READ-TEST-GF-04-01.                                              SQ2034.2
052800     OPEN OUTPUT SQ-FS3.                                          SQ2034.2
052900 READ-TEST-GF-04-02.                                              SQ2034.2
053000     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS3R1-F-G-120.        SQ2034.2
053100     WRITE SQ-FS3R1-F-G-120.                                      SQ2034.2
053200     IF XRECORD-NUMBER (1) EQUAL TO 750                           SQ2034.2
053300              GO TO READ-TEST-GF-04-03.                           SQ2034.2
053400     ADD 1 TO XRECORD-NUMBER (1).                                 SQ2034.2
053500     GO TO READ-TEST-GF-04-02.                                    SQ2034.2
053600 READ-TEST-GF-04-03.                                              SQ2034.2
053700     CLOSE SQ-FS3.                                                SQ2034.2
053800     PERFORM PASS.                                                SQ2034.2
053900     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   SQ2034.2
054000     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     SQ2034.2
054100     GO TO READ-WRITE-GF-04.                                      SQ2034.2
054200 READ-DELETE-GF-04.                                               SQ2034.2
054300     PERFORM DE-LETE.                                             SQ2034.2
054400 READ-WRITE-GF-04.                                                SQ2034.2
054500     PERFORM PRINT-DETAIL.                                        SQ2034.2
054600 SQ203A-END-ROUTINE.                                              SQ2034.2
054700     MOVE "END OF SQ203A VALIDATION TESTS" TO PRINT-REC.          SQ2034.2
054800     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      SQ2034.2
054900 TERMINATE-SQ203A.                                                SQ2034.2
055000     EXIT.                                                        SQ2034.2
055100 CCVS-EXIT SECTION.                                               SQ2034.2
055200 CCVS-999999.                                                     SQ2034.2
055300     GO TO CLOSE-FILES.                                           SQ2034.2
      *END-OF,SQ203A                                                                  
