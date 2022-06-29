      *HEADER,COBOL,OBSQ3A,SUBPRG,OBSQ4A                                              
000100 IDENTIFICATION DIVISION.                                         OBSQ44.2
000200 PROGRAM-ID.                                                      OBSQ44.2
000300     OBSQ4A.                                                      OBSQ44.2
000400****************************************************************  OBSQ44.2
000500*                                                              *  OBSQ44.2
000600*    VALIDATION FOR:-                                          *  OBSQ44.2
000700*    " HIGH       ".                                              OBSQ44.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  OBSQ44.2
000900*                                                              *  OBSQ44.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  OBSQ44.2
001100*    "4.2 ".                                                      OBSQ44.2
001200*                                                              *  OBSQ44.2
001300*         THE ROUTINE OBSQ4A READS AND VALIDATES THE MULTIPLE     OBSQ44.2
001400*    FILE TAPE CREATED IN OBSQ3A. THE FOUR FILES CONTAINED ON     OBSQ44.2
001500*    THIS TAPE ARE SQ-FS1, SQ-FS2, SQ-FS3, AND SQ-FS4.  BOTH      OBSQ44.2
001600*    MULTIPLE FILE TAPES ONE AND TWO ARE THEN PASSED ON TO OBSQ5A.OBSQ44.2
001700*    OBSQ4A USES A MULTIPLE FILE CLAUSE WITH THE POSITION PHRASE  OBSQ44.2
001800*    TO PROCESS TAPE ONE.  THIS TAPE WAS CREATED USING OPEN AND   OBSQ44.2
001900*    CLOSE STATEMENTS WITH NO REWIND.                             OBSQ44.2
002000 ENVIRONMENT DIVISION.                                            OBSQ44.2
002100 CONFIGURATION SECTION.                                           OBSQ44.2
002200 SOURCE-COMPUTER.                                                 OBSQ44.2
002300     XXXXX082.                                                    OBSQ44.2
002400 OBJECT-COMPUTER.                                                 OBSQ44.2
002500     XXXXX083.                                                    OBSQ44.2
002600 INPUT-OUTPUT SECTION.                                            OBSQ44.2
002700 FILE-CONTROL.                                                    OBSQ44.2
002800*    SELECT RAW-DATA   ASSIGN TO                                  OBSQ44.2
002900*    XXXXX062                                                     OBSQ44.2
003000*           ORGANIZATION IS INDEXED                               OBSQ44.2
003100*           ACCESS MODE IS RANDOM                                 OBSQ44.2
003200*           RECORD KEY IS RAW-DATA-KEY.                           OBSQ44.2
003300     SELECT PRINT-FILE ASSIGN TO                                  OBSQ44.2
003400     XXXXX055.                                                    OBSQ44.2
003500     SELECT SQ-FS1 ASSIGN TO                                      OBSQ44.2
003600     XXXXP004.                                                    OBSQ44.2
003700     SELECT SQ-FS2 ASSIGN TO                                      OBSQ44.2
003800     XXXXP008.                                                    OBSQ44.2
003900     SELECT SQ-FS3 ASSIGN TO                                      OBSQ44.2
004000     XXXXP009.                                                    OBSQ44.2
004100     SELECT SQ-FS4 ASSIGN TO                                      OBSQ44.2
004200     XXXXP010.                                                    OBSQ44.2
004300 I-O-CONTROL.                                                     OBSQ44.2
004400     MULTIPLE FILE CONTAINS SQ-FS1 POSITION 1,                    OBSQ44.2
004500                            SQ-FS4 POSITION 4,                    OBSQ44.2
004600                            SQ-FS3 POSITION 3,                    OBSQ44.2
004700                            SQ-FS2 POSITION 2.                    OBSQ44.2
004800 DATA DIVISION.                                                   OBSQ44.2
004900 FILE SECTION.                                                    OBSQ44.2
005000*                                                                 OBSQ44.2
005100*FD  RAW-DATA.                                                    OBSQ44.2
005200*                                                                 OBSQ44.2
005300*01  RAW-DATA-SATZ.                                               OBSQ44.2
005400*    05  RAW-DATA-KEY        PIC X(6).                            OBSQ44.2
005500*    05  C-DATE              PIC 9(6).                            OBSQ44.2
005600*    05  C-TIME              PIC 9(8).                            OBSQ44.2
005700*    05  C-NO-OF-TESTS       PIC 99.                              OBSQ44.2
005800*    05  C-OK                PIC 999.                             OBSQ44.2
005900*    05  C-ALL               PIC 999.                             OBSQ44.2
006000*    05  C-FAIL              PIC 999.                             OBSQ44.2
006100*    05  C-DELETED           PIC 999.                             OBSQ44.2
006200*    05  C-INSPECT           PIC 999.                             OBSQ44.2
006300*    05  C-NOTE              PIC X(13).                           OBSQ44.2
006400*    05  C-INDENT            PIC X.                               OBSQ44.2
006500*    05  C-ABORT             PIC X(8).                            OBSQ44.2
006600 FD  PRINT-FILE.                                                  OBSQ44.2
006700 01  PRINT-REC PICTURE X(120).                                    OBSQ44.2
006800 01  DUMMY-RECORD PICTURE X(120).                                 OBSQ44.2
006900 FD  SQ-FS1                                                       OBSQ44.2
007000     LABEL RECORD STANDARD                                        OBSQ44.2
007100                 .                                                OBSQ44.2
007200 01  SQ-FS1R1-F-G-120   PIC X(120).                               OBSQ44.2
007300 FD  SQ-FS2                                                       OBSQ44.2
007400     LABEL RECORD STANDARD                                        OBSQ44.2
007500     BLOCK 5 RECORDS.                                             OBSQ44.2
007600 01  SQ-FS2R1-F-G-120   PIC X(120).                               OBSQ44.2
007700 FD  SQ-FS3                                                       OBSQ44.2
007800     LABEL RECORD STANDARD                                        OBSQ44.2
007900     RECORD CONTAINS 120 CHARACTERS                               OBSQ44.2
008000     BLOCK CONTAINS 1200 CHARACTERS.                              OBSQ44.2
008100 01  SQ-FS3R1-F-G-120   PIC X(120).                               OBSQ44.2
008200 FD  SQ-FS4                                                       OBSQ44.2
008300     LABEL RECORD IS STANDARD                                     OBSQ44.2
008400     RECORD          120 CHARACTERS                               OBSQ44.2
008500     BLOCK CONTAINS 10 RECORDS.                                   OBSQ44.2
008600 01  SQ-FS4R1-F-G-120   PIC X(120).                               OBSQ44.2
008700 WORKING-STORAGE SECTION.                                         OBSQ44.2
008800 77  RECORDS-COUNT       PIC 999   VALUE 0.                       OBSQ44.2
008900 77  RECORDS-IN-ERROR   PIC 999   VALUE 0.                        OBSQ44.2
009000 01  FILE-RECORD-INFORMATION-REC.                                 OBSQ44.2
009100     03 FILE-RECORD-INFO-SKELETON.                                OBSQ44.2
009200        05 FILLER                 PICTURE X(48)       VALUE       OBSQ44.2
009300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  OBSQ44.2
009400        05 FILLER                 PICTURE X(46)       VALUE       OBSQ44.2
009500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    OBSQ44.2
009600        05 FILLER                 PICTURE X(26)       VALUE       OBSQ44.2
009700             ",LFIL=000000,ORG=  ,LBLR= ".                        OBSQ44.2
009800        05 FILLER                 PICTURE X(37)       VALUE       OBSQ44.2
009900             ",RECKEY=                             ".             OBSQ44.2
010000        05 FILLER                 PICTURE X(38)       VALUE       OBSQ44.2
010100             ",ALTKEY1=                             ".            OBSQ44.2
010200        05 FILLER                 PICTURE X(38)       VALUE       OBSQ44.2
010300             ",ALTKEY2=                             ".            OBSQ44.2
010400        05 FILLER                 PICTURE X(7)        VALUE SPACE.OBSQ44.2
010500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              OBSQ44.2
010600        05 FILE-RECORD-INFO-P1-120.                               OBSQ44.2
010700           07 FILLER              PIC X(5).                       OBSQ44.2
010800           07 XFILE-NAME           PIC X(6).                      OBSQ44.2
010900           07 FILLER              PIC X(8).                       OBSQ44.2
011000           07 XRECORD-NAME         PIC X(6).                      OBSQ44.2
011100           07 FILLER              PIC X(1).                       OBSQ44.2
011200           07 REELUNIT-NUMBER     PIC 9(1).                       OBSQ44.2
011300           07 FILLER              PIC X(7).                       OBSQ44.2
011400           07 XRECORD-NUMBER       PIC 9(6).                      OBSQ44.2
011500           07 FILLER              PIC X(6).                       OBSQ44.2
011600           07 UPDATE-NUMBER       PIC 9(2).                       OBSQ44.2
011700           07 FILLER              PIC X(5).                       OBSQ44.2
011800           07 ODO-NUMBER          PIC 9(4).                       OBSQ44.2
011900           07 FILLER              PIC X(5).                       OBSQ44.2
012000           07 XPROGRAM-NAME        PIC X(5).                      OBSQ44.2
012100           07 FILLER              PIC X(7).                       OBSQ44.2
012200           07 XRECORD-LENGTH       PIC 9(6).                      OBSQ44.2
012300           07 FILLER              PIC X(7).                       OBSQ44.2
012400           07 CHARS-OR-RECORDS    PIC X(2).                       OBSQ44.2
012500           07 FILLER              PIC X(1).                       OBSQ44.2
012600           07 XBLOCK-SIZE          PIC 9(4).                      OBSQ44.2
012700           07 FILLER              PIC X(6).                       OBSQ44.2
012800           07 RECORDS-IN-FILE     PIC 9(6).                       OBSQ44.2
012900           07 FILLER              PIC X(5).                       OBSQ44.2
013000           07 XFILE-ORGANIZATION   PIC X(2).                      OBSQ44.2
013100           07 FILLER              PIC X(6).                       OBSQ44.2
013200           07 XLABEL-TYPE          PIC X(1).                      OBSQ44.2
013300        05 FILE-RECORD-INFO-P121-240.                             OBSQ44.2
013400           07 FILLER              PIC X(8).                       OBSQ44.2
013500           07 XRECORD-KEY          PIC X(29).                     OBSQ44.2
013600           07 FILLER              PIC X(9).                       OBSQ44.2
013700           07 ALTERNATE-KEY1      PIC X(29).                      OBSQ44.2
013800           07 FILLER              PIC X(9).                       OBSQ44.2
013900           07 ALTERNATE-KEY2      PIC X(29).                      OBSQ44.2
014000           07 FILLER              PIC X(7).                       OBSQ44.2
014100 01  TEST-RESULTS.                                                OBSQ44.2
014200     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ44.2
014300     02 FEATURE                   PICTURE X(20) VALUE SPACE.      OBSQ44.2
014400     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ44.2
014500     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       OBSQ44.2
014600     02 FILLER                    PICTURE X  VALUE SPACE.         OBSQ44.2
014700     02  PAR-NAME.                                                OBSQ44.2
014800       03 FILLER PICTURE X(12) VALUE SPACE.                       OBSQ44.2
014900       03  PARDOT-X PICTURE X  VALUE SPACE.                       OBSQ44.2
015000       03 DOTVALUE PICTURE 99  VALUE ZERO.                        OBSQ44.2
015100       03 FILLER PIC X(5) VALUE SPACE.                            OBSQ44.2
015200     02 FILLER PIC X(10) VALUE SPACE.                             OBSQ44.2
015300     02 RE-MARK PIC X(61).                                        OBSQ44.2
015400 01  TEST-COMPUTED.                                               OBSQ44.2
015500     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ44.2
015600     02 FILLER PIC X(17) VALUE "       COMPUTED=".                OBSQ44.2
015700     02 COMPUTED-X.                                               OBSQ44.2
015800     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      OBSQ44.2
015900     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       OBSQ44.2
016000     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      OBSQ44.2
016100     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  OBSQ44.2
016200     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  OBSQ44.2
016300     03       CM-18V0 REDEFINES COMPUTED-A.                       OBSQ44.2
016400         04 COMPUTED-18V0                   PICTURE -9(18).       OBSQ44.2
016500         04 FILLER                          PICTURE X.            OBSQ44.2
016600     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ44.2
016700 01  TEST-CORRECT.                                                OBSQ44.2
016800     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ44.2
016900     02 FILLER PIC X(17) VALUE "       CORRECT =".                OBSQ44.2
017000     02 CORRECT-X.                                                OBSQ44.2
017100     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      OBSQ44.2
017200     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         OBSQ44.2
017300     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      OBSQ44.2
017400     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  OBSQ44.2
017500     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  OBSQ44.2
017600     03      CR-18V0 REDEFINES CORRECT-A.                         OBSQ44.2
017700         04 CORRECT-18V0                    PICTURE -9(18).       OBSQ44.2
017800         04 FILLER                          PICTURE X.            OBSQ44.2
017900     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ44.2
018000 01  CCVS-C-1.                                                    OBSQ44.2
018100     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PAOBSQ44.2
018200-    "SS  PARAGRAPH-NAME                                          OBSQ44.2
018300-    "        REMARKS".                                           OBSQ44.2
018400     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   OBSQ44.2
018500 01  CCVS-C-2.                                                    OBSQ44.2
018600     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ44.2
018700     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 OBSQ44.2
018800     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   OBSQ44.2
018900     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   OBSQ44.2
019000     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   OBSQ44.2
019100 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         OBSQ44.2
019200 01  REC-CT PICTURE 99 VALUE ZERO.                                OBSQ44.2
019300 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        OBSQ44.2
019400 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  OBSQ44.2
019500 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          OBSQ44.2
019600 01  PASS-COUNTER PIC 999 VALUE ZERO.                             OBSQ44.2
019700 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              OBSQ44.2
019800 01  ERROR-HOLD PIC 999 VALUE ZERO.                               OBSQ44.2
019900 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           OBSQ44.2
020000 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            OBSQ44.2
020100 01  CCVS-H-1.                                                    OBSQ44.2
020200     02  FILLER   PICTURE X(27)  VALUE SPACE.                     OBSQ44.2
020300     02 FILLER PICTURE X(67) VALUE                                OBSQ44.2
020400     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  OBSQ44.2
020500-    " SYSTEM".                                                   OBSQ44.2
020600     02  FILLER     PICTURE X(26)  VALUE SPACE.                   OBSQ44.2
020700 01  CCVS-H-2.                                                    OBSQ44.2
020800     02 FILLER PICTURE X(52) VALUE IS                             OBSQ44.2
020900     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   OBSQ44.2
021000     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   OBSQ44.2
021100     02 TEST-ID PICTURE IS X(9).                                  OBSQ44.2
021200     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   OBSQ44.2
021300 01  CCVS-H-3.                                                    OBSQ44.2
021400     02  FILLER PICTURE X(34) VALUE                               OBSQ44.2
021500     " FOR OFFICIAL USE ONLY    ".                                OBSQ44.2
021600     02  FILLER PICTURE X(58) VALUE                               OBSQ44.2
021700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBSQ44.2
021800     02  FILLER PICTURE X(28) VALUE                               OBSQ44.2
021900     "  COPYRIGHT   1985 ".                                       OBSQ44.2
022000 01  CCVS-E-1.                                                    OBSQ44.2
022100     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   OBSQ44.2
022200     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        OBSQ44.2
022300     02 ID-AGAIN PICTURE IS X(9).                                 OBSQ44.2
022400     02 FILLER PICTURE X(45) VALUE IS                             OBSQ44.2
022500     " NTIS DISTRIBUTION COBOL 85".                               OBSQ44.2
022600 01  CCVS-E-2.                                                    OBSQ44.2
022700     02  FILLER                   PICTURE X(31)  VALUE            OBSQ44.2
022800     SPACE.                                                       OBSQ44.2
022900     02  FILLER                   PICTURE X(21)  VALUE SPACE.     OBSQ44.2
023000     02 CCVS-E-2-2.                                               OBSQ44.2
023100         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            OBSQ44.2
023200         03 FILLER PICTURE IS X VALUE IS SPACE.                   OBSQ44.2
023300         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      OBSQ44.2
023400 01  CCVS-E-3.                                                    OBSQ44.2
023500     02  FILLER PICTURE X(22) VALUE                               OBSQ44.2
023600     " FOR OFFICIAL USE ONLY".                                    OBSQ44.2
023700     02  FILLER PICTURE X(12) VALUE SPACE.                        OBSQ44.2
023800     02  FILLER PICTURE X(58) VALUE                               OBSQ44.2
023900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBSQ44.2
024000     02  FILLER PICTURE X(13) VALUE SPACE.                        OBSQ44.2
024100     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 OBSQ44.2
024200 01  CCVS-E-4.                                                    OBSQ44.2
024300     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           OBSQ44.2
024400     02 FILLER PIC XXXX VALUE " OF ".                             OBSQ44.2
024500     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           OBSQ44.2
024600     02 FILLER PIC X(40) VALUE                                    OBSQ44.2
024700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       OBSQ44.2
024800 01  XXINFO.                                                      OBSQ44.2
024900     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    OBSQ44.2
025000     02 INFO-TEXT.                                                OBSQ44.2
025100     04 FILLER PIC X(20) VALUE SPACE.                             OBSQ44.2
025200     04 XXCOMPUTED PIC X(20).                                     OBSQ44.2
025300     04 FILLER PIC X(5) VALUE SPACE.                              OBSQ44.2
025400     04 XXCORRECT PIC X(20).                                      OBSQ44.2
025500 01  HYPHEN-LINE.                                                 OBSQ44.2
025600     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ44.2
025700     02 FILLER PICTURE IS X(65) VALUE IS "************************OBSQ44.2
025800-    "*****************************************".                 OBSQ44.2
025900     02 FILLER PICTURE IS X(54) VALUE IS "************************OBSQ44.2
026000-    "******************************".                            OBSQ44.2
026100 01  CCVS-PGM-ID PIC X(6) VALUE                                   OBSQ44.2
026200     "OBSQ4A".                                                    OBSQ44.2
026300 PROCEDURE DIVISION.                                              OBSQ44.2
026400 CCVS1 SECTION.                                                   OBSQ44.2
026500 OPEN-FILES.                                                      OBSQ44.2
026600*    OPEN I-O RAW-DATA.                                           OBSQ44.2
026700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ44.2
026800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     OBSQ44.2
026900*    MOVE "ABORTED " TO C-ABORT.                                  OBSQ44.2
027000*    ADD 1 TO C-NO-OF-TESTS.                                      OBSQ44.2
027100*    ACCEPT C-DATE  FROM DATE.                                    OBSQ44.2
027200*    ACCEPT C-TIME  FROM TIME.                                    OBSQ44.2
027300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             OBSQ44.2
027400*END-E-1.                                                         OBSQ44.2
027500*    CLOSE RAW-DATA.                                              OBSQ44.2
027600     OPEN     OUTPUT PRINT-FILE.                                  OBSQ44.2
027700     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   OBSQ44.2
027800     MOVE    SPACE TO TEST-RESULTS.                               OBSQ44.2
027900     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             OBSQ44.2
028000     MOVE ZERO TO REC-SKL-SUB.                                    OBSQ44.2
028100     PERFORM CCVS-INIT-FILE 9 TIMES.                              OBSQ44.2
028200 CCVS-INIT-FILE.                                                  OBSQ44.2
028300     ADD 1 TO REC-SKL-SUB.                                        OBSQ44.2
028400     MOVE FILE-RECORD-INFO-SKELETON TO                            OBSQ44.2
028500                  FILE-RECORD-INFO (REC-SKL-SUB).                 OBSQ44.2
028600 CCVS-INIT-EXIT.                                                  OBSQ44.2
028700     GO TO CCVS1-EXIT.                                            OBSQ44.2
028800 CLOSE-FILES.                                                     OBSQ44.2
028900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   OBSQ44.2
029000*    OPEN I-O RAW-DATA.                                           OBSQ44.2
029100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ44.2
029200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     OBSQ44.2
029300*    MOVE "OK.     " TO C-ABORT.                                  OBSQ44.2
029400*    MOVE PASS-COUNTER TO C-OK.                                   OBSQ44.2
029500*    MOVE ERROR-HOLD   TO C-ALL.                                  OBSQ44.2
029600*    MOVE ERROR-COUNTER TO C-FAIL.                                OBSQ44.2
029700*    MOVE DELETE-CNT TO C-DELETED.                                OBSQ44.2
029800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           OBSQ44.2
029900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             OBSQ44.2
030000*END-E-2.                                                         OBSQ44.2
030100*    CLOSE RAW-DATA.                                              OBSQ44.2
030200 TERMINATE-CCVS.                                                  OBSQ44.2
030300*    EXIT PROGRAM.                                                OBSQ44.2
030400*TERMINATE-CALL.                                                  OBSQ44.2
030500     STOP     RUN.                                                OBSQ44.2
030600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         OBSQ44.2
030700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           OBSQ44.2
030800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          OBSQ44.2
030900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          OBSQ44.2
031000     MOVE "****TEST DELETED****" TO RE-MARK.                      OBSQ44.2
031100 PRINT-DETAIL.                                                    OBSQ44.2
031200     IF REC-CT NOT EQUAL TO ZERO                                  OBSQ44.2
031300             MOVE "." TO PARDOT-X                                 OBSQ44.2
031400             MOVE REC-CT TO DOTVALUE.                             OBSQ44.2
031500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      OBSQ44.2
031600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               OBSQ44.2
031700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 OBSQ44.2
031800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 OBSQ44.2
031900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              OBSQ44.2
032000     MOVE SPACE TO CORRECT-X.                                     OBSQ44.2
032100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         OBSQ44.2
032200     MOVE     SPACE TO RE-MARK.                                   OBSQ44.2
032300 HEAD-ROUTINE.                                                    OBSQ44.2
032400     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ44.2
032500     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   OBSQ44.2
032600     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   OBSQ44.2
032700 COLUMN-NAMES-ROUTINE.                                            OBSQ44.2
032800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ44.2
032900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ44.2
033000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        OBSQ44.2
033100 END-ROUTINE.                                                     OBSQ44.2
033200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.OBSQ44.2
033300 END-RTN-EXIT.                                                    OBSQ44.2
033400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ44.2
033500 END-ROUTINE-1.                                                   OBSQ44.2
033600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      OBSQ44.2
033700      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   OBSQ44.2
033800      ADD PASS-COUNTER TO ERROR-HOLD.                             OBSQ44.2
033900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   OBSQ44.2
034000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            OBSQ44.2
034100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              OBSQ44.2
034200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                OBSQ44.2
034300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           OBSQ44.2
034400  END-ROUTINE-12.                                                 OBSQ44.2
034500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        OBSQ44.2
034600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      OBSQ44.2
034700         MOVE "NO " TO ERROR-TOTAL                                OBSQ44.2
034800         ELSE                                                     OBSQ44.2
034900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       OBSQ44.2
035000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           OBSQ44.2
035100     PERFORM WRITE-LINE.                                          OBSQ44.2
035200 END-ROUTINE-13.                                                  OBSQ44.2
035300     IF DELETE-CNT IS EQUAL TO ZERO                               OBSQ44.2
035400         MOVE "NO " TO ERROR-TOTAL  ELSE                          OBSQ44.2
035500         MOVE DELETE-CNT TO ERROR-TOTAL.                          OBSQ44.2
035600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   OBSQ44.2
035700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ44.2
035800      IF   INSPECT-COUNTER EQUAL TO ZERO                          OBSQ44.2
035900          MOVE "NO " TO ERROR-TOTAL                               OBSQ44.2
036000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   OBSQ44.2
036100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            OBSQ44.2
036200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          OBSQ44.2
036300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ44.2
036400 WRITE-LINE.                                                      OBSQ44.2
036500     ADD 1 TO RECORD-COUNT.                                       OBSQ44.2
036600*    IF RECORD-COUNT GREATER 50                                   OBSQ44.2
036700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          OBSQ44.2
036800*        MOVE SPACE TO DUMMY-RECORD                               OBSQ44.2
036900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  OBSQ44.2
037000*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             OBSQ44.2
037100*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     OBSQ44.2
037200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          OBSQ44.2
037300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          OBSQ44.2
037400*        MOVE ZERO TO RECORD-COUNT.                               OBSQ44.2
037500     PERFORM WRT-LN.                                              OBSQ44.2
037600 WRT-LN.                                                          OBSQ44.2
037700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               OBSQ44.2
037800     MOVE SPACE TO DUMMY-RECORD.                                  OBSQ44.2
037900 BLANK-LINE-PRINT.                                                OBSQ44.2
038000     PERFORM WRT-LN.                                              OBSQ44.2
038100 FAIL-ROUTINE.                                                    OBSQ44.2
038200     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   OBSQ44.2
038300     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    OBSQ44.2
038400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    OBSQ44.2
038500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ44.2
038600     GO TO FAIL-ROUTINE-EX.                                       OBSQ44.2
038700 FAIL-ROUTINE-WRITE.                                              OBSQ44.2
038800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           OBSQ44.2
038900     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   OBSQ44.2
039000 FAIL-ROUTINE-EX. EXIT.                                           OBSQ44.2
039100 BAIL-OUT.                                                        OBSQ44.2
039200     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       OBSQ44.2
039300     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               OBSQ44.2
039400 BAIL-OUT-WRITE.                                                  OBSQ44.2
039500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  OBSQ44.2
039600     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ44.2
039700 BAIL-OUT-EX. EXIT.                                               OBSQ44.2
039800 CCVS1-EXIT.                                                      OBSQ44.2
039900     EXIT.                                                        OBSQ44.2
040000 SECT-OBSQ4A-0001 SECTION.                                        OBSQ44.2
040100 SEQ-INIT-001.                                                    OBSQ44.2
040200     MOVE 0 TO RECORDS-COUNT, RECORDS-IN-ERROR.                   OBSQ44.2
040300     OPEN INPUT SQ-FS1.                                           OBSQ44.2
040400 SEQ-TEST-001.                                                    OBSQ44.2
040500     READ SQ-FS1 AT END GO TO SEQ-TEST-001-01.                    OBSQ44.2
040600     MOVE SQ-FS1R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ44.2
040700     ADD 1 TO RECORDS-COUNT.                                      OBSQ44.2
040800     IF RECORDS-COUNT GREATER THAN 750                            OBSQ44.2
040900              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ44.2
041000              GO TO SEQ-FAIL-001.                                 OBSQ44.2
041100     IF RECORDS-COUNT NOT EQUAL TO XRECORD-NUMBER (1)             OBSQ44.2
041200              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
041300              GO TO SEQ-TEST-001.                                 OBSQ44.2
041400     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS1"                      OBSQ44.2
041500              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
041600              GO TO SEQ-TEST-001.                                 OBSQ44.2
041700     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "RC"                    OBSQ44.2
041800              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
041900              GO TO SEQ-TEST-001.                                 OBSQ44.2
042000     IF XBLOCK-SIZE (1) NOT EQUAL TO 1                            OBSQ44.2
042100              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ44.2
042200     GO TO SEQ-TEST-001.                                          OBSQ44.2
042300 SEQ-TEST-001-01.                                                 OBSQ44.2
042400     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ44.2
042500              GO TO SEQ-PASS-001.                                 OBSQ44.2
042600     MOVE "ERRORS IN READING SQ-FS1" TO RE-MARK.                  OBSQ44.2
042700 SEQ-FAIL-001.                                                    OBSQ44.2
042800     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ44.2
042900     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ44.2
043000     PERFORM FAIL.                                                OBSQ44.2
043100     GO TO SEQ-WRITE-001.                                         OBSQ44.2
043200 SEQ-PASS-001.                                                    OBSQ44.2
043300     PERFORM PASS.                                                OBSQ44.2
043400     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ44.2
043500     MOVE RECORDS-COUNT TO CORRECT-18V0.                          OBSQ44.2
043600 SEQ-WRITE-001.                                                   OBSQ44.2
043700     MOVE "SEQ-TEST-001" TO PAR-NAME.                             OBSQ44.2
043800     MOVE "VERIFY FILE SQ-FS1" TO FEATURE.                        OBSQ44.2
043900     PERFORM PRINT-DETAIL.                                        OBSQ44.2
044000 SEQ-CLOSE-001.                                                   OBSQ44.2
044100     CLOSE SQ-FS1.                                                OBSQ44.2
044200 SEQ-INIT-002.                                                    OBSQ44.2
044300*             THIS TEST READS AND VALIDATES FILE SQ-FS3.          OBSQ44.2
044400     MOVE 0 TO RECORDS-COUNT, RECORDS-IN-ERROR.                   OBSQ44.2
044500     OPEN INPUT SQ-FS3.                                           OBSQ44.2
044600 SEQ-TEST-002.                                                    OBSQ44.2
044700     READ SQ-FS3 AT END GO TO SEQ-TEST-002-01.                    OBSQ44.2
044800     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ44.2
044900     ADD 1 TO RECORDS-COUNT.                                      OBSQ44.2
045000     IF RECORDS-COUNT GREATER THAN 750                            OBSQ44.2
045100              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ44.2
045200              GO TO SEQ-FAIL-002.                                 OBSQ44.2
045300     IF RECORDS-COUNT NOT EQUAL TO XRECORD-NUMBER (1)             OBSQ44.2
045400              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
045500              GO TO SEQ-TEST-002.                                 OBSQ44.2
045600     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS3"                      OBSQ44.2
045700              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
045800              GO TO SEQ-TEST-002.                                 OBSQ44.2
045900     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "CH"                    OBSQ44.2
046000              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
046100              GO TO SEQ-TEST-002.                                 OBSQ44.2
046200     IF XBLOCK-SIZE (1) NOT EQUAL TO 1200                         OBSQ44.2
046300              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ44.2
046400     GO TO SEQ-TEST-002.                                          OBSQ44.2
046500 SEQ-TEST-002-01.                                                 OBSQ44.2
046600     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ44.2
046700              GO TO SEQ-PASS-002.                                 OBSQ44.2
046800     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  OBSQ44.2
046900 SEQ-FAIL-002.                                                    OBSQ44.2
047000     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ44.2
047100     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ44.2
047200     PERFORM FAIL.                                                OBSQ44.2
047300     GO TO SEQ-WRITE-002.                                         OBSQ44.2
047400 SEQ-PASS-002.                                                    OBSQ44.2
047500     PERFORM PASS.                                                OBSQ44.2
047600     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ44.2
047700     MOVE RECORDS-COUNT TO CORRECT-18V0.                          OBSQ44.2
047800 SEQ-WRITE-002.                                                   OBSQ44.2
047900     MOVE "SEQ-TEST-002" TO PAR-NAME.                             OBSQ44.2
048000     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        OBSQ44.2
048100     PERFORM PRINT-DETAIL.                                        OBSQ44.2
048200 SEQ-CLOSE-002.                                                   OBSQ44.2
048300     CLOSE SQ-FS3.                                                OBSQ44.2
048400 SEQ-INIT-003.                                                    OBSQ44.2
048500*             THIS TEST READS AND VALIDATES FILE SQ-FS2.          OBSQ44.2
048600     MOVE 0 TO RECORDS-COUNT, RECORDS-IN-ERROR.                   OBSQ44.2
048700     OPEN INPUT SQ-FS2.                                           OBSQ44.2
048800 SEQ-TEST-003.                                                    OBSQ44.2
048900     READ SQ-FS2 AT END GO TO SEQ-TEST-003-01.                    OBSQ44.2
049000     MOVE SQ-FS2R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ44.2
049100     ADD 1 TO RECORDS-COUNT.                                      OBSQ44.2
049200     IF RECORDS-COUNT GREATER THAN 750                            OBSQ44.2
049300              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ44.2
049400              GO TO SEQ-FAIL-003.                                 OBSQ44.2
049500     IF RECORDS-COUNT NOT EQUAL TO XRECORD-NUMBER (1)             OBSQ44.2
049600              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
049700              GO TO SEQ-TEST-003.                                 OBSQ44.2
049800     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS2"                      OBSQ44.2
049900              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
050000              GO TO SEQ-TEST-003.                                 OBSQ44.2
050100     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "RC"                    OBSQ44.2
050200              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
050300              GO TO SEQ-TEST-003.                                 OBSQ44.2
050400     IF XBLOCK-SIZE (1) NOT EQUAL TO 5                            OBSQ44.2
050500              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ44.2
050600     GO TO SEQ-TEST-003.                                          OBSQ44.2
050700 SEQ-TEST-003-01.                                                 OBSQ44.2
050800     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ44.2
050900              GO TO SEQ-PASS-003.                                 OBSQ44.2
051000     MOVE "ERRORS IN READING SQ-FS2" TO RE-MARK.                  OBSQ44.2
051100 SEQ-FAIL-003.                                                    OBSQ44.2
051200     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ44.2
051300     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ44.2
051400     PERFORM FAIL.                                                OBSQ44.2
051500     GO TO SEQ-WRITE-003.                                         OBSQ44.2
051600 SEQ-PASS-003.                                                    OBSQ44.2
051700     PERFORM PASS.                                                OBSQ44.2
051800     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ44.2
051900     MOVE RECORDS-COUNT TO CORRECT-18V0.                          OBSQ44.2
052000 SEQ-WRITE-003.                                                   OBSQ44.2
052100     MOVE "SEQ-TEST-003" TO PAR-NAME.                             OBSQ44.2
052200     MOVE "VERIFY FILE SQ-FS2" TO FEATURE.                        OBSQ44.2
052300     PERFORM PRINT-DETAIL.                                        OBSQ44.2
052400 SEQ-CLOSE-003.                                                   OBSQ44.2
052500     CLOSE SQ-FS2.                                                OBSQ44.2
052600 SEQ-INIT-004.                                                    OBSQ44.2
052700*             THIS TEST READS AND VALIDATES FILE SQ-FS4.          OBSQ44.2
052800     MOVE 0 TO RECORDS-COUNT, RECORDS-IN-ERROR.                   OBSQ44.2
052900     OPEN INPUT SQ-FS4.                                           OBSQ44.2
053000 SEQ-TEST-004.                                                    OBSQ44.2
053100     READ SQ-FS4 AT END GO TO SEQ-TEST-004-01.                    OBSQ44.2
053200     MOVE SQ-FS4R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        OBSQ44.2
053300     ADD 1 TO RECORDS-COUNT.                                      OBSQ44.2
053400     IF RECORDS-COUNT GREATER THAN 750                            OBSQ44.2
053500              MOVE "MORE THAN 750 RECORDS" TO RE-MARK             OBSQ44.2
053600              GO TO SEQ-FAIL-004.                                 OBSQ44.2
053700     IF RECORDS-COUNT NOT EQUAL TO XRECORD-NUMBER (1)             OBSQ44.2
053800              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
053900              GO TO SEQ-TEST-004.                                 OBSQ44.2
054000     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS4"                      OBSQ44.2
054100              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
054200              GO TO SEQ-TEST-004.                                 OBSQ44.2
054300     IF CHARS-OR-RECORDS (1) NOT EQUAL TO "RC"                    OBSQ44.2
054400              ADD 1 TO RECORDS-IN-ERROR                           OBSQ44.2
054500              GO TO SEQ-TEST-004.                                 OBSQ44.2
054600     IF XBLOCK-SIZE (1) NOT EQUAL TO 10                           OBSQ44.2
054700              ADD 1 TO RECORDS-IN-ERROR.                          OBSQ44.2
054800     GO TO SEQ-TEST-004.                                          OBSQ44.2
054900 SEQ-TEST-004-01.                                                 OBSQ44.2
055000     IF RECORDS-IN-ERROR EQUAL TO ZERO                            OBSQ44.2
055100              GO TO SEQ-PASS-004.                                 OBSQ44.2
055200     MOVE "ERRORS IN READING SQ-FS4" TO RE-MARK.                  OBSQ44.2
055300 SEQ-FAIL-004.                                                    OBSQ44.2
055400     MOVE "RECORDS IN ERROR" TO COMPUTED-A.                       OBSQ44.2
055500     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       OBSQ44.2
055600     PERFORM FAIL.                                                OBSQ44.2
055700     GO TO SEQ-WRITE-004.                                         OBSQ44.2
055800 SEQ-PASS-004.                                                    OBSQ44.2
055900     PERFORM PASS.                                                OBSQ44.2
056000     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   OBSQ44.2
056100     MOVE RECORDS-COUNT TO CORRECT-18V0.                          OBSQ44.2
056200 SEQ-WRITE-004.                                                   OBSQ44.2
056300     MOVE "SEQ-TEST-004" TO PAR-NAME.                             OBSQ44.2
056400     MOVE "VERIFY FILE SQ-FS4" TO FEATURE.                        OBSQ44.2
056500     PERFORM PRINT-DETAIL.                                        OBSQ44.2
056600 SEQ-CLOSE-004.                                                   OBSQ44.2
056700     CLOSE SQ-FS4.                                                OBSQ44.2
056800 OBSQ4A-END-ROUTINE.                                              OBSQ44.2
056900     MOVE "END OF OBSQ4A VALIDATION TESTS" TO PRINT-REC.          OBSQ44.2
057000     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      OBSQ44.2
057100     GO TO CCVS-EXIT.                                             OBSQ44.2
057200 CCVS-EXIT SECTION.                                               OBSQ44.2
057300 CCVS-999999.                                                     OBSQ44.2
057400     GO TO CLOSE-FILES.                                           OBSQ44.2
      *END-OF,OBSQ4A                                                                  
