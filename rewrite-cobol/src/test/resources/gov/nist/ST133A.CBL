      *HEADER,COBOL,ST133A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1334.2
000200 PROGRAM-ID.                                                      ST1334.2
000300     ST133A.                                                      ST1334.2
000400****************************************************************  ST1334.2
000500*                                                              *  ST1334.2
000600*    VALIDATION FOR:-                                          *  ST1334.2
000700*                                                              *  ST1334.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1334.2
000900*                                                              *  ST1334.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1334.2
001100*                                                              *  ST1334.2
001200****************************************************************  ST1334.2
001300*                                                              *  ST1334.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1334.2
001500*                                                              *  ST1334.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1334.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1334.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1334.2
001900*                                                              *  ST1334.2
002000****************************************************************  ST1334.2
002100*                                                                 ST1334.2
002200*    ST133 DOES TWO SORTS ON THE SAME SET OF 80-CHARACTER RECORDS.ST1334.2
002300*    THESE RECORDS ARE SHOWN BELOW. THE FIRST SORT EMPLOYS THE    ST1334.2
002400*    COMBINATION INPUT PROCEDURE, GIVING. THE SECOND SORT EMPLOYS ST1334.2
002500*    USING, OUTPUT PROCEDURE. THE OUTPUT FILE FROM THE FIRST SORT ST1334.2
002600*    BECOMES THE INPUT FILE FOR THE SECOND SORT.                  ST1334.2
002700*                                                                 ST1334.2
002800*    THE RESULTS OF BOTH SORTS ARE REFLECTED IN THE INPUT AND     ST1334.2
002900*    OUTPUT FILES ASSOCIATED WITH THE SECOND SORT. BECAUSE OF     ST1334.2
003000*    THIS, THE CONTENTS OF BOTH FILES ARE SPOT-CHECKED IN THE     ST1334.2
003100*    OUTPUT PROCEDURE. TO DO THIS, IT IS NECESSARY TO REFER TO    ST1334.2
003200*    BOTH FILES IN THE OUTPUT PROCEDURE. IN ADDITION, BOTH FILES  ST1334.2
003300*    ARE REFERENCED IN THE MAINLINE BETWEEN THE SORTS.            ST1334.2
003400*      BEFORE SORTS        AFTER FIRST SORT    AFTER SECOND SORT  ST1334.2
003500*      NON-KEY KEY         NON-KEY KEY         NON-KEY KEY        ST1334.2
003600*      X(72)   S9(8)       X(72)   S9(8)       X(72)   S9(8)      ST1334.2
003700*                                                                 ST1334.2
003800*      A        00000000   A       +00000099   A       -00000199  ST1334.2
003900*      A       +00000001   A       +00000098   A       -00000198  ST1334.2
004000*      A       +00000002   A       +00000097   A       -00000197  ST1334.2
004100*        .         .         .         .         .         .      ST1334.2
004200*        .         .         .         .         .         .      ST1334.2
004300*        .         .         .         .         .         .      ST1334.2
004400*      A       +00000098   A       +00000001   A       -00000101  ST1334.2
004500*      A       +00000099   A        00000000   A       -00000100  ST1334.2
004600*      A       -00000100   A       -00000100   A        00000000  ST1334.2
004700*      A       -00000101   A       -00000101   A       +00000001  ST1334.2
004800*        .         .         .         .         .         .      ST1334.2
004900*        .         .         .         .         .         .      ST1334.2
005000*        .         .         .         .         .         .      ST1334.2
005100*      A       -00000199   A       -00000199   A       +00000099  ST1334.2
005200*    THE NON-KEY ITEMS ARE ALL JUSTIFIED RIGHT.                   ST1334.2
005300*                                                                 ST1334.2
005400*    THE SAME SORT AREA CLAUSE IS EXERCISED IN THIS PROGRAM.      ST1334.2
005500*                                                                 ST1334.2
005600 ENVIRONMENT DIVISION.                                            ST1334.2
005700 CONFIGURATION SECTION.                                           ST1334.2
005800 SOURCE-COMPUTER.                                                 ST1334.2
005900     XXXXX082.                                                    ST1334.2
006000 OBJECT-COMPUTER.                                                 ST1334.2
006100     XXXXX083.                                                    ST1334.2
006200 INPUT-OUTPUT SECTION.                                            ST1334.2
006300 FILE-CONTROL.                                                    ST1334.2
006400     SELECT PRINT-FILE ASSIGN TO                                  ST1334.2
006500     XXXXX055.                                                    ST1334.2
006600     SELECT   FIRST-SORTFILE ASSIGN  TO                           ST1334.2
006700     XXXXX027.                                                    ST1334.2
006800     SELECT   SECOND-SORTFILE ASSIGN TO                           ST1334.2
006900     XXXXX028.                                                    ST1334.2
007000     SELECT   SORTIN-2C ASSIGN TO                                 ST1334.2
007100     XXXXX001.                                                    ST1334.2
007200     SELECT   SORTOUT-2C ASSIGN TO                                ST1334.2
007300     XXXXX002.                                                    ST1334.2
007400 I-O-CONTROL.                                                     ST1334.2
007500     SAME SORT AREA FOR FIRST-SORTFILE                            ST1334.2
007600                        SECOND-SORTFILE.                          ST1334.2
007700 DATA DIVISION.                                                   ST1334.2
007800 FILE SECTION.                                                    ST1334.2
007900 FD  PRINT-FILE.                                                  ST1334.2
008000 01  PRINT-REC PICTURE X(120).                                    ST1334.2
008100 01  DUMMY-RECORD PICTURE X(120).                                 ST1334.2
008200 FD  SORTIN-2C                                                    ST1334.2
008300     LABEL RECORDS STANDARD                                       ST1334.2
008400C    VALUE OF                                                     ST1334.2
008500C    XXXXX074                                                     ST1334.2
008600C    IS                                                           ST1334.2
008700C    XXXXX075                                                     ST1334.2
008800*    XXXXX069                                                     ST1334.2
008900     DATA RECORD IS SORTIN-REC.                                   ST1334.2
009000 01  SORTIN-REC.                                                  ST1334.2
009100     02 SORTIN-NON-KEY  PICTURE X(72) JUSTIFIED RIGHT.            ST1334.2
009200     02 SORTIN-KEY      PICTURE S9(8) COMPUTATIONAL.              ST1334.2
009300 FD  SORTOUT-2C                                                   ST1334.2
009400     LABEL RECORDS STANDARD                                       ST1334.2
009500C    VALUE OF                                                     ST1334.2
009600C    XXXXX074                                                     ST1334.2
009700C    IS                                                           ST1334.2
009800C    XXXXX076                                                     ST1334.2
009900*    XXXXX069                                                     ST1334.2
010000     DATA RECORD IS SORTOUT-REC.                                  ST1334.2
010100 01  SORTOUT-REC.                                                 ST1334.2
010200     02 SORTOUT-NON-KEY PICTURE X(72) JUSTIFIED RIGHT.            ST1334.2
010300     02 SORTOUT-KEY     PICTURE S9(8) COMPUTATIONAL.              ST1334.2
010400 SD  FIRST-SORTFILE                                               ST1334.2
010500     DATA RECORD IS FIRST-SORTFILE-REC.                           ST1334.2
010600 01  FIRST-SORTFILE-REC.                                          ST1334.2
010700     02 FIRST-NON-KEY   PICTURE X(72) JUSTIFIED RIGHT.            ST1334.2
010800     02 FIRST-KEY       PICTURE S9(8) USAGE IS COMPUTATIONAL.     ST1334.2
010900 SD  SECOND-SORTFILE.                                             ST1334.2
011000 01  SECOND-SORTFILE-REC.                                         ST1334.2
011100     02 SECOND-NON-KEY  PICTURE X(72) JUSTIFIED.                  ST1334.2
011200     02 SECOND-KEY      PICTURE S9(8) COMPUTATIONAL.              ST1334.2
011300 WORKING-STORAGE SECTION.                                         ST1334.2
011400 77  BREAKDOWN-SWITCH   PICTURE 9 VALUE ZERO.                     ST1334.2
011500 77  SP-ACE             PICTURE X(14) VALUE "      (SPACES)".     ST1334.2
011600 77  UTIL-CTR           PICTURE S99999.                           ST1334.2
011700 77  JUSTIFIED-A        PICTURE X(72) VALUE "                     ST1334.2
011800-    "                                                  A".       ST1334.2
011900 01  COMPUTED-BREAKDOWN.                                          ST1334.2
012000     02 FIRST-20        PICTURE X(20).                            ST1334.2
012100     02 SECOND-20       PICTURE X(20).                            ST1334.2
012200     02 THIRD-20        PICTURE X(20).                            ST1334.2
012300     02 FOURTH-20       PICTURE X(20).                            ST1334.2
012400 01  FILE-RECORD-INFORMATION-REC.                                 ST1334.2
012500     03 FILE-RECORD-INFO-SKELETON.                                ST1334.2
012600        05 FILLER                 PICTURE X(48)       VALUE       ST1334.2
012700             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1334.2
012800        05 FILLER                 PICTURE X(46)       VALUE       ST1334.2
012900             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1334.2
013000        05 FILLER                 PICTURE X(26)       VALUE       ST1334.2
013100             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1334.2
013200        05 FILLER                 PICTURE X(37)       VALUE       ST1334.2
013300             ",RECKEY=                             ".             ST1334.2
013400        05 FILLER                 PICTURE X(38)       VALUE       ST1334.2
013500             ",ALTKEY1=                             ".            ST1334.2
013600        05 FILLER                 PICTURE X(38)       VALUE       ST1334.2
013700             ",ALTKEY2=                             ".            ST1334.2
013800        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1334.2
013900     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1334.2
014000        05 FILE-RECORD-INFO-P1-120.                               ST1334.2
014100           07 FILLER              PIC X(5).                       ST1334.2
014200           07 XFILE-NAME           PIC X(6).                      ST1334.2
014300           07 FILLER              PIC X(8).                       ST1334.2
014400           07 XRECORD-NAME         PIC X(6).                      ST1334.2
014500           07 FILLER              PIC X(1).                       ST1334.2
014600           07 REELUNIT-NUMBER     PIC 9(1).                       ST1334.2
014700           07 FILLER              PIC X(7).                       ST1334.2
014800           07 XRECORD-NUMBER       PIC 9(6).                      ST1334.2
014900           07 FILLER              PIC X(6).                       ST1334.2
015000           07 UPDATE-NUMBER       PIC 9(2).                       ST1334.2
015100           07 FILLER              PIC X(5).                       ST1334.2
015200           07 ODO-NUMBER          PIC 9(4).                       ST1334.2
015300           07 FILLER              PIC X(5).                       ST1334.2
015400           07 XPROGRAM-NAME        PIC X(5).                      ST1334.2
015500           07 FILLER              PIC X(7).                       ST1334.2
015600           07 XRECORD-LENGTH       PIC 9(6).                      ST1334.2
015700           07 FILLER              PIC X(7).                       ST1334.2
015800           07 CHARS-OR-RECORDS    PIC X(2).                       ST1334.2
015900           07 FILLER              PIC X(1).                       ST1334.2
016000           07 XBLOCK-SIZE          PIC 9(4).                      ST1334.2
016100           07 FILLER              PIC X(6).                       ST1334.2
016200           07 RECORDS-IN-FILE     PIC 9(6).                       ST1334.2
016300           07 FILLER              PIC X(5).                       ST1334.2
016400           07 XFILE-ORGANIZATION   PIC X(2).                      ST1334.2
016500           07 FILLER              PIC X(6).                       ST1334.2
016600           07 XLABEL-TYPE          PIC X(1).                      ST1334.2
016700        05 FILE-RECORD-INFO-P121-240.                             ST1334.2
016800           07 FILLER              PIC X(8).                       ST1334.2
016900           07 XRECORD-KEY          PIC X(29).                     ST1334.2
017000           07 FILLER              PIC X(9).                       ST1334.2
017100           07 ALTERNATE-KEY1      PIC X(29).                      ST1334.2
017200           07 FILLER              PIC X(9).                       ST1334.2
017300           07 ALTERNATE-KEY2      PIC X(29).                      ST1334.2
017400           07 FILLER              PIC X(7).                       ST1334.2
017500 01  TEST-RESULTS.                                                ST1334.2
017600     02 FILLER                   PIC X      VALUE SPACE.          ST1334.2
017700     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1334.2
017800     02 FILLER                   PIC X      VALUE SPACE.          ST1334.2
017900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1334.2
018000     02 FILLER                   PIC X      VALUE SPACE.          ST1334.2
018100     02  PAR-NAME.                                                ST1334.2
018200       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1334.2
018300       03  PARDOT-X              PIC X      VALUE SPACE.          ST1334.2
018400       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1334.2
018500     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1334.2
018600     02 RE-MARK                  PIC X(61).                       ST1334.2
018700 01  TEST-COMPUTED.                                               ST1334.2
018800     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1334.2
018900     02 FILLER                   PIC X(17)  VALUE                 ST1334.2
019000            "       COMPUTED=".                                   ST1334.2
019100     02 COMPUTED-X.                                               ST1334.2
019200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1334.2
019300     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1334.2
019400                                 PIC -9(9).9(9).                  ST1334.2
019500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1334.2
019600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1334.2
019700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1334.2
019800     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1334.2
019900         04 COMPUTED-18V0                    PIC -9(18).          ST1334.2
020000         04 FILLER                           PIC X.               ST1334.2
020100     03 FILLER PIC X(50) VALUE SPACE.                             ST1334.2
020200 01  TEST-CORRECT.                                                ST1334.2
020300     02 FILLER PIC X(30) VALUE SPACE.                             ST1334.2
020400     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1334.2
020500     02 CORRECT-X.                                                ST1334.2
020600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1334.2
020700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1334.2
020800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1334.2
020900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1334.2
021000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1334.2
021100     03      CR-18V0 REDEFINES CORRECT-A.                         ST1334.2
021200         04 CORRECT-18V0                     PIC -9(18).          ST1334.2
021300         04 FILLER                           PIC X.               ST1334.2
021400     03 FILLER PIC X(2) VALUE SPACE.                              ST1334.2
021500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1334.2
021600 01  CCVS-C-1.                                                    ST1334.2
021700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1334.2
021800-    "SS  PARAGRAPH-NAME                                          ST1334.2
021900-    "       REMARKS".                                            ST1334.2
022000     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1334.2
022100 01  CCVS-C-2.                                                    ST1334.2
022200     02 FILLER                     PIC X        VALUE SPACE.      ST1334.2
022300     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1334.2
022400     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1334.2
022500     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1334.2
022600     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1334.2
022700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1334.2
022800 01  REC-CT                        PIC 99       VALUE ZERO.       ST1334.2
022900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1334.2
023000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1334.2
023100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1334.2
023200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1334.2
023300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1334.2
023400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1334.2
023500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1334.2
023600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1334.2
023700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1334.2
023800 01  CCVS-H-1.                                                    ST1334.2
023900     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1334.2
024000     02  FILLER                    PIC X(42)    VALUE             ST1334.2
024100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1334.2
024200     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1334.2
024300 01  CCVS-H-2A.                                                   ST1334.2
024400   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1334.2
024500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1334.2
024600   02  FILLER                        PIC XXXX   VALUE             ST1334.2
024700     "4.2 ".                                                      ST1334.2
024800   02  FILLER                        PIC X(28)  VALUE             ST1334.2
024900            " COPY - NOT FOR DISTRIBUTION".                       ST1334.2
025000   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1334.2
025100                                                                  ST1334.2
025200 01  CCVS-H-2B.                                                   ST1334.2
025300   02  FILLER                        PIC X(15)  VALUE             ST1334.2
025400            "TEST RESULT OF ".                                    ST1334.2
025500   02  TEST-ID                       PIC X(9).                    ST1334.2
025600   02  FILLER                        PIC X(4)   VALUE             ST1334.2
025700            " IN ".                                               ST1334.2
025800   02  FILLER                        PIC X(12)  VALUE             ST1334.2
025900     " HIGH       ".                                              ST1334.2
026000   02  FILLER                        PIC X(22)  VALUE             ST1334.2
026100            " LEVEL VALIDATION FOR ".                             ST1334.2
026200   02  FILLER                        PIC X(58)  VALUE             ST1334.2
026300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1334.2
026400 01  CCVS-H-3.                                                    ST1334.2
026500     02  FILLER                      PIC X(34)  VALUE             ST1334.2
026600            " FOR OFFICIAL USE ONLY    ".                         ST1334.2
026700     02  FILLER                      PIC X(58)  VALUE             ST1334.2
026800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1334.2
026900     02  FILLER                      PIC X(28)  VALUE             ST1334.2
027000            "  COPYRIGHT   1985 ".                                ST1334.2
027100 01  CCVS-E-1.                                                    ST1334.2
027200     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1334.2
027300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1334.2
027400     02 ID-AGAIN                     PIC X(9).                    ST1334.2
027500     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1334.2
027600 01  CCVS-E-2.                                                    ST1334.2
027700     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1334.2
027800     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1334.2
027900     02 CCVS-E-2-2.                                               ST1334.2
028000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1334.2
028100         03 FILLER                   PIC X      VALUE SPACE.      ST1334.2
028200         03 ENDER-DESC               PIC X(44)  VALUE             ST1334.2
028300            "ERRORS ENCOUNTERED".                                 ST1334.2
028400 01  CCVS-E-3.                                                    ST1334.2
028500     02  FILLER                      PIC X(22)  VALUE             ST1334.2
028600            " FOR OFFICIAL USE ONLY".                             ST1334.2
028700     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1334.2
028800     02  FILLER                      PIC X(58)  VALUE             ST1334.2
028900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1334.2
029000     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1334.2
029100     02 FILLER                       PIC X(15)  VALUE             ST1334.2
029200             " COPYRIGHT 1985".                                   ST1334.2
029300 01  CCVS-E-4.                                                    ST1334.2
029400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1334.2
029500     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1334.2
029600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1334.2
029700     02 FILLER                       PIC X(40)  VALUE             ST1334.2
029800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1334.2
029900 01  XXINFO.                                                      ST1334.2
030000     02 FILLER                       PIC X(19)  VALUE             ST1334.2
030100            "*** INFORMATION ***".                                ST1334.2
030200     02 INFO-TEXT.                                                ST1334.2
030300       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1334.2
030400       04 XXCOMPUTED                 PIC X(20).                   ST1334.2
030500       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1334.2
030600       04 XXCORRECT                  PIC X(20).                   ST1334.2
030700     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1334.2
030800 01  HYPHEN-LINE.                                                 ST1334.2
030900     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1334.2
031000     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1334.2
031100-    "*****************************************".                 ST1334.2
031200     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1334.2
031300-    "******************************".                            ST1334.2
031400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1334.2
031500     "ST133A".                                                    ST1334.2
031600 PROCEDURE DIVISION.                                              ST1334.2
031700 CCVS1 SECTION.                                                   ST1334.2
031800 OPEN-FILES.                                                      ST1334.2
031900     OPEN     OUTPUT PRINT-FILE.                                  ST1334.2
032000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   ST1334.2
032100     MOVE    SPACE TO TEST-RESULTS.                               ST1334.2
032200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             ST1334.2
032300     GO TO CCVS1-EXIT.                                            ST1334.2
032400 CLOSE-FILES.                                                     ST1334.2
032500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1334.2
032600 TERMINATE-CCVS.                                                  ST1334.2
032700*    EXIT PROGRAM.                                                ST1334.2
032800*TERMINATE-CALL.                                                  ST1334.2
032900     STOP     RUN.                                                ST1334.2
033000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1334.2
033100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1334.2
033200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1334.2
033300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1334.2
033400     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1334.2
033500 PRINT-DETAIL.                                                    ST1334.2
033600     IF REC-CT NOT EQUAL TO ZERO                                  ST1334.2
033700             MOVE "." TO PARDOT-X                                 ST1334.2
033800             MOVE REC-CT TO DOTVALUE.                             ST1334.2
033900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1334.2
034000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1334.2
034100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1334.2
034200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1334.2
034300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1334.2
034400     MOVE SPACE TO CORRECT-X.                                     ST1334.2
034500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1334.2
034600     MOVE     SPACE TO RE-MARK.                                   ST1334.2
034700 HEAD-ROUTINE.                                                    ST1334.2
034800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1334.2
034900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1334.2
035000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1334.2
035100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1334.2
035200 COLUMN-NAMES-ROUTINE.                                            ST1334.2
035300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1334.2
035400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1334.2
035500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1334.2
035600 END-ROUTINE.                                                     ST1334.2
035700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1334.2
035800 END-RTN-EXIT.                                                    ST1334.2
035900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1334.2
036000 END-ROUTINE-1.                                                   ST1334.2
036100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1334.2
036200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1334.2
036300      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1334.2
036400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1334.2
036500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1334.2
036600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1334.2
036700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1334.2
036800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1334.2
036900  END-ROUTINE-12.                                                 ST1334.2
037000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1334.2
037100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1334.2
037200         MOVE "NO " TO ERROR-TOTAL                                ST1334.2
037300         ELSE                                                     ST1334.2
037400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1334.2
037500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1334.2
037600     PERFORM WRITE-LINE.                                          ST1334.2
037700 END-ROUTINE-13.                                                  ST1334.2
037800     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1334.2
037900         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1334.2
038000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1334.2
038100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1334.2
038200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1334.2
038300      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1334.2
038400          MOVE "NO " TO ERROR-TOTAL                               ST1334.2
038500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1334.2
038600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1334.2
038700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1334.2
038800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1334.2
038900 WRITE-LINE.                                                      ST1334.2
039000     ADD 1 TO RECORD-COUNT.                                       ST1334.2
039100*    IF RECORD-COUNT GREATER 42                                   ST1334.2
039200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1334.2
039300*        MOVE SPACE TO DUMMY-RECORD                               ST1334.2
039400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1334.2
039500*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1334.2
039600*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1334.2
039700*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1334.2
039800*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1334.2
039900*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1334.2
040000*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1334.2
040100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1334.2
040200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1334.2
040300*        MOVE ZERO TO RECORD-COUNT.                               ST1334.2
040400     PERFORM WRT-LN.                                              ST1334.2
040500 WRT-LN.                                                          ST1334.2
040600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1334.2
040700     MOVE SPACE TO DUMMY-RECORD.                                  ST1334.2
040800 BLANK-LINE-PRINT.                                                ST1334.2
040900     PERFORM WRT-LN.                                              ST1334.2
041000 FAIL-ROUTINE.                                                    ST1334.2
041100     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1334.2
041200            GO TO   FAIL-ROUTINE-WRITE.                           ST1334.2
041300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1334.2
041400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1334.2
041500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1334.2
041600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1334.2
041700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1334.2
041800     GO TO  FAIL-ROUTINE-EX.                                      ST1334.2
041900 FAIL-ROUTINE-WRITE.                                              ST1334.2
042000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1334.2
042100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1334.2
042200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1334.2
042300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1334.2
042400 FAIL-ROUTINE-EX. EXIT.                                           ST1334.2
042500 BAIL-OUT.                                                        ST1334.2
042600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1334.2
042700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1334.2
042800 BAIL-OUT-WRITE.                                                  ST1334.2
042900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1334.2
043000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1334.2
043100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1334.2
043200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1334.2
043300 BAIL-OUT-EX. EXIT.                                               ST1334.2
043400 CCVS1-EXIT.                                                      ST1334.2
043500     EXIT.                                                        ST1334.2
043600 MAINLINE SECTION.                                                ST1334.2
043700 FIRST-SORT-PARA.                                                 ST1334.2
043800     SORT     FIRST-SORTFILE  ON DESCENDING KEY                   ST1334.2
043900              FIRST-KEY                                           ST1334.2
044000              INPUT PROCEDURE INPROC                              ST1334.2
044100              GIVING SORTOUT-2C.                                  ST1334.2
044200     MOVE    "FIRST SORT DONE" TO FEATURE.                        ST1334.2
044300     PERFORM PRINT-DETAIL.                                        ST1334.2
044400 TAPECOPY-OPEN.                                                   ST1334.2
044500     OPEN     INPUT SORTOUT-2C.                                   ST1334.2
044600     OPEN     OUTPUT SORTIN-2C.                                   ST1334.2
044700 TAPECOPY-LOOP.                                                   ST1334.2
044800     READ     SORTOUT-2C AT END GO TO TAPECOPY-CLOSE.             ST1334.2
044900     MOVE     SORTOUT-REC TO SORTIN-REC.                          ST1334.2
045000     WRITE    SORTIN-REC.                                         ST1334.2
045100     GO       TO TAPECOPY-LOOP.                                   ST1334.2
045200 TAPECOPY-CLOSE.                                                  ST1334.2
045300     CLOSE    SORTIN-2C.                                          ST1334.2
045400     CLOSE    SORTOUT-2C.                                         ST1334.2
045500     MOVE    "TAPE COPY DONE" TO FEATURE.                         ST1334.2
045600     PERFORM PRINT-DETAIL.                                        ST1334.2
045700*        NOTE THIS TAPECOPY ROUTINE HAS NO EFFECT ON THE SORTS ---ST1334.2
045800*             ITS ONLY FUNCTION IS TO EXERCISE THE OPEN, CLOSE,   ST1334.2
045900*             READ, AND WRITE VERBS IN THE MAINLINE.              ST1334.2
046000 SECOND-SORT-PARA.                                                ST1334.2
046100     SORT     SECOND-SORTFILE ON ASCENDING KEY                    ST1334.2
046200              SECOND-KEY                                          ST1334.2
046300              USING SORTOUT-2C                                    ST1334.2
046400              OUTPUT PROCEDURE OUTPROC.                           ST1334.2
046500     MOVE    "SECOND SORT DONE" TO FEATURE.                       ST1334.2
046600     PERFORM PRINT-DETAIL.                                        ST1334.2
046700     GO TO    CCVS-EXIT.                                          ST1334.2
046800 INPROC SECTION.                                                  ST1334.2
046900 INPROC-INIT.                                                     ST1334.2
047000     MOVE     ZERO TO UTIL-CTR.                                   ST1334.2
047100 INPROC-LOOP.                                                     ST1334.2
047200     IF       UTIL-CTR LESS THAN 100                              ST1334.2
047300              MOVE UTIL-CTR TO FIRST-KEY                          ST1334.2
047400              ELSE                                                ST1334.2
047500              MULTIPLY UTIL-CTR BY -1 GIVING FIRST-KEY.           ST1334.2
047600     MOVE     "A" TO FIRST-NON-KEY.                               ST1334.2
047700     RELEASE  FIRST-SORTFILE-REC.                                 ST1334.2
047800     ADD      1 TO UTIL-CTR.                                      ST1334.2
047900     IF       UTIL-CTR LESS THAN 200 GO TO INPROC-LOOP.           ST1334.2
048000 INPROC-EXIT.                                                     ST1334.2
048100     EXIT.                                                        ST1334.2
048200 OUTPROC SECTION.                                                 ST1334.2
048300 SORT-INIT-1.                                                     ST1334.2
048400     MOVE     ZERO TO UTIL-CTR.                                   ST1334.2
048500 SORT-TEST-1.                                                     ST1334.2
048600     MOVE    "NUMERIC KEY CHECKS" TO FEATURE.                     ST1334.2
048700     MOVE     "SORT-TEST-1 " TO PAR-NAME.                         ST1334.2
048800     PERFORM  RETURN-SORTFILE.                                    ST1334.2
048900     IF       SECOND-KEY NOT EQUAL TO -199  GO TO SORT-FAIL-1.    ST1334.2
049000     IF       SECOND-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
049100              PERFORM PASS-1 GO TO SORT-WRITE-1.                  ST1334.2
049200 SORT-FAIL-1.                                                     ST1334.2
049300     PERFORM  FAIL-1.                                             ST1334.2
049400     MOVE     SECOND-KEY TO COMPUTED-N.                           ST1334.2
049500     MOVE     -199 TO CORRECT-N                                   ST1334.2
049600     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
049700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
049800     MOVE     SECOND-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
049900     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
050000 SORT-WRITE-1.                                                    ST1334.2
050100     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
050200 SORT-TEST-2.                                                     ST1334.2
050300     MOVE     "SORT-TEST-2" TO PAR-NAME.                          ST1334.2
050400     PERFORM  RETURN-SORTFILE.                                    ST1334.2
050500     IF       SECOND-KEY NOT EQUAL TO -198  GO TO SORT-FAIL-2.    ST1334.2
050600     IF       SECOND-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
050700              PERFORM PASS-1 GO TO SORT-WRITE-2.                  ST1334.2
050800 SORT-FAIL-2.                                                     ST1334.2
050900     PERFORM  FAIL-1.                                             ST1334.2
051000     MOVE     SECOND-KEY TO COMPUTED-N.                           ST1334.2
051100     MOVE     -198 TO CORRECT-N                                   ST1334.2
051200     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
051300     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
051400     MOVE     SECOND-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
051500     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
051600 SORT-WRITE-2.                                                    ST1334.2
051700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
051800 SORT-TEST-3.                                                     ST1334.2
051900     MOVE     "SORT-TEST-3" TO PAR-NAME.                          ST1334.2
052000     PERFORM  RETURN-SORTFILE 98 TIMES.                           ST1334.2
052100     IF       SECOND-KEY NOT EQUAL TO -100  GO TO SORT-FAIL-3.    ST1334.2
052200     IF       SECOND-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
052300              PERFORM PASS-1 GO TO SORT-WRITE-3.                  ST1334.2
052400 SORT-FAIL-3.                                                     ST1334.2
052500     PERFORM FAIL-1.                                              ST1334.2
052600     MOVE     SECOND-KEY TO COMPUTED-N.                           ST1334.2
052700     MOVE     -100 TO CORRECT-N.                                  ST1334.2
052800     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
052900     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
053000     MOVE     SECOND-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
053100     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
053200 SORT-WRITE-3.                                                    ST1334.2
053300     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
053400 SORT-TEST-4.                                                     ST1334.2
053500     MOVE     "SORT-TEST-4" TO PAR-NAME.                          ST1334.2
053600     PERFORM  RETURN-SORTFILE.                                    ST1334.2
053700     IF       SECOND-KEY NOT EQUAL TO ZERO GO TO SORT-FAIL-4.     ST1334.2
053800     IF       SECOND-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
053900              PERFORM PASS-1 GO TO SORT-WRITE-4.                  ST1334.2
054000 SORT-FAIL-4.                                                     ST1334.2
054100     PERFORM  FAIL-1.                                             ST1334.2
054200     MOVE     SECOND-KEY TO COMPUTED-N.                           ST1334.2
054300     MOVE     ZERO TO CORRECT-N.                                  ST1334.2
054400     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
054500     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
054600     MOVE     SECOND-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
054700     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
054800 SORT-WRITE-4.                                                    ST1334.2
054900     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
055000 SORT-TEST-5.                                                     ST1334.2
055100     MOVE     "SORT-TEST-5" TO PAR-NAME.                          ST1334.2
055200     PERFORM  RETURN-SORTFILE 99 TIMES.                           ST1334.2
055300     IF       SECOND-KEY NOT EQUAL TO 99 GO TO SORT-FAIL-5.       ST1334.2
055400     IF       SECOND-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
055500              PERFORM PASS-1 GO TO SORT-WRITE-5.                  ST1334.2
055600 SORT-FAIL-5.                                                     ST1334.2
055700     PERFORM  FAIL-1.                                             ST1334.2
055800     MOVE     SECOND-KEY TO COMPUTED-N.                           ST1334.2
055900     MOVE     +99  TO CORRECT-N                                   ST1334.2
056000     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
056100     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
056200     MOVE     SECOND-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
056300     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
056400 SORT-WRITE-5.                                                    ST1334.2
056500     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
056600 SORT-TEST-6.                                                     ST1334.2
056700     MOVE     "SORT-TEST-6 " TO PAR-NAME.                         ST1334.2
056800     RETURN   SECOND-SORTFILE AT END                              ST1334.2
056900              PERFORM PASS-1 GO TO SORT-WRITE-6.                  ST1334.2
057000 SORT-FAIL-6.                                                     ST1334.2
057100     PERFORM  FAIL-1.                                             ST1334.2
057200     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1334.2
057300     MOVE     201 TO CORRECT-N.                                   ST1334.2
057400     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1334.2
057500     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
057600     MOVE     SECOND-KEY TO COMPUTED-N.                           ST1334.2
057700     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
057800     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
057900     MOVE     SECOND-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
058000     MOVE     1 TO BREAKDOWN-SWITCH.                              ST1334.2
058100     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
058200     MOVE     ZERO TO BREAKDOWN-SWITCH.                           ST1334.2
058300 SORT-WRITE-6.                                                    ST1334.2
058400     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
058500 SORT-EXIT-A.                                                     ST1334.2
058600     EXIT.                                                        ST1334.2
058700 SORT-INIT-B.                                                     ST1334.2
058800     MOVE     ZERO TO UTIL-CTR.                                   ST1334.2
058900     OPEN     INPUT SORTOUT-2C.                                   ST1334.2
059000 SORT-TEST-7.                                                     ST1334.2
059100     MOVE     "SORT-TEST-7 " TO PAR-NAME.                         ST1334.2
059200     PERFORM  READ-SORTOUT.                                       ST1334.2
059300     IF       SORTOUT-KEY NOT EQUAL TO +99   GO TO SORT-FAIL-7.   ST1334.2
059400     IF       SORTOUT-NON-KEY EQUAL TO JUSTIFIED-A                ST1334.2
059500              PERFORM PASS-1 GO TO SORT-WRITE-7.                  ST1334.2
059600 SORT-FAIL-7.                                                     ST1334.2
059700     PERFORM  FAIL-1.                                             ST1334.2
059800     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1334.2
059900     MOVE     +99  TO CORRECT-N                                   ST1334.2
060000     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
060100     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
060200     MOVE     SORTOUT-NON-KEY TO COMPUTED-BREAKDOWN.              ST1334.2
060300     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
060400 SORT-WRITE-7.                                                    ST1334.2
060500     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
060600 SORT-TEST-8.                                                     ST1334.2
060700     MOVE     "SORT-TEST-8 " TO PAR-NAME.                         ST1334.2
060800     PERFORM  READ-SORTOUT.                                       ST1334.2
060900     IF       SORTOUT-KEY NOT EQUAL TO +98   GO TO SORT-FAIL-8.   ST1334.2
061000     IF       SORTOUT-NON-KEY EQUAL TO JUSTIFIED-A                ST1334.2
061100              PERFORM PASS-1 GO TO SORT-WRITE-8.                  ST1334.2
061200 SORT-FAIL-8.                                                     ST1334.2
061300     PERFORM  FAIL-1.                                             ST1334.2
061400     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1334.2
061500     MOVE     +98 TO CORRECT-N                                    ST1334.2
061600     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
061700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
061800     MOVE     SORTOUT-NON-KEY TO COMPUTED-BREAKDOWN.              ST1334.2
061900     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
062000 SORT-WRITE-8.                                                    ST1334.2
062100     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
062200 SORT-TEST-9.                                                     ST1334.2
062300     MOVE     "SORT-TEST-9 " TO PAR-NAME.                         ST1334.2
062400     PERFORM  READ-SORTOUT 98 TIMES.                              ST1334.2
062500     IF       SORTOUT-KEY NOT EQUAL TO ZERO  GO TO SORT-FAIL-9.   ST1334.2
062600     IF       SORTOUT-NON-KEY EQUAL TO JUSTIFIED-A                ST1334.2
062700              PERFORM PASS-1 GO TO SORT-WRITE-9.                  ST1334.2
062800 SORT-FAIL-9.                                                     ST1334.2
062900     PERFORM  FAIL-1.                                             ST1334.2
063000     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1334.2
063100     MOVE     ZERO TO CORRECT-N                                   ST1334.2
063200     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
063300     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
063400     MOVE     SORTOUT-NON-KEY TO COMPUTED-BREAKDOWN.              ST1334.2
063500     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
063600 SORT-WRITE-9.                                                    ST1334.2
063700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
063800 SORT-TEST-10.                                                    ST1334.2
063900     MOVE     "SORT-TEST-10 " TO PAR-NAME.                        ST1334.2
064000     PERFORM  READ-SORTOUT.                                       ST1334.2
064100     IF       SORTOUT-KEY NOT EQUAL TO -100  GO TO SORT-FAIL-11.  ST1334.2
064200     IF       SORTOUT-NON-KEY EQUAL TO JUSTIFIED-A                ST1334.2
064300              PERFORM PASS-1 GO TO SORT-WRITE-10.                 ST1334.2
064400 SORT-FAIL-10.                                                    ST1334.2
064500     PERFORM  FAIL-1.                                             ST1334.2
064600     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1334.2
064700     MOVE     -100 TO CORRECT-N                                   ST1334.2
064800     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
064900     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
065000     MOVE     SORTOUT-NON-KEY TO COMPUTED-BREAKDOWN.              ST1334.2
065100     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
065200 SORT-WRITE-10.                                                   ST1334.2
065300     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
065400 SORT-TEST-11.                                                    ST1334.2
065500     MOVE     "SORT-TEST-11 " TO PAR-NAME.                        ST1334.2
065600     PERFORM  READ-SORTOUT 99 TIMES.                              ST1334.2
065700     IF       SORTOUT-KEY NOT EQUAL TO -199  GO TO SORT-FAIL-12.  ST1334.2
065800     IF       SORTOUT-NON-KEY EQUAL TO JUSTIFIED-A                ST1334.2
065900              PERFORM PASS-1 GO TO SORT-WRITE-11.                 ST1334.2
066000 SORT-FAIL-11.                                                    ST1334.2
066100     PERFORM  FAIL-1.                                             ST1334.2
066200     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1334.2
066300     MOVE     -199 TO CORRECT-N                                   ST1334.2
066400     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
066500     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
066600     MOVE     SORTOUT-NON-KEY TO COMPUTED-BREAKDOWN.              ST1334.2
066700     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
066800 SORT-WRITE-11.                                                   ST1334.2
066900     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
067000 SORT-TEST-12.                                                    ST1334.2
067100     MOVE     "SORT-TEST-12 " TO PAR-NAME.                        ST1334.2
067200     READ     SORTOUT-2C AT END                                   ST1334.2
067300              PERFORM PASS-1 GO TO SORT-WRITE-12.                 ST1334.2
067400 SORT-FAIL-12.                                                    ST1334.2
067500     PERFORM  FAIL-1.                                             ST1334.2
067600     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1334.2
067700     MOVE     201 TO CORRECT-N.                                   ST1334.2
067800     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1334.2
067900     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
068000     MOVE     SORTOUT-KEY TO COMPUTED-N.                          ST1334.2
068100     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
068200     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
068300     MOVE     SORTOUT-NON-KEY TO COMPUTED-BREAKDOWN.              ST1334.2
068400     MOVE     1 TO BREAKDOWN-SWITCH.                              ST1334.2
068500     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
068600     MOVE     ZERO TO BREAKDOWN-SWITCH.                           ST1334.2
068700 SORT-WRITE-12.                                                   ST1334.2
068800     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
068900 SORT-EXIT-B.                                                     ST1334.2
069000     EXIT.                                                        ST1334.2
069100 SORT-INIT-C.                                                     ST1334.2
069200     OPEN     INPUT SORTIN-2C.                                    ST1334.2
069300     MOVE     ZERO TO UTIL-CTR.                                   ST1334.2
069400 SORT-TEST-13.                                                    ST1334.2
069500     MOVE     "SORT-TEST-13 " TO PAR-NAME.                        ST1334.2
069600     PERFORM  READ-SORTIN.                                        ST1334.2
069700     IF       SORTIN-KEY NOT EQUAL TO +99    GO TO SORT-FAIL-13.  ST1334.2
069800     IF       SORTIN-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
069900              PERFORM PASS-1 GO TO SORT-WRITE-13.                 ST1334.2
070000 SORT-FAIL-13.                                                    ST1334.2
070100     PERFORM  FAIL-1.                                             ST1334.2
070200     MOVE     SORTIN-KEY TO COMPUTED-N.                           ST1334.2
070300     MOVE     +99  TO CORRECT-N                                   ST1334.2
070400     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
070500     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
070600     MOVE     SORTIN-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
070700     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
070800 SORT-WRITE-13.                                                   ST1334.2
070900     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
071000 SORT-TEST-14.                                                    ST1334.2
071100     MOVE     "SORT-TEST-14 " TO PAR-NAME.                        ST1334.2
071200     PERFORM  READ-SORTIN.                                        ST1334.2
071300     IF       SORTIN-KEY NOT EQUAL TO +98 GO TO SORT-FAIL-14.     ST1334.2
071400     IF       SORTIN-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
071500              PERFORM PASS-1 GO TO SORT-WRITE-14.                 ST1334.2
071600 SORT-FAIL-14.                                                    ST1334.2
071700     PERFORM  FAIL-1.                                             ST1334.2
071800     MOVE     SORTIN-KEY TO COMPUTED-N.                           ST1334.2
071900     MOVE     +98  TO CORRECT-N                                   ST1334.2
072000     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
072100     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
072200     MOVE     SORTIN-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
072300     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
072400 SORT-WRITE-14.                                                   ST1334.2
072500     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
072600 SORT-TEST-15.                                                    ST1334.2
072700     MOVE     "SORT-TEST-15 " TO PAR-NAME.                        ST1334.2
072800     PERFORM  READ-SORTIN 98 TIMES.                               ST1334.2
072900     IF       SORTIN-KEY NOT EQUAL TO ZERO  GO TO SORT-FAIL-15.   ST1334.2
073000     IF       SORTIN-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
073100              PERFORM PASS-1 GO TO SORT-WRITE-15.                 ST1334.2
073200 SORT-FAIL-15.                                                    ST1334.2
073300     PERFORM  FAIL-1.                                             ST1334.2
073400     MOVE     SORTIN-KEY TO COMPUTED-N.                           ST1334.2
073500     MOVE     ZERO TO CORRECT-N                                   ST1334.2
073600     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
073700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
073800     MOVE     SORTIN-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
073900     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
074000 SORT-WRITE-15.                                                   ST1334.2
074100     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
074200 SORT-TEST-16.                                                    ST1334.2
074300     MOVE     "SORT-TEST-16 " TO PAR-NAME.                        ST1334.2
074400     PERFORM  READ-SORTIN.                                        ST1334.2
074500     IF       SORTIN-KEY NOT EQUAL TO -100  GO TO SORT-FAIL-16.   ST1334.2
074600     IF       SORTIN-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
074700              PERFORM PASS-1 GO TO SORT-WRITE-16.                 ST1334.2
074800 SORT-FAIL-16.                                                    ST1334.2
074900     PERFORM  FAIL-1.                                             ST1334.2
075000     MOVE     SORTIN-KEY TO COMPUTED-N.                           ST1334.2
075100     MOVE     -100 TO CORRECT-N                                   ST1334.2
075200     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
075300     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
075400     MOVE     SORTIN-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
075500     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
075600 SORT-WRITE-16.                                                   ST1334.2
075700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
075800 SORT-TEST-17.                                                    ST1334.2
075900     MOVE     "SORT-TEST-17 " TO PAR-NAME.                        ST1334.2
076000     PERFORM  READ-SORTIN 99 TIMES.                               ST1334.2
076100     IF       SORTIN-KEY NOT EQUAL TO -199  GO TO SORT-FAIL-17.   ST1334.2
076200     IF       SORTIN-NON-KEY EQUAL TO JUSTIFIED-A                 ST1334.2
076300              PERFORM PASS-1 GO TO SORT-WRITE-17.                 ST1334.2
076400 SORT-FAIL-17.                                                    ST1334.2
076500     PERFORM  FAIL-1.                                             ST1334.2
076600     MOVE     SORTIN-KEY TO COMPUTED-N.                           ST1334.2
076700     MOVE     -199 TO CORRECT-N                                   ST1334.2
076800     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
076900     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
077000     MOVE     SORTIN-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
077100     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
077200 SORT-WRITE-17.                                                   ST1334.2
077300     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
077400 SORT-TEST-18.                                                    ST1334.2
077500     MOVE     "SORT-TEST-18" TO PAR-NAME.                         ST1334.2
077600     READ     SORTIN-2C AT END                                    ST1334.2
077700              PERFORM PASS-1 GO TO SORT-WRITE-18.                 ST1334.2
077800 SORT-FAIL-18.                                                    ST1334.2
077900     PERFORM  FAIL-1.                                             ST1334.2
078000     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1334.2
078100     MOVE     201 TO CORRECT-N.                                   ST1334.2
078200     MOVE     "END OF FILE NOT FOUND" TO RE-MARK.                 ST1334.2
078300     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
078400     MOVE     SORTIN-KEY TO COMPUTED-N.                           ST1334.2
078500     MOVE     "KEY AREA" TO RE-MARK.                              ST1334.2
078600     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
078700     MOVE     SORTIN-NON-KEY TO COMPUTED-BREAKDOWN.               ST1334.2
078800     MOVE     1 TO BREAKDOWN-SWITCH.                              ST1334.2
078900     PERFORM  NON-KEY-BREAKDOWN.                                  ST1334.2
079000     MOVE     ZERO TO BREAKDOWN-SWITCH.                           ST1334.2
079100 SORT-WRITE-18.                                                   ST1334.2
079200     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
079300 SORT-CLOSE-19.                                                   ST1334.2
079400     CLOSE    SORTIN-2C.                                          ST1334.2
079500     CLOSE    SORTOUT-2C.                                         ST1334.2
079600     GO       TO OUTPROC-EXIT.                                    ST1334.2
079700 NON-KEY-BREAKDOWN.                                               ST1334.2
079800     MOVE     FIRST-20 TO COMPUTED-A.                             ST1334.2
079900     IF       BREAKDOWN-SWITCH EQUAL TO ZERO                      ST1334.2
080000              MOVE SP-ACE TO CORRECT-A.                           ST1334.2
080100     MOVE     "FIRST 20 OF 72-CHAR FIELD" TO RE-MARK.             ST1334.2
080200     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
080300     MOVE     SECOND-20 TO COMPUTED-A.                            ST1334.2
080400     IF       BREAKDOWN-SWITCH EQUAL TO ZERO                      ST1334.2
080500              MOVE SP-ACE TO CORRECT-A.                           ST1334.2
080600     MOVE     "SECOND 20 OF 72-CHAR FIELD" TO RE-MARK.            ST1334.2
080700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
080800     MOVE     THIRD-20 TO COMPUTED-A.                             ST1334.2
080900     IF       BREAKDOWN-SWITCH EQUAL TO ZERO                      ST1334.2
081000     MOVE SP-ACE TO CORRECT-A.                                    ST1334.2
081100     MOVE     "THIRD 20 OF 72-CHAR FIELD" TO RE-MARK.             ST1334.2
081200     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
081300     MOVE     FOURTH-20 TO COMPUTED-A.                            ST1334.2
081400     IF       BREAKDOWN-SWITCH EQUAL TO ZERO                      ST1334.2
081500              MOVE "           A" TO CORRECT-A.                   ST1334.2
081600     MOVE     "LAST 12 OF 72-CHAR FIELD" TO RE-MARK.              ST1334.2
081700 RETURN-SORTFILE.                                                 ST1334.2
081800     ADD      1 TO UTIL-CTR.                                      ST1334.2
081900     RETURN   SECOND-SORTFILE AT END GO TO RETURN-ERROR.          ST1334.2
082000 RETURN-ERROR.                                                    ST1334.2
082100     MOVE     "RETURN-ERROR" TO PAR-NAME.                         ST1334.2
082200     PERFORM  FAIL-1.                                             ST1334.2
082300     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1334.2
082400     MOVE     201 TO CORRECT-N.                                   ST1334.2
082500     MOVE     "END OF SORT FILE PREMATURE" TO RE-MARK.            ST1334.2
082600     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
082700     GO       TO SORT-EXIT-A.                                     ST1334.2
082800 READ-SORTOUT.                                                    ST1334.2
082900     ADD      1 TO UTIL-CTR.                                      ST1334.2
083000     READ     SORTOUT-2C AT END GO TO READ-SORTOUT-ERROR.         ST1334.2
083100 READ-SORTOUT-ERROR.                                              ST1334.2
083200     MOVE     "READ-SORTOUT-ERROR" TO PAR-NAME.                   ST1334.2
083300     PERFORM  FAIL-1.                                             ST1334.2
083400     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1334.2
083500     MOVE     201 TO CORRECT-N.                                   ST1334.2
083600     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1334.2
083700     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
083800     GO       TO SORT-EXIT-B.                                     ST1334.2
083900 READ-SORTIN.                                                     ST1334.2
084000     ADD      1 TO UTIL-CTR.                                      ST1334.2
084100     READ     SORTIN-2C AT END GO TO READ-SORTIN-ERROR.           ST1334.2
084200 READ-SORTIN-ERROR.                                               ST1334.2
084300     MOVE     "READ-SORTIN-ERROR" TO PAR-NAME.                    ST1334.2
084400     PERFORM  FAIL-1.                                             ST1334.2
084500     MOVE     UTIL-CTR TO COMPUTED-N.                             ST1334.2
084600     MOVE     201 TO CORRECT-N.                                   ST1334.2
084700     MOVE     "EOF PREMATURELY FOUND" TO RE-MARK.                 ST1334.2
084800     PERFORM  PRINT-DETAIL-1.                                     ST1334.2
084900     GO TO    SORT-CLOSE-19.                                      ST1334.2
085000 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       ST1334.2
085100 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         ST1334.2
085200 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        ST1334.2
085300 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    ST1334.2
085400     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1334.2
085500 PRINT-DETAIL-1.                                                  ST1334.2
085600     IF REC-CT NOT EQUAL TO ZERO                                  ST1334.2
085700             MOVE "." TO PARDOT-X                                 ST1334.2
085800             MOVE REC-CT TO DOTVALUE.                             ST1334.2
085900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    ST1334.2
086000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             ST1334.2
086100        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             ST1334.2
086200          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             ST1334.2
086300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1334.2
086400     MOVE SPACE TO CORRECT-X.                                     ST1334.2
086500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1334.2
086600     MOVE     SPACE TO RE-MARK.                                   ST1334.2
086700 WRITE-LINE-1.                                                    ST1334.2
086800     ADD 1 TO RECORD-COUNT.                                       ST1334.2
086900*    IF RECORD-COUNT GREATER 50                                   ST1334.2
087000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1334.2
087100*        MOVE SPACE TO DUMMY-RECORD                               ST1334.2
087200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1334.2
087300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           ST1334.2
087400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   ST1334.2
087500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        ST1334.2
087600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1334.2
087700*        MOVE ZERO TO RECORD-COUNT.                               ST1334.2
087800     PERFORM WRT-LN-1.                                            ST1334.2
087900 WRT-LN-1.                                                        ST1334.2
088000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1334.2
088100     MOVE SPACE TO DUMMY-RECORD.                                  ST1334.2
088200 BLANK-LINE-PRINT-1.                                              ST1334.2
088300     PERFORM WRT-LN-1.                                            ST1334.2
088400 FAIL-ROUTINE-1.                                                  ST1334.2
088500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     ST1334.2
088600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      ST1334.2
088700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    ST1334.2
088800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1334.2
088900     GO TO FAIL-ROUTINE-EX-1.                                     ST1334.2
089000 FAIL-RTN-WRITE-1.                                                ST1334.2
089100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         ST1334.2
089200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. ST1334.2
089300 FAIL-ROUTINE-EX-1. EXIT.                                         ST1334.2
089400 BAIL-OUT-1.                                                      ST1334.2
089500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     ST1334.2
089600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             ST1334.2
089700 BAIL-OUT-WRITE-1.                                                ST1334.2
089800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1334.2
089900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1334.2
090000 BAIL-OUT-EX-1. EXIT.                                             ST1334.2
090100 OUTPROC-EXIT.                                                    ST1334.2
090200     EXIT.                                                        ST1334.2
090300 CCVS-EXIT SECTION.                                               ST1334.2
090400 CCVS-999999.                                                     ST1334.2
090500     GO TO CLOSE-FILES.                                           ST1334.2
      *END-OF,ST133A                                                                  
