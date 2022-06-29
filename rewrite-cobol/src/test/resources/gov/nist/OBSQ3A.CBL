      *HEADER,COBOL,OBSQ3A                                                            
000100 IDENTIFICATION DIVISION.                                         OBSQ34.2
000200 PROGRAM-ID.                                                      OBSQ34.2
000300     OBSQ3A.                                                      OBSQ34.2
000400****************************************************************  OBSQ34.2
000500*                                                              *  OBSQ34.2
000600*    VALIDATION FOR:-                                          *  OBSQ34.2
000700*    " HIGH       ".                                              OBSQ34.2
000800*    USING CCVS85 VERSION 1.0 ISSUED IN JANUARY 1986.          *  OBSQ34.2
000900*                                                              *  OBSQ34.2
001000*    CREATION DATE     /     VALIDATION DATE                   *  OBSQ34.2
001100*    "4.2 ".                                                      OBSQ34.2
001200*                                                              *  OBSQ34.2
001300*    THIS ROUTINE TESTS THE USE OF MULTIPLE FILE CLAUSE           OBSQ34.2
001400*    OF THE I-O-CONTROL PARAGRAPH.  TWO TAPES ARE CREATED         OBSQ34.2
001500*    CONTAINING 4 FILES EACH.  TAPE ONE IS CREATED WITHOUT THE    OBSQ34.2
001600*    USE OF THE NO REWIND OPTION WITH THE OPEN AND CLOSE          OBSQ34.2
001700*   STATEMENTS. IT IS THEN PASSED ON TO OBSQ4A AND OBSQ5A WHERE ITOBSQ34.2
001800*    IS READ AND VALIDATED.  TAPE TWO IS CREATED USING THE        OBSQ34.2
001900*    POSITION PHRASE OF THE MULTIPLE FILE CLAUSE AND WITH THE USE OBSQ34.2
002000*    OF THE NO REWIND OPTION WITH THE OPEN AND CLOSE STATEMENT.   OBSQ34.2
002100*    TAPE TWO IS THEN PASSED ON TO OBSQ5A WHERE IT IS READ AND    OBSQ34.2
002200*    VALIDATED.                                                   OBSQ34.2
002300 ENVIRONMENT DIVISION.                                            OBSQ34.2
002400 CONFIGURATION SECTION.                                           OBSQ34.2
002500 SOURCE-COMPUTER.                                                 OBSQ34.2
002600     XXXXX082.                                                    OBSQ34.2
002700 OBJECT-COMPUTER.                                                 OBSQ34.2
002800     XXXXX083.                                                    OBSQ34.2
002900 INPUT-OUTPUT SECTION.                                            OBSQ34.2
003000 FILE-CONTROL.                                                    OBSQ34.2
003100*    SELECT RAW-DATA   ASSIGN TO                                  OBSQ34.2
003200*    XXXXX062                                                     OBSQ34.2
003300*           ORGANIZATION IS INDEXED                               OBSQ34.2
003400*           ACCESS MODE IS RANDOM                                 OBSQ34.2
003500*           RECORD KEY IS RAW-DATA-KEY.                           OBSQ34.2
003600     SELECT PRINT-FILE ASSIGN TO                                  OBSQ34.2
003700     XXXXX055.                                                    OBSQ34.2
003800     SELECT SQ-FS1 ASSIGN TO                                      OBSQ34.2
003900     XXXXP004                                                     OBSQ34.2
004000     ORGANIZATION IS SEQUENTIAL.                                  OBSQ34.2
004100     SELECT SQ-FS2 ASSIGN TO                                      OBSQ34.2
004200     XXXXP008                                                     OBSQ34.2
004300     ACCESS MODE IS SEQUENTIAL.                                   OBSQ34.2
004400     SELECT SQ-FS3 ASSIGN                                         OBSQ34.2
004500     XXXXP009                                                     OBSQ34.2
004600     ORGANIZATION IS SEQUENTIAL.                                  OBSQ34.2
004700     SELECT SQ-FS4 ASSIGN                                         OBSQ34.2
004800     XXXXP010                                                     OBSQ34.2
004900     ACCESS MODE SEQUENTIAL.                                      OBSQ34.2
005000     SELECT SQ-FS5 ASSIGN                                         OBSQ34.2
005100     XXXXP005.                                                    OBSQ34.2
005200     SELECT SQ-FS6 ASSIGN                                         OBSQ34.2
005300     XXXXP011                                                     OBSQ34.2
005400     ORGANIZATION IS SEQUENTIAL.                                  OBSQ34.2
005500     SELECT SQ-FS7 ASSIGN TO                                      OBSQ34.2
005600     XXXXP012                                                     OBSQ34.2
005700     ORGANIZATION IS SEQUENTIAL                                   OBSQ34.2
005800     ACCESS MODE IS SEQUENTIAL.                                   OBSQ34.2
005900     SELECT SQ-FS8 ASSIGN TO                                      OBSQ34.2
006000     XXXXP013                                                     OBSQ34.2
006100     ACCESS MODE IS SEQUENTIAL.                                   OBSQ34.2
006200 I-O-CONTROL.                                                     OBSQ34.2
006300     MULTIPLE FILE TAPE CONTAINS SQ-FS1,                          OBSQ34.2
006400                                 SQ-FS2,                          OBSQ34.2
006500                                 SQ-FS3,                          OBSQ34.2
006600                                 SQ-FS4;                          OBSQ34.2
006700     MULTIPLE FILE TAPE SQ-FS8 POSITION 4,                        OBSQ34.2
006800                        SQ-FS7 POSITION 3,                        OBSQ34.2
006900                        SQ-FS6 POSITION 2,                        OBSQ34.2
007000                        SQ-FS5 POSITION 1.                        OBSQ34.2
007100 DATA DIVISION.                                                   OBSQ34.2
007200 FILE SECTION.                                                    OBSQ34.2
007300*                                                                 OBSQ34.2
007400*FD  RAW-DATA.                                                    OBSQ34.2
007500*                                                                 OBSQ34.2
007600*01  RAW-DATA-SATZ.                                               OBSQ34.2
007700*    05  RAW-DATA-KEY        PIC X(6).                            OBSQ34.2
007800*    05  C-DATE              PIC 9(6).                            OBSQ34.2
007900*    05  C-TIME              PIC 9(8).                            OBSQ34.2
008000*    05  C-NO-OF-TESTS       PIC 99.                              OBSQ34.2
008100*    05  C-OK                PIC 999.                             OBSQ34.2
008200*    05  C-ALL               PIC 999.                             OBSQ34.2
008300*    05  C-FAIL              PIC 999.                             OBSQ34.2
008400*    05  C-DELETED           PIC 999.                             OBSQ34.2
008500*    05  C-INSPECT           PIC 999.                             OBSQ34.2
008600*    05  C-NOTE              PIC X(13).                           OBSQ34.2
008700*    05  C-INDENT            PIC X.                               OBSQ34.2
008800*    05  C-ABORT             PIC X(8).                            OBSQ34.2
008900 FD  PRINT-FILE.                                                  OBSQ34.2
009000 01  PRINT-REC PICTURE X(120).                                    OBSQ34.2
009100 01  DUMMY-RECORD PICTURE X(120).                                 OBSQ34.2
009200 FD  SQ-FS1                                                       OBSQ34.2
009300     LABEL RECORD IS STANDARD                                     OBSQ34.2
009400          .                                                       OBSQ34.2
009500 01  SQ-FS1R1-F-G-120   PIC X(120).                               OBSQ34.2
009600 FD  SQ-FS2                                                       OBSQ34.2
009700     LABEL RECORD STANDARD                                        OBSQ34.2
009800     BLOCK CONTAINS 5 RECORDS.                                    OBSQ34.2
009900 01  SQ-FS2R1-F-G-120   PIC X(120).                               OBSQ34.2
010000 FD  SQ-FS3                                                       OBSQ34.2
010100     LABEL RECORD STANDARD                                        OBSQ34.2
010200     BLOCK CONTAINS 1200 CHARACTERS                               OBSQ34.2
010300     RECORD CONTAINS 120 CHARACTERS.                              OBSQ34.2
010400 01  SQ-FS3R1-F-G-120   PIC X(120).                               OBSQ34.2
010500 FD  SQ-FS4                                                       OBSQ34.2
010600     LABEL RECORDS STANDARD                                       OBSQ34.2
010700     BLOCK 10 RECORDS                                             OBSQ34.2
010800     RECORD 120 CHARACTERS.                                       OBSQ34.2
010900 01  SQ-FS4R1-F-G-120   PIC X(120).                               OBSQ34.2
011000 FD  SQ-FS5                                                       OBSQ34.2
011100     LABEL RECORDS ARE STANDARD                                   OBSQ34.2
011200     BLOCK CONTAINS 5 RECORDS.                                    OBSQ34.2
011300 01  SQ-FS5R1-F-G-120   PIC X(120).                               OBSQ34.2
011400 FD  SQ-FS6                                                       OBSQ34.2
011500     LABEL RECORD IS STANDARD                                     OBSQ34.2
011600     BLOCK CONTAINS 10 RECORDS.                                   OBSQ34.2
011700 01  SQ-FS6R1-F-G-120   PIC X(120).                               OBSQ34.2
011800 FD  SQ-FS7                                                       OBSQ34.2
011900     LABEL RECORD STANDARD                                        OBSQ34.2
012000     BLOCK CONTAINS 2400 CHARACTERS.                              OBSQ34.2
012100 01  SQ-FS7R1-F-G-120   PIC X(120).                               OBSQ34.2
012200 FD  SQ-FS8                                                       OBSQ34.2
012300     LABEL RECORDS ARE STANDARD                                   OBSQ34.2
012400     BLOCK 120 CHARACTERS                                         OBSQ34.2
012500     RECORD 120.                                                  OBSQ34.2
012600 01  SQ-FS8R1-F-G-120   PIC X(120).                               OBSQ34.2
012700 WORKING-STORAGE SECTION.                                         OBSQ34.2
012800 01  COUNT-OF-RECS PIC 9999.                                      OBSQ34.2
012900 01  FILE-RECORD-INFORMATION-REC.                                 OBSQ34.2
013000     03 FILE-RECORD-INFO-SKELETON.                                OBSQ34.2
013100        05 FILLER                 PICTURE X(48)       VALUE       OBSQ34.2
013200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  OBSQ34.2
013300        05 FILLER                 PICTURE X(46)       VALUE       OBSQ34.2
013400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    OBSQ34.2
013500        05 FILLER                 PICTURE X(26)       VALUE       OBSQ34.2
013600             ",LFIL=000000,ORG=  ,LBLR= ".                        OBSQ34.2
013700        05 FILLER                 PICTURE X(37)       VALUE       OBSQ34.2
013800             ",RECKEY=                             ".             OBSQ34.2
013900        05 FILLER                 PICTURE X(38)       VALUE       OBSQ34.2
014000             ",ALTKEY1=                             ".            OBSQ34.2
014100        05 FILLER                 PICTURE X(38)       VALUE       OBSQ34.2
014200             ",ALTKEY2=                             ".            OBSQ34.2
014300        05 FILLER                 PICTURE X(7)        VALUE SPACE.OBSQ34.2
014400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              OBSQ34.2
014500        05 FILE-RECORD-INFO-P1-120.                               OBSQ34.2
014600           07 FILLER              PIC X(5).                       OBSQ34.2
014700           07 XFILE-NAME           PIC X(6).                      OBSQ34.2
014800           07 FILLER              PIC X(8).                       OBSQ34.2
014900           07 XRECORD-NAME         PIC X(6).                      OBSQ34.2
015000           07 FILLER              PIC X(1).                       OBSQ34.2
015100           07 REELUNIT-NUMBER     PIC 9(1).                       OBSQ34.2
015200           07 FILLER              PIC X(7).                       OBSQ34.2
015300           07 XRECORD-NUMBER       PIC 9(6).                      OBSQ34.2
015400           07 FILLER              PIC X(6).                       OBSQ34.2
015500           07 UPDATE-NUMBER       PIC 9(2).                       OBSQ34.2
015600           07 FILLER              PIC X(5).                       OBSQ34.2
015700           07 ODO-NUMBER          PIC 9(4).                       OBSQ34.2
015800           07 FILLER              PIC X(5).                       OBSQ34.2
015900           07 XPROGRAM-NAME        PIC X(5).                      OBSQ34.2
016000           07 FILLER              PIC X(7).                       OBSQ34.2
016100           07 XRECORD-LENGTH       PIC 9(6).                      OBSQ34.2
016200           07 FILLER              PIC X(7).                       OBSQ34.2
016300           07 CHARS-OR-RECORDS    PIC X(2).                       OBSQ34.2
016400           07 FILLER              PIC X(1).                       OBSQ34.2
016500           07 XBLOCK-SIZE          PIC 9(4).                      OBSQ34.2
016600           07 FILLER              PIC X(6).                       OBSQ34.2
016700           07 RECORDS-IN-FILE     PIC 9(6).                       OBSQ34.2
016800           07 FILLER              PIC X(5).                       OBSQ34.2
016900           07 XFILE-ORGANIZATION   PIC X(2).                      OBSQ34.2
017000           07 FILLER              PIC X(6).                       OBSQ34.2
017100           07 XLABEL-TYPE          PIC X(1).                      OBSQ34.2
017200        05 FILE-RECORD-INFO-P121-240.                             OBSQ34.2
017300           07 FILLER              PIC X(8).                       OBSQ34.2
017400           07 XRECORD-KEY          PIC X(29).                     OBSQ34.2
017500           07 FILLER              PIC X(9).                       OBSQ34.2
017600           07 ALTERNATE-KEY1      PIC X(29).                      OBSQ34.2
017700           07 FILLER              PIC X(9).                       OBSQ34.2
017800           07 ALTERNATE-KEY2      PIC X(29).                      OBSQ34.2
017900           07 FILLER              PIC X(7).                       OBSQ34.2
018000 01  TEST-RESULTS.                                                OBSQ34.2
018100     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ34.2
018200     02 FEATURE                   PICTURE X(20) VALUE SPACE.      OBSQ34.2
018300     02 FILLER                    PICTURE X VALUE SPACE.          OBSQ34.2
018400     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       OBSQ34.2
018500     02 FILLER                    PICTURE X  VALUE SPACE.         OBSQ34.2
018600     02  PAR-NAME.                                                OBSQ34.2
018700       03 FILLER PICTURE X(12) VALUE SPACE.                       OBSQ34.2
018800       03  PARDOT-X PICTURE X  VALUE SPACE.                       OBSQ34.2
018900       03 DOTVALUE PICTURE 99  VALUE ZERO.                        OBSQ34.2
019000       03 FILLER PIC X(5) VALUE SPACE.                            OBSQ34.2
019100     02 FILLER PIC X(10) VALUE SPACE.                             OBSQ34.2
019200     02 RE-MARK PIC X(61).                                        OBSQ34.2
019300 01  TEST-COMPUTED.                                               OBSQ34.2
019400     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ34.2
019500     02 FILLER PIC X(17) VALUE "       COMPUTED=".                OBSQ34.2
019600     02 COMPUTED-X.                                               OBSQ34.2
019700     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      OBSQ34.2
019800     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       OBSQ34.2
019900     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      OBSQ34.2
020000     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  OBSQ34.2
020100     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  OBSQ34.2
020200     03       CM-18V0 REDEFINES COMPUTED-A.                       OBSQ34.2
020300         04 COMPUTED-18V0                   PICTURE -9(18).       OBSQ34.2
020400         04 FILLER                          PICTURE X.            OBSQ34.2
020500     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ34.2
020600 01  TEST-CORRECT.                                                OBSQ34.2
020700     02 FILLER PIC X(30) VALUE SPACE.                             OBSQ34.2
020800     02 FILLER PIC X(17) VALUE "       CORRECT =".                OBSQ34.2
020900     02 CORRECT-X.                                                OBSQ34.2
021000     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      OBSQ34.2
021100     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         OBSQ34.2
021200     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      OBSQ34.2
021300     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  OBSQ34.2
021400     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  OBSQ34.2
021500     03      CR-18V0 REDEFINES CORRECT-A.                         OBSQ34.2
021600         04 CORRECT-18V0                    PICTURE -9(18).       OBSQ34.2
021700         04 FILLER                          PICTURE X.            OBSQ34.2
021800     03 FILLER PIC X(50) VALUE SPACE.                             OBSQ34.2
021900 01  CCVS-C-1.                                                    OBSQ34.2
022000     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PAOBSQ34.2
022100-    "SS  PARAGRAPH-NAME                                          OBSQ34.2
022200-    "        REMARKS".                                           OBSQ34.2
022300     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   OBSQ34.2
022400 01  CCVS-C-2.                                                    OBSQ34.2
022500     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ34.2
022600     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 OBSQ34.2
022700     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   OBSQ34.2
022800     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   OBSQ34.2
022900     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   OBSQ34.2
023000 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         OBSQ34.2
023100 01  REC-CT PICTURE 99 VALUE ZERO.                                OBSQ34.2
023200 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        OBSQ34.2
023300 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  OBSQ34.2
023400 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          OBSQ34.2
023500 01  PASS-COUNTER PIC 999 VALUE ZERO.                             OBSQ34.2
023600 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              OBSQ34.2
023700 01  ERROR-HOLD PIC 999 VALUE ZERO.                               OBSQ34.2
023800 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           OBSQ34.2
023900 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            OBSQ34.2
024000 01  CCVS-H-1.                                                    OBSQ34.2
024100     02  FILLER   PICTURE X(27)  VALUE SPACE.                     OBSQ34.2
024200     02 FILLER PICTURE X(67) VALUE                                OBSQ34.2
024300     " FEDERAL SOFTWARE TESTING CENTER COBOL COMPILER VALIDATION  OBSQ34.2
024400-    " SYSTEM".                                                   OBSQ34.2
024500     02  FILLER     PICTURE X(26)  VALUE SPACE.                   OBSQ34.2
024600 01  CCVS-H-2.                                                    OBSQ34.2
024700     02 FILLER PICTURE X(52) VALUE IS                             OBSQ34.2
024800     "CCVS85 FSTC COPY, NOT FOR DISTRIBUTION.".                   OBSQ34.2
024900     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   OBSQ34.2
025000     02 TEST-ID PICTURE IS X(9).                                  OBSQ34.2
025100     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   OBSQ34.2
025200 01  CCVS-H-3.                                                    OBSQ34.2
025300     02  FILLER PICTURE X(34) VALUE                               OBSQ34.2
025400     " FOR OFFICIAL USE ONLY    ".                                OBSQ34.2
025500     02  FILLER PICTURE X(58) VALUE                               OBSQ34.2
025600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBSQ34.2
025700     02  FILLER PICTURE X(28) VALUE                               OBSQ34.2
025800     "  COPYRIGHT   1985 ".                                       OBSQ34.2
025900 01  CCVS-E-1.                                                    OBSQ34.2
026000     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   OBSQ34.2
026100     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        OBSQ34.2
026200     02 ID-AGAIN PICTURE IS X(9).                                 OBSQ34.2
026300     02 FILLER PICTURE X(45) VALUE IS                             OBSQ34.2
026400     " NTIS DISTRIBUTION COBOL 85".                               OBSQ34.2
026500 01  CCVS-E-2.                                                    OBSQ34.2
026600     02  FILLER                   PICTURE X(31)  VALUE            OBSQ34.2
026700     SPACE.                                                       OBSQ34.2
026800     02  FILLER                   PICTURE X(21)  VALUE SPACE.     OBSQ34.2
026900     02 CCVS-E-2-2.                                               OBSQ34.2
027000         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            OBSQ34.2
027100         03 FILLER PICTURE IS X VALUE IS SPACE.                   OBSQ34.2
027200         03 ENDER-DESC PIC X(46) VALUE "ERRORS ENCOUNTERED".      OBSQ34.2
027300 01  CCVS-E-3.                                                    OBSQ34.2
027400     02  FILLER PICTURE X(22) VALUE                               OBSQ34.2
027500     " FOR OFFICIAL USE ONLY".                                    OBSQ34.2
027600     02  FILLER PICTURE X(12) VALUE SPACE.                        OBSQ34.2
027700     02  FILLER PICTURE X(58) VALUE                               OBSQ34.2
027800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBSQ34.2
027900     02  FILLER PICTURE X(13) VALUE SPACE.                        OBSQ34.2
028000     02 FILLER PIC X(15) VALUE " COPYRIGHT 1985".                 OBSQ34.2
028100 01  CCVS-E-4.                                                    OBSQ34.2
028200     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           OBSQ34.2
028300     02 FILLER PIC XXXX VALUE " OF ".                             OBSQ34.2
028400     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           OBSQ34.2
028500     02 FILLER PIC X(40) VALUE                                    OBSQ34.2
028600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       OBSQ34.2
028700 01  XXINFO.                                                      OBSQ34.2
028800     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    OBSQ34.2
028900     02 INFO-TEXT.                                                OBSQ34.2
029000     04 FILLER PIC X(20) VALUE SPACE.                             OBSQ34.2
029100     04 XXCOMPUTED PIC X(20).                                     OBSQ34.2
029200     04 FILLER PIC X(5) VALUE SPACE.                              OBSQ34.2
029300     04 XXCORRECT PIC X(20).                                      OBSQ34.2
029400 01  HYPHEN-LINE.                                                 OBSQ34.2
029500     02 FILLER PICTURE IS X VALUE IS SPACE.                       OBSQ34.2
029600     02 FILLER PICTURE IS X(65) VALUE IS "************************OBSQ34.2
029700-    "*****************************************".                 OBSQ34.2
029800     02 FILLER PICTURE IS X(54) VALUE IS "************************OBSQ34.2
029900-    "******************************".                            OBSQ34.2
030000 01  CCVS-PGM-ID PIC X(6) VALUE                                   OBSQ34.2
030100     "OBSQ3A".                                                    OBSQ34.2
030200 PROCEDURE DIVISION.                                              OBSQ34.2
030300 CCVS1 SECTION.                                                   OBSQ34.2
030400 OPEN-FILES.                                                      OBSQ34.2
030500*    OPEN I-O RAW-DATA.                                           OBSQ34.2
030600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ34.2
030700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     OBSQ34.2
030800*    MOVE "ABORTED " TO C-ABORT.                                  OBSQ34.2
030900*    ADD 1 TO C-NO-OF-TESTS.                                      OBSQ34.2
031000*    ACCEPT C-DATE  FROM DATE.                                    OBSQ34.2
031100*    ACCEPT C-TIME  FROM TIME.                                    OBSQ34.2
031200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             OBSQ34.2
031300*END-E-1.                                                         OBSQ34.2
031400*    CLOSE RAW-DATA.                                              OBSQ34.2
031500     OPEN     OUTPUT PRINT-FILE.                                  OBSQ34.2
031600     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   OBSQ34.2
031700     MOVE    SPACE TO TEST-RESULTS.                               OBSQ34.2
031800     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             OBSQ34.2
031900     MOVE ZERO TO REC-SKL-SUB.                                    OBSQ34.2
032000     PERFORM CCVS-INIT-FILE 9 TIMES.                              OBSQ34.2
032100 CCVS-INIT-FILE.                                                  OBSQ34.2
032200     ADD 1 TO REC-SKL-SUB.                                        OBSQ34.2
032300     MOVE FILE-RECORD-INFO-SKELETON TO                            OBSQ34.2
032400                  FILE-RECORD-INFO (REC-SKL-SUB).                 OBSQ34.2
032500 CCVS-INIT-EXIT.                                                  OBSQ34.2
032600     GO TO CCVS1-EXIT.                                            OBSQ34.2
032700 CLOSE-FILES.                                                     OBSQ34.2
032800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   OBSQ34.2
032900*    OPEN I-O RAW-DATA.                                           OBSQ34.2
033000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            OBSQ34.2
033100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     OBSQ34.2
033200*    MOVE "OK.     " TO C-ABORT.                                  OBSQ34.2
033300*    MOVE PASS-COUNTER TO C-OK.                                   OBSQ34.2
033400*    MOVE ERROR-HOLD   TO C-ALL.                                  OBSQ34.2
033500*    MOVE ERROR-COUNTER TO C-FAIL.                                OBSQ34.2
033600*    MOVE DELETE-CNT TO C-DELETED.                                OBSQ34.2
033700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           OBSQ34.2
033800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             OBSQ34.2
033900*END-E-2.                                                         OBSQ34.2
034000*    CLOSE RAW-DATA.                                              OBSQ34.2
034100 TERMINATE-CCVS.                                                  OBSQ34.2
034200*    EXIT PROGRAM.                                                OBSQ34.2
034300*TERMINATE-CALL.                                                  OBSQ34.2
034400     STOP     RUN.                                                OBSQ34.2
034500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         OBSQ34.2
034600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           OBSQ34.2
034700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          OBSQ34.2
034800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          OBSQ34.2
034900     MOVE "****TEST DELETED****" TO RE-MARK.                      OBSQ34.2
035000 PRINT-DETAIL.                                                    OBSQ34.2
035100     IF REC-CT NOT EQUAL TO ZERO                                  OBSQ34.2
035200             MOVE "." TO PARDOT-X                                 OBSQ34.2
035300             MOVE REC-CT TO DOTVALUE.                             OBSQ34.2
035400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      OBSQ34.2
035500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               OBSQ34.2
035600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 OBSQ34.2
035700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 OBSQ34.2
035800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              OBSQ34.2
035900     MOVE SPACE TO CORRECT-X.                                     OBSQ34.2
036000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         OBSQ34.2
036100     MOVE     SPACE TO RE-MARK.                                   OBSQ34.2
036200 HEAD-ROUTINE.                                                    OBSQ34.2
036300     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ34.2
036400     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   OBSQ34.2
036500     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   OBSQ34.2
036600 COLUMN-NAMES-ROUTINE.                                            OBSQ34.2
036700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ34.2
036800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ34.2
036900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        OBSQ34.2
037000 END-ROUTINE.                                                     OBSQ34.2
037100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.OBSQ34.2
037200 END-RTN-EXIT.                                                    OBSQ34.2
037300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   OBSQ34.2
037400 END-ROUTINE-1.                                                   OBSQ34.2
037500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      OBSQ34.2
037600      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   OBSQ34.2
037700      ADD PASS-COUNTER TO ERROR-HOLD.                             OBSQ34.2
037800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   OBSQ34.2
037900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            OBSQ34.2
038000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              OBSQ34.2
038100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                OBSQ34.2
038200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           OBSQ34.2
038300  END-ROUTINE-12.                                                 OBSQ34.2
038400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        OBSQ34.2
038500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      OBSQ34.2
038600         MOVE "NO " TO ERROR-TOTAL                                OBSQ34.2
038700         ELSE                                                     OBSQ34.2
038800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       OBSQ34.2
038900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           OBSQ34.2
039000     PERFORM WRITE-LINE.                                          OBSQ34.2
039100 END-ROUTINE-13.                                                  OBSQ34.2
039200     IF DELETE-CNT IS EQUAL TO ZERO                               OBSQ34.2
039300         MOVE "NO " TO ERROR-TOTAL  ELSE                          OBSQ34.2
039400         MOVE DELETE-CNT TO ERROR-TOTAL.                          OBSQ34.2
039500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   OBSQ34.2
039600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ34.2
039700      IF   INSPECT-COUNTER EQUAL TO ZERO                          OBSQ34.2
039800          MOVE "NO " TO ERROR-TOTAL                               OBSQ34.2
039900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   OBSQ34.2
040000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            OBSQ34.2
040100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          OBSQ34.2
040200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           OBSQ34.2
040300 WRITE-LINE.                                                      OBSQ34.2
040400     ADD 1 TO RECORD-COUNT.                                       OBSQ34.2
040500*    IF RECORD-COUNT GREATER 50                                   OBSQ34.2
040600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          OBSQ34.2
040700*        MOVE SPACE TO DUMMY-RECORD                               OBSQ34.2
040800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  OBSQ34.2
040900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             OBSQ34.2
041000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     OBSQ34.2
041100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          OBSQ34.2
041200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          OBSQ34.2
041300*        MOVE ZERO TO RECORD-COUNT.                               OBSQ34.2
041400     PERFORM WRT-LN.                                              OBSQ34.2
041500 WRT-LN.                                                          OBSQ34.2
041600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               OBSQ34.2
041700     MOVE SPACE TO DUMMY-RECORD.                                  OBSQ34.2
041800 BLANK-LINE-PRINT.                                                OBSQ34.2
041900     PERFORM WRT-LN.                                              OBSQ34.2
042000 FAIL-ROUTINE.                                                    OBSQ34.2
042100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   OBSQ34.2
042200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    OBSQ34.2
042300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    OBSQ34.2
042400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ34.2
042500     GO TO FAIL-ROUTINE-EX.                                       OBSQ34.2
042600 FAIL-ROUTINE-WRITE.                                              OBSQ34.2
042700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           OBSQ34.2
042800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   OBSQ34.2
042900 FAIL-ROUTINE-EX. EXIT.                                           OBSQ34.2
043000 BAIL-OUT.                                                        OBSQ34.2
043100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       OBSQ34.2
043200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               OBSQ34.2
043300 BAIL-OUT-WRITE.                                                  OBSQ34.2
043400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  OBSQ34.2
043500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     OBSQ34.2
043600 BAIL-OUT-EX. EXIT.                                               OBSQ34.2
043700 CCVS1-EXIT.                                                      OBSQ34.2
043800     EXIT.                                                        OBSQ34.2
043900 SECT-OBSQ3A-0001 SECTION.                                        OBSQ34.2
044000 SEQ-INIT-001.                                                    OBSQ34.2
044100*             THIS TEST CREATES FILE SQ-FS1 AS THE FIRST FILE     OBSQ34.2
044200*             ON MULTIPLE FILE TAPE ONE.  THIS FILE IS CLOSED     OBSQ34.2
044300*             WITH NO REWIND.                                     OBSQ34.2
044400     PERFORM BUILD-RECORD.                                        OBSQ34.2
044500     MOVE "SQ-FS1" TO XFILE-NAME (1).                             OBSQ34.2
044600     MOVE "RC"    TO CHARS-OR-RECORDS (1).                        OBSQ34.2
044700     MOVE 1        TO XBLOCK-SIZE (1).                            OBSQ34.2
044800     OPEN OUTPUT SQ-FS1.                                          OBSQ34.2
044900 SEQ-TEST-001.                                                    OBSQ34.2
045000     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS1R1-F-G-120.        OBSQ34.2
045100     WRITE SQ-FS1R1-F-G-120.                                      OBSQ34.2
045200     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ34.2
045300              GO TO SEQ-WRITE-001.                                OBSQ34.2
045400     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
045500     GO TO SEQ-TEST-001.                                          OBSQ34.2
045600 SEQ-WRITE-001.                                                   OBSQ34.2
045700     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        OBSQ34.2
045800     MOVE "SEQ-TEST-001" TO PAR-NAME.                             OBSQ34.2
045900     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   OBSQ34.2
046000     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
046100     PERFORM PRINT-DETAIL.                                        OBSQ34.2
046200 SEQ-CLOSE-001.                                                   OBSQ34.2
046300     CLOSE SQ-FS1 WITH NO REWIND.                                 OBSQ34.2
046400 SEQ-INIT-002.                                                    OBSQ34.2
046500*             THIS TEST CREATES FILE SQ-FS2 AS THE SECOND FILE    OBSQ34.2
046600*             ON MULTIPLE FILE TAPE ONE.  THIS FILE IS OPENED     OBSQ34.2
046700*             AND CLOSED WITH NO REWIND.                          OBSQ34.2
046800     PERFORM BUILD-RECORD.                                        OBSQ34.2
046900     MOVE "SQ-FS2" TO XFILE-NAME (1).                             OBSQ34.2
047000     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       OBSQ34.2
047100     MOVE 5        TO XBLOCK-SIZE (1).                            OBSQ34.2
047200     OPEN OUTPUT SQ-FS2 WITH NO REWIND.                           OBSQ34.2
047300 SEQ-TEST-002.                                                    OBSQ34.2
047400     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS2R1-F-G-120.        OBSQ34.2
047500     WRITE SQ-FS2R1-F-G-120.                                      OBSQ34.2
047600     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ34.2
047700              GO TO SEQ-WRITE-002.                                OBSQ34.2
047800     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
047900     GO TO SEQ-TEST-002.                                          OBSQ34.2
048000 SEQ-WRITE-002.                                                   OBSQ34.2
048100     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        OBSQ34.2
048200     MOVE "SEQ-TEST-002" TO PAR-NAME.                             OBSQ34.2
048300     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   OBSQ34.2
048400     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
048500     PERFORM PRINT-DETAIL.                                        OBSQ34.2
048600 SEQ-CLOSE-002.                                                   OBSQ34.2
048700     CLOSE SQ-FS2 WITH NO REWIND.                                 OBSQ34.2
048800 SEQ-INIT-003.                                                    OBSQ34.2
048900*             THIS TEST CREATES FILE SQ-FS3 AS THE THIRD FILE     OBSQ34.2
049000*             ON MULTIPLE FILE TAPE ONE.  THIS FILE IS OPENED     OBSQ34.2
049100*             AND CLOSED WITH NO REWIND.                          OBSQ34.2
049200     PERFORM BUILD-RECORD.                                        OBSQ34.2
049300     MOVE "SQ-FS3" TO XFILE-NAME (1).                             OBSQ34.2
049400     MOVE "CH"     TO CHARS-OR-RECORDS (1).                       OBSQ34.2
049500     MOVE 1200     TO XBLOCK-SIZE (1).                            OBSQ34.2
049600     OPEN OUTPUT SQ-FS3 NO REWIND.                                OBSQ34.2
049700 SEQ-TEST-003.                                                    OBSQ34.2
049800     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS3R1-F-G-120.        OBSQ34.2
049900     WRITE SQ-FS3R1-F-G-120.                                      OBSQ34.2
050000     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ34.2
050100              GO TO SEQ-WRITE-003.                                OBSQ34.2
050200     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
050300     GO TO SEQ-TEST-003.                                          OBSQ34.2
050400 SEQ-WRITE-003.                                                   OBSQ34.2
050500     MOVE "CREATE FILE SQ-FS3" TO FEATURE.                        OBSQ34.2
050600     MOVE "SEQ-TEST-003" TO PAR-NAME.                             OBSQ34.2
050700     MOVE "FILE CREATED, RECS=" TO COMPUTED-A.                    OBSQ34.2
050800     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
050900     PERFORM PRINT-DETAIL.                                        OBSQ34.2
051000 SEQ-CLOSE-003.                                                   OBSQ34.2
051100     CLOSE SQ-FS3 WITH NO REWIND.                                 OBSQ34.2
051200 SEQ-INIT-004.                                                    OBSQ34.2
051300*             THIS TEST CREATES FILE SQ-FS4 AS THE FOURTH AND LASTOBSQ34.2
051400*             FILE ON MULTIPLE FILE TAPE ONE.  THIS FILE IS OPENEDOBSQ34.2
051500*             WITH NO REWIND.                                     OBSQ34.2
051600     PERFORM BUILD-RECORD.                                        OBSQ34.2
051700     MOVE "SQ-FS4" TO XFILE-NAME (1).                             OBSQ34.2
051800     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       OBSQ34.2
051900     MOVE 10       TO XBLOCK-SIZE (1).                            OBSQ34.2
052000     OPEN OUTPUT SQ-FS4 WITH NO REWIND.                           OBSQ34.2
052100 SEQ-TEST-004.                                                    OBSQ34.2
052200     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS4R1-F-G-120.        OBSQ34.2
052300     WRITE SQ-FS4R1-F-G-120.                                      OBSQ34.2
052400     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ34.2
052500              GO TO SEQ-WRITE-004.                                OBSQ34.2
052600     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
052700     GO TO SEQ-TEST-004.                                          OBSQ34.2
052800 SEQ-WRITE-004.                                                   OBSQ34.2
052900     MOVE "CREATE FILE SQ-FS4" TO FEATURE.                        OBSQ34.2
053000     MOVE "SEQ-TEST-004" TO PAR-NAME.                             OBSQ34.2
053100     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   OBSQ34.2
053200     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
053300     PERFORM PRINT-DETAIL.                                        OBSQ34.2
053400 SEQ-CLOSE-004.                                                   OBSQ34.2
053500     CLOSE SQ-FS4.                                                OBSQ34.2
053600 SEQ-INIT-005.                                                    OBSQ34.2
053700*             THIS TEST CREATES FILE SQ-FS5 AS THE FIRST FILE ON  OBSQ34.2
053800*             MULTIPLE FILE TAPE TWO.  THE POSITION PHRASE IS     OBSQ34.2
053900*             USED IN THE MULTIPLE FILE CLAUSE.                   OBSQ34.2
054000     PERFORM BUILD-RECORD.                                        OBSQ34.2
054100     MOVE "SQ-FS5" TO XFILE-NAME (1).                             OBSQ34.2
054200     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       OBSQ34.2
054300     MOVE 5        TO XBLOCK-SIZE (1).                            OBSQ34.2
054400     OPEN OUTPUT SQ-FS5.                                          OBSQ34.2
054500 SEQ-TEST-005.                                                    OBSQ34.2
054600     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS5R1-F-G-120.        OBSQ34.2
054700     WRITE SQ-FS5R1-F-G-120.                                      OBSQ34.2
054800     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ34.2
054900              GO TO SEQ-WRITE-005.                                OBSQ34.2
055000     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
055100     GO TO SEQ-TEST-005.                                          OBSQ34.2
055200 SEQ-WRITE-005.                                                   OBSQ34.2
055300     MOVE "CREATE FILE SQ-FS5" TO FEATURE.                        OBSQ34.2
055400     MOVE "SEQ-TEST-005" TO PAR-NAME.                             OBSQ34.2
055500     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   OBSQ34.2
055600     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
055700     PERFORM PRINT-DETAIL.                                        OBSQ34.2
055800 SEQ-CLOSE-005.                                                   OBSQ34.2
055900     CLOSE SQ-FS5.                                                OBSQ34.2
056000 SEQ-INIT-006.                                                    OBSQ34.2
056100*             THIS  TEST CREATES FILE SQ-FS6 AS THE SECOND FILE   OBSQ34.2
056200*             ON MULTIPLE FILE TAPE TWO.  THE POSITION PHRASE IS  OBSQ34.2
056300*             USED IN THE MULTIPLE FILE CLAUSE.                   OBSQ34.2
056400     PERFORM BUILD-RECORD.                                        OBSQ34.2
056500     MOVE "SQ-FS6" TO XFILE-NAME (1).                             OBSQ34.2
056600     MOVE "RC"     TO CHARS-OR-RECORDS (1).                       OBSQ34.2
056700     MOVE 10       TO XBLOCK-SIZE (1).                            OBSQ34.2
056800     OPEN OUTPUT SQ-FS6.                                          OBSQ34.2
056900 SEQ-TEST-006.                                                    OBSQ34.2
057000     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS6R1-F-G-120.        OBSQ34.2
057100     WRITE SQ-FS6R1-F-G-120.                                      OBSQ34.2
057200     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ34.2
057300              GO TO SEQ-WRITE-006.                                OBSQ34.2
057400     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
057500     GO TO SEQ-TEST-006.                                          OBSQ34.2
057600 SEQ-WRITE-006.                                                   OBSQ34.2
057700     MOVE "CREATE FILE SQ-FS6" TO FEATURE.                        OBSQ34.2
057800     MOVE "SEQ-TEST-006" TO PAR-NAME.                             OBSQ34.2
057900     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   OBSQ34.2
058000     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
058100     PERFORM PRINT-DETAIL.                                        OBSQ34.2
058200 SEQ-CLOSE-006.                                                   OBSQ34.2
058300     CLOSE SQ-FS6.                                                OBSQ34.2
058400 SEQ-INIT-007.                                                    OBSQ34.2
058500*             THIS TEST CREATES FILE SQ-FS7 AS THE THIRD FILE     OBSQ34.2
058600*             ON MULTIPLE FILE TAPE TWO.  THE POSITION PHRASE IS  OBSQ34.2
058700*             USED IN THE MULTIPLE FILE CLAUSE.                   OBSQ34.2
058800     PERFORM BUILD-RECORD.                                        OBSQ34.2
058900     MOVE "SQ-FS7" TO XFILE-NAME (1).                             OBSQ34.2
059000     MOVE "CH"     TO CHARS-OR-RECORDS (1).                       OBSQ34.2
059100     MOVE 2400     TO XBLOCK-SIZE (1).                            OBSQ34.2
059200     OPEN OUTPUT SQ-FS7.                                          OBSQ34.2
059300 SEQ-TEST-007.                                                    OBSQ34.2
059400     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS7R1-F-G-120.        OBSQ34.2
059500     WRITE SQ-FS7R1-F-G-120.                                      OBSQ34.2
059600     IF XRECORD-NUMBER (1) EQUAL TO 750                           OBSQ34.2
059700              GO TO SEQ-WRITE-007.                                OBSQ34.2
059800     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
059900     GO TO SEQ-TEST-007.                                          OBSQ34.2
060000 SEQ-WRITE-007.                                                   OBSQ34.2
060100     MOVE "CREATE FILE SQ-FS7" TO FEATURE.                        OBSQ34.2
060200     MOVE "SEQ-TEST-007" TO PAR-NAME.                             OBSQ34.2
060300     MOVE "FILE CREATED, RECS-=" TO COMPUTED-A.                   OBSQ34.2
060400     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
060500     PERFORM PRINT-DETAIL.                                        OBSQ34.2
060600 SEQ-CLOSE-007.                                                   OBSQ34.2
060700     CLOSE SQ-FS7.                                                OBSQ34.2
060800 SEQ-INIT-008.                                                    OBSQ34.2
060900*             THIS TEST CREATES FILE SQ-FS8 AS THE FOURTH AND LASTOBSQ34.2
061000*             FILE ON MULTIPLE FILE TAPE TWO.  THE POSITION PHRASEOBSQ34.2
061100*             IS USED IN THE MULTIPLE FILE CLAUSE.                OBSQ34.2
061200     PERFORM BUILD-RECORD.                                        OBSQ34.2
061300     MOVE "SQ-FS8" TO XFILE-NAME (1).                             OBSQ34.2
061400     MOVE "CH"     TO CHARS-OR-RECORDS (1).                       OBSQ34.2
061500     MOVE 120      TO XBLOCK-SIZE (1).                            OBSQ34.2
061600     OPEN OUTPUT SQ-FS8.                                          OBSQ34.2
061700 SEQ-TEST-008.                                                    OBSQ34.2
061800     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS8R1-F-G-120.        OBSQ34.2
061900     WRITE SQ-FS8R1-F-G-120.                                      OBSQ34.2
062000     IF XRECORD-NUMBER (1) EQUAL 750                              OBSQ34.2
062100              GO TO SEQ-WRITE-008.                                OBSQ34.2
062200     ADD 1 TO XRECORD-NUMBER (1).                                 OBSQ34.2
062300     GO TO SEQ-TEST-008.                                          OBSQ34.2
062400 SEQ-WRITE-008.                                                   OBSQ34.2
062500     MOVE "CREATE FILE SQ-FS8" TO FEATURE.                        OBSQ34.2
062600     MOVE "SEQ-TEST-008" TO PAR-NAME.                             OBSQ34.2
062700     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   OBSQ34.2
062800     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     OBSQ34.2
062900     PERFORM PRINT-DETAIL.                                        OBSQ34.2
063000 SEQ-CLOSE-008.                                                   OBSQ34.2
063100     CLOSE SQ-FS8.                                                OBSQ34.2
063200 OBSQ3A-END-ROUTINE.                                              OBSQ34.2
063300     MOVE "END OF OBSQ3A VALIDATION TESTS" TO PRINT-REC.          OBSQ34.2
063400     WRITE PRINT-REC AFTER ADVANCING 1 LINE.                      OBSQ34.2
063500 TERMINATE-OBSQ3A.                                                OBSQ34.2
063600     GO TO CCVS-EXIT.                                             OBSQ34.2
063700 BUILD-RECORD.                                                    OBSQ34.2
063800     MOVE "R1-F-G" TO XRECORD-NAME (1).                           OBSQ34.2
063900     MOVE "OBSQ3A" TO XPROGRAM-NAME (1).                          OBSQ34.2
064000     MOVE 120      TO XRECORD-LENGTH (1).                         OBSQ34.2
064100     MOVE 750      TO RECORDS-IN-FILE (1).                        OBSQ34.2
064200     MOVE "SQ"     TO XFILE-ORGANIZATION (1).                     OBSQ34.2
064300     MOVE "S"      TO XLABEL-TYPE (1).                            OBSQ34.2
064400     MOVE 1        TO XRECORD-NUMBER (1).                         OBSQ34.2
064500 CCVS-EXIT SECTION.                                               OBSQ34.2
064600 CCVS-999999.                                                     OBSQ34.2
064700     GO TO CLOSE-FILES.                                           OBSQ34.2
      *END-OF,OBSQ3A                                                                  
