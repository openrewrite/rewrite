      *HEADER,COBOL,OBSQ3A,SUBPRG,OBSQ5A                                              
000100 IDENTIFICATION DIVISION.                                         OBSQ54.2
000200 PROGRAM-ID.                                                      OBSQ54.2
000300     OBSQ5A.                                                      OBSQ54.2
000400****************************************************************  OBSQ54.2
000500*                                                              *  OBSQ54.2
000600*    VALIDATION FOR:-                                          *  OBSQ54.2
000700*    " HIGH       ".                                              OBSQ54.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  OBSQ54.2
000900*                                                              *  OBSQ54.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  OBSQ54.2
001100*    "4.2 ".                                                      OBSQ54.2
001200*                                                              *  OBSQ54.2
001300*         THE ROUTINE OBSQ5A TESTS THE USE OF THE MULTIPLE FILE   OBSQ54.2
001400*    CLAUSE BY READING AND VALIDATING THE TWO MULTIPLE FILE TAPES OBSQ54.2
001500*    CREATED IN OBSQ3A. TAPE ONE IS PROCESSED USING THE MULTIPLE  OBSQ54.2
001600*    FILE CLAUSE WITH POSITION PHRASE.  ONLY FILE SQ-FS3 IS       OBSQ54.2
001700*    SPECIFIED AND PROCESSED FROM THIS TAPE.  TAPE TWO IS         OBSQ54.2
001800*    PROCESSED USING THE MULTIPLE FILE CLAUSE WITHOUT THE         OBSQ54.2
001900*    POSITION PHRASE.  ALL FOUR FILES ON THIS TAPE ARE PROCESSED. OBSQ54.2
002000*    THESE FILES WERE CREATED USING A MULTIPLE FILE CLAUSE WITH   OBSQ54.2
002100*    POSITION PHRASE.                                             OBSQ54.2
002200 ENVIRONMENT DIVISION.                                            OBSQ54.2
002300 CONFIGURATION SECTION.                                           OBSQ54.2
002400 SOURCE-COMPUTER.                                                 OBSQ54.2
002500     XXXXX082.                                                    OBSQ54.2
002600 OBJECT-COMPUTER.                                                 OBSQ54.2
002700     XXXXX083.                                                    OBSQ54.2
002800 INPUT-OUTPUT SECTION.                                            OBSQ54.2
002900 FILE-CONTROL.                                                    OBSQ54.2
003000*    SELECT RAW-DATA   ASSIGN TO                                  OBSQ54.2
003100*    XXXXX062                                                     OBSQ54.2
003200*           ORGANIZATION IS INDEXED                               OBSQ54.2
003300*           ACCESS MODE IS RANDOM                                 OBSQ54.2
003400*           RECORD KEY IS RAW-DATA-KEY.                           OBSQ54.2
003500     SELECT PRINT-FILE ASSIGN TO                                  OBSQ54.2
003600     XXXXX055.                                                    OBSQ54.2
003700     SELECT SQ-FS3 ASSIGN TO                                      OBSQ54.2
003800     XXXXD009.                                                    OBSQ54.2
003900     SELECT SQ-FS5 ASSIGN TO                                      OBSQ54.2
004000     XXXXD005.                                                    OBSQ54.2
004100     SELECT SQ-FS6 ASSIGN TO                                      OBSQ54.2
004200     XXXXD011.                                                    OBSQ54.2
004300     SELECT SQ-FS7 ASSIGN TO                                      OBSQ54.2
004400     XXXXD012.                                                    OBSQ54.2
004500     SELECT SQ-FS8 ASSIGN TO                                      OBSQ54.2
004600     XXXXD013.                                                    OBSQ54.2
004700 I-O-CONTROL.                                                     OBSQ54.2
004800     MULTIPLE FILE TAPE CONTAINS SQ-FS3 POSITION 3;               OBSQ54.2
004900     MULTIPLE FILE TAPE SQ-FS5,                                   OBSQ54.2
005000                        SQ-FS6,                                   OBSQ54.2
005100                        SQ-FS7,                                   OBSQ54.2
005200                        SQ-FS8.                                   OBSQ54.2
005300 DATA DIVISION.                                                   OBSQ54.2
005400 FILE SECTION.                                                    OBSQ54.2
005500*                                                                 OBSQ54.2
005600*FD  RAW-DATA.                                                    OBSQ54.2
005700*                                                                 OBSQ54.2
005800*01  RAW-DATA-SATZ.                                               OBSQ54.2
005900*    05  RAW-DATA-KEY        PIC X(6).                            OBSQ54.2
006000*    05  C-DATE              PIC 9(6).                            OBSQ54.2
006100*    05  C-TIME              PIC 9(8).                            OBSQ54.2
006200*    05  C-NO-OF-TESTS       PIC 99.                              OBSQ54.2
006300*    05  C-OK                PIC 999.                             OBSQ54.2
006400*    05  C-ALL               PIC 999.                             OBSQ54.2
006500*    05  C-FAIL              PIC 999.                             OBSQ54.2
006600*    05  C-DELETED           PIC 999.                             OBSQ54.2
006700*    05  C-INSPECT           PIC 999.                             OBSQ54.2
006800*    05  C-NOTE              PIC X(13).                           OBSQ54.2
006900*    05  C-INDENT            PIC X.                               OBSQ54.2
007000*    05  C-ABORT             PIC X(8).                            OBSQ54.2
007100 FD  PRINT-FILE.                                                  OBSQ54.2
007200 01  PRINT-REC PICTURE X(120).                                    OBSQ54.2
007300 01  DUMMY-RECORD PICTURE X(120).                                 OBSQ54.2
007400 FD  SQ-FS3                                                       OBSQ54.2
007500     LABEL RECORD IS STANDARD                                     OBSQ54.2
007600     RECORD CONTAINS 120 CHARACTERS                               OBSQ54.2
007700     BLOCK CONTAINS 1200 CHARACTERS.                              OBSQ54.2
007800 01  SQ-FS3R1-F-G-120   PIC X(120).                               OBSQ54.2
007900 FD  SQ-FS5                                                       OBSQ54.2
008000     LABEL RECORD STANDARD                                        OBSQ54.2
008100     BLOCK CONTAINS 5 RECORDS.                                    OBSQ54.2
008200 01  SQ-FS5R1-F-G-120   PIC X(120).                               OBSQ54.2
008300 FD  SQ-FS6                                                       OBSQ54.2
008400     LABEL RECORD STANDARD                                        OBSQ54.2
008500     BLOCK CONTAINS 10 RECORDS.                                   OBSQ54.2
008600 01  SQ-FS6R1-F-G-120   PIC X(120).                               OBSQ54.2
008700 FD  SQ-FS7                                                       OBSQ54.2
008800     LABEL RECORD STANDARD                                        OBSQ54.2
008900     BLOCK CONTAINS 2400 CHARACTERS.                              OBSQ54.2
009000 01  SQ-FS7R1-F-G-120   PIC X(120).                               OBSQ54.2
009100 FD  SQ-FS8                                                       OBSQ54.2
009200     LABEL RECORD STANDARD                                        OBSQ54.2
009300     RECORD 120                                                   OBSQ54.2
009400     BLOCK CONTAINS 120 CHARACTERS.                               OBSQ54.2
009500 01  SQ-FS8R1-F-G-120   PIC X(120).                               OBSQ54.2
009600 WORKING-STORAGE SECTION.                                         OBSQ54.2
009700 77  COUNT-OF-RECS  PICTURE 999 VALUE 0.                          OBSQ54.2
009800 77  RECORDS-IN-ERROR   PIC 999   VALUE 0.                        OBSQ54.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 OBSQ54.2
010000     03 FILE-RECORD-INFO-SKELETON.                                OBSQ54.2
010100        05 FILLER                 PICTURE X(48)       VALUE       OBSQ54.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  OBSQ54.2
010300        05 FILLER                 PICTURE X(46)       VALUE       OBSQ54.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    OBSQ54.2
010500        05 FILLER                 PICTURE X(26)       VALUE       OBSQ54.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        OBSQ54.2
010700        05 FILLER                 PICTURE X(37)       VALUE       OBSQ54.2
010800             ",RECKEY=                             ".             OBSQ54.2
010900        05 FILLER                 PICTURE X(38)       VALUE       OBSQ54.2
011000             ",ALTKEY1=                             ".            OBSQ54.2
011100        05 FILLER                 PICTURE X(38)       VALUE       OBSQ54.2
011200             ",ALTKEY2=                             ".            OBSQ54.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.OBSQ54.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              OBSQ54.2
011500        05 FILE-RECORD-INFO-P1-120.                               OBSQ54.2
011600           07 FILLER              PIC X(5).                       OBSQ54.2
011700           07 XFILE-NAME           PIC X(6).                      OBSQ54.2
011800           07 FILLER              PIC X(8).                       OBSQ54.2
011900           07 XRECORD-NAME         PIC X(6).                      OBSQ54.2
012000           07 FILLER              PIC X(1).                       OBSQ54.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       OBSQ54.2
012200           07 FILLER              PIC X(7).                       OBSQ54.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      OBSQ54.2
012400           07 FILLER              PIC X(6).                       OBSQ54.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       OBSQ54.2
012600           07 FILLER              PIC X(5).                       OBSQ54.2
012700           07 ODO-NUMBER          PIC 9(4).                       OBSQ54.2
012800           07 FILLER              PIC X(5).                       OBSQ54.2
012900           07 XPROGRAM-NAME        PIC X(5).                      OBSQ54.2
013000           07 FILLER              PIC X(7).                       OBSQ54.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      OBSQ54.2
013200           07 FILLER              PIC X(7).                       OBSQ54.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       OBSQ54.2
013400           07 FILLER              PIC X(1).                       OBSQ54.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      OBSQ54.2
013600           07 FILLER              PIC X(6).                       OBSQ54.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       OBSQ54.2
013800           07 FILLER              PIC X(5).                       OBSQ54.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      OBSQ54.2
014000           07 FILLER              PIC X(6).                       OBSQ54.2
014100           07 XLABEL-TYPE          PIC X(1).                      OBSQ54.2
014200        05 FILE-RECORD-INFO-P121-240.                             OBSQ54.2
014300           07 FILLER              PIC X(8).                       OBSQ54.2
014400           07 XRECORD-KEY          PIC X(29).                     OBSQ54.2
014500           07 FILLER              PIC X(9).                       OBSQ54.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      OBSQ54.2
014700           07 FILLER              PIC X(9).                       OBSQ54.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      OBSQ54.2
014900           07 FILLER              PIC X(7).                       OBSQ54.2
015000 01  TEST-RESULTS.                                                OBSQ54.2
015100     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ54.2
015200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      OBSQ54.2
015300     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ54.2
015400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       OBSQ54.2
015500     02 FILLER                    PICTURE X  VALUE SPACE.         OBSQ54.2
015600     02  PAR-NAME.                                                OBSQ54.2
015700       03 FILLER PICTURE X(12) VALUE SPACE.                       OBSQ54.2
015800       03  PARDOT-X PICTURE X  VALUE SPACE.                       OBSQ54.2
015900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        OBSQ54.2
016000       03 FILLER PIC X(5) VALUE SPACE.                            OBSQ54.2
016100     02 FILLER PIC X(10) VALUE SPACE.                             OBSQ54.2
016200     02 RE-MARK PIC X(61).                                        OBSQ54.2
016300 01  TEST-COMPUTED.                                               OBSQ54.2
016400     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ54.2
016500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                OBSQ54.2
016600     02 COMPUTED-X.                                               OBSQ54.2
016700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      OBSQ54.2
016800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       OBSQ54.2
016900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      OBSQ54.2
017000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  OBSQ54.2
017100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  OBSQ54.2
017200     03       CM-18V0 REDEFINES COMPUTED-A.                       OBSQ54.2
017300         04 COMPUTED-18V0                   PICTURE -9(18).       OBSQ54.2
017400         04 FILLER                          PICTURE X.            OBSQ54.2
017500     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ54.2
017600 01  TEST-CORRECT.                                                OBSQ54.2
017700     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ54.2
017800     02 FILLER PIC X(17) VALUE "       CORRECT =".                OBSQ54.2
017900     02 CORRECT-X.                                                OBSQ54.2
018000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      OBSQ54.2
018100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         OBSQ54.2
018200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      OBSQ54.2
018300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  OBSQ54.2
018400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  OBSQ54.2
018500     03      CR-18V0 REDEFINES CORRECT-A.                         OBSQ54.2
018600         04 CORRECT-18V0                    PICTURE -9(18).       OBSQ54.2
018700         04 FILLER                          PICTURE X.            OBSQ54.2
018800     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ54.2
018900 01  CCVS-C-1.                                                    OBSQ54.2
019000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PAOBSQ54.2
019100-    "SS  PARAGRAPH-NAME                                          OBSQ54.2
019200-    "        REMARKS".                                           OBSQ54.2
019300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   OBSQ54.2
019400 01  CCVS-C-2.                                                    OBSQ54.2
019500     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ54.2
019600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 OBSQ54.2
019700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   OBSQ54.2
019800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   OBSQ54.2
019900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   OBSQ54.2
020000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         OBSQ54.2
020100 01  REC-CT PICTURE 99 VALUE ZERO.                                OBSQ54.2
020200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        OBSQ54.2
020300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  OBSQ54.2
020400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          OBSQ54.2
020500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             OBSQ54.2
020600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              OBSQ54.2
020700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               OBSQ54.2
020800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           OBSQ54.2
020900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            OBSQ54.2
021000 01  REC-COUNT    PIC 9(5) VALUE ZERO.                            OBSQ54.2
021100 01  CCVS-H-1.                                                    OBSQ54.2
021200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     OBSQ54.2
021300     02 FILLER PICTURE X(67) VALUE                                OBSQ54.2
021400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  OBSQ54.2
021500-    " SYSTEM".                                                   OBSQ54.2
021600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   OBSQ54.2
021700 01  CCVS-H-2.                                                    OBSQ54.2
021800     02 FILLER PICTURE X(52) VALUE IS                             OBSQ54.2
021900     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   OBSQ54.2
022000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   OBSQ54.2
022100     02 TEST-ID PICTURE IS X(9).                                  OBSQ54.2
022200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   OBSQ54.2
022300 01  CCVS-H-3.                                                    OBSQ54.2
022400     02  FILLER PICTURE X(34) VALUE                               OBSQ54.2
022500     " FOR OFFICIAL USE ONLY    ".                                OBSQ54.2
022600     02  FILLER PICTURE X(58) VALUE                               OBSQ54.2
022700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBSQ54.2
022800     02  FILLER PICTURE X(28) VALUE                               OBSQ54.2
022900     "  COPYRIGHT   1985 ".                                       OBSQ54.2
023000 01  CCVS-E-1.                                                    OBSQ54.2
023100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   OBSQ54.2
023200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        OBSQ54.2
023300     02 ID-AGAIN PICTURE IS X(9).                                 OBSQ54.2
023400     02 FILLER PICTURE X(45) VALUE IS                             OBSQ54.2
023500     " NTIS DISTRIBUTION COBOL 85".                               OBSQ54.2
023600 01  CCVS-E-2.                                                    OBSQ54.2
023700     02  FILLER                   PICTURE X(31)  VALUE            OBSQ54.2
023800     SPACE.                                                       OBSQ54.2
023900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     OBSQ54.2
024000     02 CCVS-E-2-2.                                               OBSQ54.2
024100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            OBSQ54.2
024200         03 FILLER PICTURE IS X VALUE IS SPACE.                   OBSQ54.2
024300         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      OBSQ54.2
024400 01  CCVS-E-3.                                                    OBSQ54.2
024500     02  FILLER PICTURE X(22) VALUE                               OBSQ54.2
024600     " FOR OFFICIAL USE ONLY".                                    OBSQ54.2
024700     02  FILLER PICTURE X(12) VALUE SPACE.                        OBSQ54.2
024800     02  FILLER PICTURE X(58) VALUE                               OBSQ54.2
024900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBSQ54.2
025000     02  FILLER PICTURE X(13) VALUE SPACE.                        OBSQ54.2
025100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 OBSQ54.2
025200 01  CCVS-E-4.                                                    OBSQ54.2
025300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           OBSQ54.2
025400     02 FILLER PIC XXXX VALUE " OF ".                             OBSQ54.2
025500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           OBSQ54.2
025600     02 FILLER PIC X(40) VALUE                                    OBSQ54.2
025700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       OBSQ54.2
025800 01  XXINFO.                                                      OBSQ54.2
025900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    OBSQ54.2
026000     02 INFO-TEXT.                                                OBSQ54.2
026100     04 FILLER PIC X(20) VALUE SPACE.                             OBSQ54.2
026200     04 XXCOMPUTED PIC X(20).                                     OBSQ54.2
026300     04 FILLER PIC X(5) VALUE SPACE.                              OBSQ54.2
026400     04 XXCORRECT PIC X(20).                                      OBSQ54.2
026500 01  HYPHEN-LINE.                                                 OBSQ54.2
026600     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ54.2
026700     02 FILLER PICTURE IS X(65) VALUE IS "************************OBSQ54.2
026800-    "*****************************************".                 OBSQ54.2
026900     02 FILLER PICTURE IS X(54) VALUE IS "************************OBSQ54.2
027000-    "******************************".                            OBSQ54.2
027100 01  CCVS-PGM-ID PIC X(6) VALUE                                   OBSQ54.2
027200     "OBSQ5A".                                                    OBSQ54.2
027300 PROCEDURE DIVISION.                                              OBSQ54.2
027400 CCVS1 SECTION.                                                   OBSQ54.2
027500 OPEN-FILES.                                                      OBSQ54.2
027600*    OPEN I-O RAW-DATA.                                           OBSQ54.2
027700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ54.2
027800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     OBSQ54.2
027900*    MOVE "ABORTED " TO C-ABORT.                                  OBSQ54.2
028000*    ADD 1 TO C-NO-OF-TESTS.                                      OBSQ54.2
028100*    ACCEPT C-DATE  FROM DATE.                                    OBSQ54.2
028200*    ACCEPT C-TIME  FROM TIME.                                    OBSQ54.2
028300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             OBSQ54.2
028400*END-E-1.                                                         OBSQ54.2
028500*    CLOSE RAW-DATA.                                              OBSQ54.2
028600     OPEN     OUTPUT PRINT-FILE.                                  OBSQ54.2
028700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   OBSQ54.2
028800     MOVE    SPACE TO TEST-RESULTS.                               OBSQ54.2
028900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             OBSQ54.2
029000     MOVE ZERO TO REC-SKL-SUB.                                    OBSQ54.2
029100     PERFORM CCVS-INIT-FILE 9 TIMES.                              OBSQ54.2
029200 CCVS-INIT-FILE.                                                  OBSQ54.2
029300     ADD 1 TO REC-SKL-SUB.                                        OBSQ54.2
029400     MOVE FILE-RECORD-INFO-SKELETON TO                            OBSQ54.2
029500                  FILE-RECORD-INFO (REC-SKL-SUB).                 OBSQ54.2
029600 CCVS-INIT-EXIT.                                                  OBSQ54.2
029700     GO TO CCVS1-EXIT.                                            OBSQ54.2
029800 CLOSE-FILES.                                                     OBSQ54.2
029900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   OBSQ54.2
030000*    OPEN I-O RAW-DATA.                                           OBSQ54.2
030100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ54.2
030200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     OBSQ54.2
030300*    MOVE "OK.     " TO C-ABORT.                                  OBSQ54.2
030400*    MOVE PASS-COUNTER TO C-OK.                                   OBSQ54.2
030500*    MOVE ERROR-HOLD   TO C-ALL.                                  OBSQ54.2
030600*    MOVE ERROR-COUNTER TO C-FAIL.                                OBSQ54.2
030700*    MOVE DELETE-CNT TO C-DELETED.                                OBSQ54.2
030800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           OBSQ54.2
030900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             OBSQ54.2
031000*END-E-2.                                                         OBSQ54.2
031100*    CLOSE RAW-DATA.                                              OBSQ54.2
031200 TERMINATE-CCVS.                                                  OBSQ54.2
031300*    EXIT PROGRAM.                                                OBSQ54.2
031400*TERMINATE-CALL.                                                  OBSQ54.2
031500     STOP     RUN.                                                OBSQ54.2
031600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         OBSQ54.2
031700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           OBSQ54.2
031800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          OBSQ54.2
031900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          OBSQ54.2
032000     MOVE "****TEST DELETED****" TO RE-MARK.                      OBSQ54.2
032100 PRINT-DETAIL.                                                    OBSQ54.2
032200     IF REC-CT NOT EQUAL TO ZERO                                  OBSQ54.2
032300             MOVE "." TO PARDOT-X                                 OBSQ54.2
032400             MOVE REC-CT TO DOTVALUE.                             OBSQ54.2
032500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      OBSQ54.2
032600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               OBSQ54.2
032700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 OBSQ54.2
032800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 OBSQ54.2
032900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              OBSQ54.2
033000     MOVE SPACE TO CORRECT-X.                                     OBSQ54.2
033100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         OBSQ54.2
033200     MOVE     SPACE TO RE-MARK.                                   OBSQ54.2
033300 HEAD-ROUTINE.                                                    OBSQ54.2
033400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ54.2
033500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   OBSQ54.2
033600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   OBSQ54.2
033700 COLUMN-NAMES-ROUTINE.                                            OBSQ54.2
033800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ54.2
033900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ54.2
034000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        OBSQ54.2
034100 END-ROUTINE.                                                     OBSQ54.2
034200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.OBSQ54.2
034300 END-RTN-EXIT.                                                    OBSQ54.2
034400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ54.2
034500 END-ROUTINE-1.                                                   OBSQ54.2
034600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      OBSQ54.2
034700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   OBSQ54.2
034800      ADD PASS-COUNTER TO ERROR-HOLD.                             OBSQ54.2
034900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   OBSQ54.2
035000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            OBSQ54.2
035100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              OBSQ54.2
035200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                OBSQ54.2
035300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           OBSQ54.2
035400  END-ROUTINE-12.                                                 OBSQ54.2
035500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        OBSQ54.2
035600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      OBSQ54.2
035700         MOVE "NO " TO ERROR-TOTAL                                OBSQ54.2
035800         ELSE                                                     OBSQ54.2
035900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       OBSQ54.2
036000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           OBSQ54.2
036100     PERFORM WRITE-LINE.                                          OBSQ54.2
036200 END-ROUTINE-13.                                                  OBSQ54.2
036300     IF DELETE-CNT IS EQUAL TO ZERO                               OBSQ54.2
036400         MOVE "NO " TO ERROR-TOTAL  ELSE                          OBSQ54.2
036500         MOVE DELETE-CNT TO ERROR-TOTAL.                          OBSQ54.2
036600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   OBSQ54.2
036700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ54.2
036800      IF   INSPECT-COUNTER EQUAL TO ZERO                          OBSQ54.2
036900          MOVE "NO " TO ERROR-TOTAL                               OBSQ54.2
037000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   OBSQ54.2
037100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            OBSQ54.2
037200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          OBSQ54.2
037300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ54.2
037400 WRITE-LINE.                                                      OBSQ54.2
037500     ADD 1 TO RECORD-COUNT.                                       OBSQ54.2
037600*    IF RECORD-COUNT GREATER 50                                   OBSQ54.2
037700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          OBSQ54.2
037800*        MOVE SPACE TO DUMMY-RECORD                               OBSQ54.2
037900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  OBSQ54.2
038000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             OBSQ54.2
038100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     OBSQ54.2
038200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          OBSQ54.2
038300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          OBSQ54.2
038400*        MOVE ZERO TO RECORD-COUNT.                               OBSQ54.2
038500     PERFORM WRT-LN.                                              OBSQ54.2
038600 WRT-LN.                                                          OBSQ54.2
038700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               OBSQ54.2
038800     MOVE SPACE TO DUMMY-RECORD.                                  OBSQ54.2
038900 BLANK-LINE-PRINT.                                                OBSQ54.2
039000     PERFORM WRT-LN.                                              OBSQ54.2
039100 FAIL-ROUTINE.                                                    OBSQ54.2
039200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   OBSQ54.2
039300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    OBSQ54.2
039400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    OBSQ54.2
039500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ54.2
039600     GO TO FAIL-ROUTINE-EX.                                       OBSQ54.2
039700 FAIL-ROUTINE-WRITE.                                              OBSQ54.2
039800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           OBSQ54.2
039900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   OBSQ54.2
040000 FAIL-ROUTINE-EX. EXIT.                                           OBSQ54.2
040100 BAIL-OUT.                                                        OBSQ54.2
040200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       OBSQ54.2
040300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               OBSQ54.2
040400 BAIL-OUT-WRITE.                                                  OBSQ54.2
040500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  OBSQ54.2
040600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ54.2
040700 BAIL-OUT-EX. EXIT.                                               OBSQ54.2
040800 CCVS1-EXIT.                                                      OBSQ54.2
040900     EXIT.                                                        OBSQ54.2
041000 SECT-OBSQ5A-0001 SECTION.                                        OBSQ54.2
041100 SEQ-INIT-001.                                                    OBSQ54.2
041200     MOVE 0 TO REC-COUNT,    RECORDS-IN-ERROR.                    OBSQ54.2
041300     OPEN INPUT SQ-FS3.                                           OBSQ54.2
041400 SEQ-TEST-001.                                                    OBSQ54.2
041500     READ SQ-FS3 AT END GO TO SEQ-TEST-001-01.                    OBSQ54.2
041600     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ54.2
041700     ADD 1 TO REC-COUNT.                                          OBSQ54.2
041800     IF REC-COUNT    GREATER THAN 750                             OBSQ54.2
041900              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ54.2
042000              GO TO SEQ-FAIL-001.                                 OBSQ54.2
042100     IF REC-COUNT    NOT EQUAL TO XRECORD-NUMBER (1)              OBSQ54.2
042200              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
042300              GO TO SEQ-TEST-001.                                 OBSQ54.2
042400     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS3"                      OBSQ54.2
042500              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
042600              GO TO SEQ-TEST-001.                                 OBSQ54.2
042700     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "CH"                    OBSQ54.2
042800              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
042900              GO TO SEQ-TEST-001.                                 OBSQ54.2
043000     IF XBLOCK-SIZE (1) NOT EQUAL TO 1200                         OBSQ54.2
043100              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ54.2
043200     GO TO SEQ-TEST-001.                                          OBSQ54.2
043300 SEQ-TEST-001-01.                                                 OBSQ54.2
043400     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ54.2
043500              GO TO SEQ-PASS-001.                                 OBSQ54.2
043600     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  OBSQ54.2
043700 SEQ-FAIL-001.                                                    OBSQ54.2
043800     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ54.2
043900     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ54.2
044000     PERFORM FAIL.                                                OBSQ54.2
044100     GO TO SEQ-WRITE-001.                                         OBSQ54.2
044200 SEQ-PASS-001.                                                    OBSQ54.2
044300     PERFORM PASS.                                                OBSQ54.2
044400     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ54.2
044500     MOVE REC-COUNT    TO CORRECT-18V0.                           OBSQ54.2
044600 SEQ-WRITE-001.                                                   OBSQ54.2
044700     MOVE "SEQ-TEST-001" TO PAR-NAME.                             OBSQ54.2
044800     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        OBSQ54.2
044900     PERFORM PRINT-DETAIL.                                        OBSQ54.2
045000 SEQ-CLOSE-001.                                                   OBSQ54.2
045100     CLOSE SQ-FS3.                                                OBSQ54.2
045200 SEQ-INIT-002.                                                    OBSQ54.2
045300*             THIS TEST READS AND VALIDATES FILE SQ-FS5.          OBSQ54.2
045400     MOVE 0 TO REC-COUNT,    RECORDS-IN-ERROR.                    OBSQ54.2
045500     OPEN INPUT SQ-FS5.                                           OBSQ54.2
045600 SEQ-TEST-002.                                                    OBSQ54.2
045700     READ SQ-FS5 AT END GO TO SEQ-TEST-002-01.                    OBSQ54.2
045800     MOVE SQ-FS5R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ54.2
045900     ADD 1 TO REC-COUNT.                                          OBSQ54.2
046000     IF REC-COUNT    GREATER THAN 750                             OBSQ54.2
046100              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ54.2
046200              GO TO SEQ-FAIL-002.                                 OBSQ54.2
046300     IF REC-COUNT    NOT EQUAL TO XRECORD-NUMBER (1)              OBSQ54.2
046400              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
046500              GO TO SEQ-TEST-002.                                 OBSQ54.2
046600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS5"                      OBSQ54.2
046700              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
046800              GO TO SEQ-TEST-002.                                 OBSQ54.2
046900     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "RC"                    OBSQ54.2
047000              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
047100              GO TO SEQ-TEST-002.                                 OBSQ54.2
047200     IF XBLOCK-SIZE (1) NOT EQUAL TO 5                            OBSQ54.2
047300              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ54.2
047400     GO TO SEQ-TEST-002.                                          OBSQ54.2
047500 SEQ-TEST-002-01.                                                 OBSQ54.2
047600     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ54.2
047700              GO TO SEQ-PASS-002.                                 OBSQ54.2
047800     MOVE "ERRORS IN READINGS SQ-FS5" TO RE-MARK.                 OBSQ54.2
047900 SEQ-FAIL-002.                                                    OBSQ54.2
048000     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ54.2
048100     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ54.2
048200     PERFORM FAIL.                                                OBSQ54.2
048300     GO TO SEQ-WRITE-002.                                         OBSQ54.2
048400 SEQ-PASS-002.                                                    OBSQ54.2
048500     PERFORM PASS.                                                OBSQ54.2
048600     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ54.2
048700     MOVE REC-COUNT    TO CORRECT-18V0.                           OBSQ54.2
048800 SEQ-WRITE-002.                                                   OBSQ54.2
048900     MOVE "SEQ-TEST-002" TO PAR-NAME.                             OBSQ54.2
049000     MOVE "VERIFY FILE SQ-FS5" TO FEATURE                         OBSQ54.2
049100     PERFORM PRINT-DETAIL.                                        OBSQ54.2
049200 SEQ-CLOSE-002.                                                   OBSQ54.2
049300     CLOSE SQ-FS5 WITH NO REWIND.                                 OBSQ54.2
049400 SEQ-INIT-003.                                                    OBSQ54.2
049500*             THIS TEST READS AND VALIDATES FILE SQ-FS6.          OBSQ54.2
049600     MOVE 0 TO REC-COUNT,    RECORDS-IN-ERROR.                    OBSQ54.2
049700     OPEN INPUT SQ-FS6 WITH NO REWIND.                            OBSQ54.2
049800 SEQ-TEST-003.                                                    OBSQ54.2
049900     READ SQ-FS6 AT END GO TO SEQ-TEST-003-01.                    OBSQ54.2
050000     MOVE SQ-FS6R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ54.2
050100     ADD 1 TO REC-COUNT.                                          OBSQ54.2
050200     IF REC-COUNT    GREATER THAN 750                             OBSQ54.2
050300              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ54.2
050400              GO TO SEQ-FAIL-003.                                 OBSQ54.2
050500     IF REC-COUNT    NOT EQUAL TO XRECORD-NUMBER (1)              OBSQ54.2
050600              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
050700              GO TO SEQ-TEST-003.                                 OBSQ54.2
050800     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS6"                      OBSQ54.2
050900              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
051000              GO TO SEQ-TEST-003.                                 OBSQ54.2
051100     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "RC"                    OBSQ54.2
051200              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
051300              GO TO SEQ-TEST-003.                                 OBSQ54.2
051400     IF XBLOCK-SIZE (1) NOT EQUAL TO 10                           OBSQ54.2
051500              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ54.2
051600     GO TO SEQ-TEST-003.                                          OBSQ54.2
051700 SEQ-TEST-003-01.                                                 OBSQ54.2
051800     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ54.2
051900              GO TO SEQ-PASS-003.                                 OBSQ54.2
052000     MOVE "ERRORS IN READING SQ-FS6" TO RE-MARK.                  OBSQ54.2
052100 SEQ-FAIL-003.                                                    OBSQ54.2
052200     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ54.2
052300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ54.2
052400     PERFORM FAIL.                                                OBSQ54.2
052500     GO TO SEQ-WRITE-003.                                         OBSQ54.2
052600 SEQ-PASS-003.                                                    OBSQ54.2
052700     PERFORM PASS.                                                OBSQ54.2
052800     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ54.2
052900     MOVE REC-COUNT    TO CORRECT-18V0.                           OBSQ54.2
053000 SEQ-WRITE-003.                                                   OBSQ54.2
053100     MOVE "SEQ-TEST-003" TO PAR-NAME.                             OBSQ54.2
053200     MOVE "VERIFY FILE SQ-FS6" TO FEATURE.                        OBSQ54.2
053300     PERFORM PRINT-DETAIL.                                        OBSQ54.2
053400 SEQ-CLOSE-003.                                                   OBSQ54.2
053500     CLOSE SQ-FS6 WITH NO REWIND.                                 OBSQ54.2
053600 SEQ-INIT-004.                                                    OBSQ54.2
053700*             THIS TEST READS AND VALIDATES FILE SQ-FS7.          OBSQ54.2
053800     MOVE 0 TO REC-COUNT,    RECORDS-IN-ERROR.                    OBSQ54.2
053900     OPEN INPUT SQ-FS7 WITH NO REWIND.                            OBSQ54.2
054000 SEQ-TEST-004.                                                    OBSQ54.2
054100     READ SQ-FS7 AT END GO TO SEQ-TEST-004-01.                    OBSQ54.2
054200     MOVE SQ-FS7R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ54.2
054300     ADD 1 TO REC-COUNT.                                          OBSQ54.2
054400     IF REC-COUNT    GREATER THAN 750                             OBSQ54.2
054500              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ54.2
054600              GO TO SEQ-FAIL-004.                                 OBSQ54.2
054700     IF REC-COUNT    NOT EQUAL TO XRECORD-NUMBER (1)              OBSQ54.2
054800              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
054900              GO TO SEQ-TEST-004.                                 OBSQ54.2
055000     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS7"                      OBSQ54.2
055100              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
055200              GO TO SEQ-TEST-004.                                 OBSQ54.2
055300     IF CHARS-OR-RECORDS (1) NOT EQUAL "CH"                       OBSQ54.2
055400              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
055500              GO TO SEQ-TEST-004.                                 OBSQ54.2
055600     IF XBLOCK-SIZE (1) NOT EQUAL TO 2400                         OBSQ54.2
055700              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ54.2
055800     GO TO SEQ-TEST-004.                                          OBSQ54.2
055900 SEQ-TEST-004-01.                                                 OBSQ54.2
056000     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ54.2
056100              GO TO SEQ-PASS-004.                                 OBSQ54.2
056200     MOVE "ERRORS IN READING SQ-FS7" TO RE-MARK.                  OBSQ54.2
056300 SEQ-FAIL-004.                                                    OBSQ54.2
056400     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ54.2
056500     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ54.2
056600     PERFORM FAIL.                                                OBSQ54.2
056700     GO TO SEQ-WRITE-004.                                         OBSQ54.2
056800 SEQ-PASS-004.                                                    OBSQ54.2
056900     PERFORM PASS.                                                OBSQ54.2
057000     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ54.2
057100     MOVE REC-COUNT    TO CORRECT-18V0.                           OBSQ54.2
057200 SEQ-WRITE-004.                                                   OBSQ54.2
057300     MOVE "SEQ-TEST-004" TO PAR-NAME.                             OBSQ54.2
057400     MOVE "VERIFY FILE SQ-FS7" TO FEATURE.                        OBSQ54.2
057500     PERFORM PRINT-DETAIL.                                        OBSQ54.2
057600 SEQ-CLOSE-004.                                                   OBSQ54.2
057700     CLOSE SQ-FS7 WITH NO REWIND.                                 OBSQ54.2
057800 SEQ-INIT-005.                                                    OBSQ54.2
057900*             THIS TEST READS AND VALIDATES FILE SQ-FS8.          OBSQ54.2
058000     MOVE 0 TO REC-COUNT,    RECORDS-IN-ERROR.                    OBSQ54.2
058100     OPEN INPUT SQ-FS8 WITH NO REWIND.                            OBSQ54.2
058200 SEQ-TEST-005.                                                    OBSQ54.2
058300     READ SQ-FS8 AT END GO TO SEQ-TEST-005-01.                    OBSQ54.2
058400     MOVE SQ-FS8R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ54.2
058500     ADD 1 TO REC-COUNT.                                          OBSQ54.2
058600     IF REC-COUNT    GREATER THAN 750                             OBSQ54.2
058700              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ54.2
058800              GO TO SEQ-FAIL-005.                                 OBSQ54.2
058900     IF REC-COUNT    NOT EQUAL TO XRECORD-NUMBER (1)              OBSQ54.2
059000              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
059100              GO TO SEQ-TEST-005.                                 OBSQ54.2
059200     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS8"                      OBSQ54.2
059300              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
059400              GO TO SEQ-TEST-005.                                 OBSQ54.2
059500     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "CH"                    OBSQ54.2
059600              ADD 1 TO RECORDS-IN-ERROR                           OBSQ54.2
059700              GO TO SEQ-TEST-005.                                 OBSQ54.2
059800     IF XBLOCK-SIZE (1) NOT EQUAL TO 120                          OBSQ54.2
059900              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ54.2
060000     GO TO SEQ-TEST-005.                                          OBSQ54.2
060100 SEQ-TEST-005-01.                                                 OBSQ54.2
060200     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ54.2
060300              GO TO SEQ-PASS-005.                                 OBSQ54.2
060400     MOVE "ERRORS IN READING SQ-FS8" TO RE-MARK.                  OBSQ54.2
060500 SEQ-FAIL-005.                                                    OBSQ54.2
060600     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ54.2
060700     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ54.2
060800     PERFORM FAIL.                                                OBSQ54.2
060900     GO TO SEQ-WRITE-005.                                         OBSQ54.2
061000 SEQ-PASS-005.                                                    OBSQ54.2
061100     PERFORM PASS.                                                OBSQ54.2
061200     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ54.2
061300     MOVE REC-COUNT    TO CORRECT-18V0.                           OBSQ54.2
061400 SEQ-WRITE-005.                                                   OBSQ54.2
061500     MOVE "SEQ-TEST-005" TO PAR-NAME.                             OBSQ54.2
061600     MOVE "VERIFY FILE SQ-FS8" TO FEATURE.                        OBSQ54.2
061700     PERFORM PRINT-DETAIL.                                        OBSQ54.2
061800 SEQ-CLOSE-005.                                                   OBSQ54.2
061900     CLOSE SQ-FS8.                                                OBSQ54.2
062000 OBSQ5A-END-ROUTINE.                                              OBSQ54.2
062100     MOVE "END OF OBSQ5A VALIDATION TESTS" TO PRINT-REC.          OBSQ54.2
062200     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      OBSQ54.2
062300     GO TO CCVS-EXIT.                                             OBSQ54.2
062400 CCVS-EXIT SECTION.                                               OBSQ54.2
062500 CCVS-999999.                                                     OBSQ54.2
062600     GO TO CLOSE-FILES.                                           OBSQ54.2
      *END-OF,OBSQ5A                                                                  
