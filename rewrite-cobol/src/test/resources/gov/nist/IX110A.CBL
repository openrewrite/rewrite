      *HEADER,COBOL,IX109A,SUBPRG,IX110A                                              
000100 IDENTIFICATION DIVISION.                                         IX1104.2
000200 PROGRAM-ID.                                                      IX1104.2
000300     IX110A.                                                      IX1104.2
000400****************************************************************  IX1104.2
000500*                                                              *  IX1104.2
000600*    VALIDATION FOR:-                                          *  IX1104.2
000700*                                                              *  IX1104.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1104.2
000900*                                                              *  IX1104.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1104.2
001100*                                                              *  IX1104.2
001200****************************************************************  IX1104.2
001300*                                                                 IX1104.2
001400*    1. THE ROUTINE USES THE FILE IX-FS3 WHICH HAS BEEN CREATED   IX1104.2
001500*       BY  IX109. THIS FILE IS OPENED IN I-O MODE.               IX1104.2
001600                                                                  IX1104.2
001700*    2. THE ROUTINE CHECKS THE FILE STATUS CODES:                 IX1104.2
001800*           00  -  AFTER OPEN I-O                                 IX1104.2
001900*           22  -  AFTER WRITE  (DUPLIACATE PRIMARY RECORD KEY)   IX1104.2
002000*    00 OR  22  -  AFTER REWRITE  (DUPLIACATE PRIMARY RECORD KEY) IX1104.2
002100*           23  -  AFTER READ  (A NOT EXISTING RECORD KEY)        IX1104.2
002200                                                                  IX1104.2
002300*    3. X-CARDS USED IN THIS PROGRAM (WITH THE OPT CODE):         IX1104.2
002400*                                                                 IX1104.2
002500*                 XXXXX024                                        IX1104.2
002600*                 XXXXX055.                                       IX1104.2
002700*          P      XXXXX062.                                       IX1104.2
002800*                 XXXXX082.                                       IX1104.2
002900*                 XXXXX083.                                       IX1104.2
003000*                 XXXXX084                                        IX1104.2
003100*                                                                 IX1104.2
003200*                                                                 IX1104.2
003300 ENVIRONMENT DIVISION.                                            IX1104.2
003400 CONFIGURATION SECTION.                                           IX1104.2
003500 SOURCE-COMPUTER.                                                 IX1104.2
003600     XXXXX082.                                                    IX1104.2
003700 OBJECT-COMPUTER.                                                 IX1104.2
003800     XXXXX083.                                                    IX1104.2
003900 INPUT-OUTPUT SECTION.                                            IX1104.2
004000 FILE-CONTROL.                                                    IX1104.2
004100*    SELECT RAW-DATA   ASSIGN TO                                  IX1104.2
004200*    XXXXX062                                                     IX1104.2
004300*           ORGANIZATION IS INDEXED                               IX1104.2
004400*           ACCESS MODE IS RANDOM                                 IX1104.2
004500*           RECORD KEY IS RAW-DATA-KEY.                           IX1104.2
004600*                                                                 IX1104.2
004700     SELECT PRINT-FILE ASSIGN TO                                  IX1104.2
004800     XXXXX055.                                                    IX1104.2
004900*                                                                 IX1104.2
005000     SELECT IX-FS3 ASSIGN                                         IX1104.2
005100     XXXXX024                                                     IX1104.2
005200     ORGANIZATION IS INDEXED                                      IX1104.2
005300     ACCESS MODE IS RANDOM                                        IX1104.2
005400     RECORD KEY IS IX-FS3-KEY                                     IX1104.2
005500     FILE STATUS IS IX-FS3-STATUS.                                IX1104.2
005600                                                                  IX1104.2
005700 DATA DIVISION.                                                   IX1104.2
005800                                                                  IX1104.2
005900 FILE SECTION.                                                    IX1104.2
006000*                                                                 IX1104.2
006100*FD  RAW-DATA.                                                    IX1104.2
006200*                                                                 IX1104.2
006300*01  RAW-DATA-SATZ.                                               IX1104.2
006400*    05  RAW-DATA-KEY        PIC X(6).                            IX1104.2
006500*    05  C-DATE              PIC 9(6).                            IX1104.2
006600*    05  C-TIME              PIC 9(8).                            IX1104.2
006700*    05  C-NO-OF-TESTS       PIC 99.                              IX1104.2
006800*    05  C-OK                PIC 999.                             IX1104.2
006900*    05  C-ALL               PIC 999.                             IX1104.2
007000*    05  C-FAIL              PIC 999.                             IX1104.2
007100*    05  C-DELETED           PIC 999.                             IX1104.2
007200*    05  C-INSPECT           PIC 999.                             IX1104.2
007300*    05  C-NOTE              PIC X(13).                           IX1104.2
007400*    05  C-INDENT            PIC X.                               IX1104.2
007500*    05  C-ABORT             PIC X(8).                            IX1104.2
007600                                                                  IX1104.2
007700 FD  PRINT-FILE.                                                  IX1104.2
007800                                                                  IX1104.2
007900 01  PRINT-REC               PIC X(120).                          IX1104.2
008000                                                                  IX1104.2
008100 01  DUMMY-RECORD            PIC X(120).                          IX1104.2
008200                                                                  IX1104.2
008300 FD  IX-FS3                                                       IX1104.2
008400C       DATA RECORDS IX-FS3R1-F-G-240                             IX1104.2
008500C       LABEL RECORD STANDARD                                     IX1104.2
008600        RECORD 240                                                IX1104.2
008700        BLOCK CONTAINS 2 RECORDS.                                 IX1104.2
008800                                                                  IX1104.2
008900 01  IX-FS3R1-F-G-240.                                            IX1104.2
009000     05  IX-FS3-REC-120      PIC X(120).                          IX1104.2
009100     05  IX-FS3-REC-120-240.                                      IX1104.2
009200         10  FILLER          PIC X(8).                            IX1104.2
009300         10  IX-FS3-KEY      PIC X(29).                           IX1104.2
009400         10  FILLER          PIC X(9).                            IX1104.2
009500         10  IX-FS3-ALTER-KEY  PIC X(29).                         IX1104.2
009600         10  FILLER            PIC X(45).                         IX1104.2
009700                                                                  IX1104.2
009800                                                                  IX1104.2
009900 WORKING-STORAGE SECTION.                                         IX1104.2
010000                                                                  IX1104.2
010100 01  GRP-0101.                                                    IX1104.2
010200     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1104.2
010300     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1104.2
010400     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1104.2
010500                                                                  IX1104.2
010600 01  GRP-0102.                                                    IX1104.2
010700     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1104.2
010800     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1104.2
010900     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1104.2
011000                                                                  IX1104.2
011100 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1104.2
011200                                                                  IX1104.2
011300 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1104.2
011400                                                                  IX1104.2
011500 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1104.2
011600                                                                  IX1104.2
011700 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1104.2
011800                                                                  IX1104.2
011900 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1104.2
012000                                                                  IX1104.2
012100 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1104.2
012200                                                                  IX1104.2
012250 01  STATUS-TEST-10          PIC P VALUE ZERO.                    IX1104.2
012300                                                                  IX1104.2
012400 01  IX-FS3-STATUS.                                               IX1104.2
012500     05  IX-FS3-STAT1        PIC X.                               IX1104.2
012600     05  IX-FS3-STAT2        PIC X.                               IX1104.2
012700                                                                  IX1104.2
012800 01  COUNT-OF-RECS           PIC 9(5).                            IX1104.2
012900                                                                  IX1104.2
013000 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1104.2
013100                                                                  IX1104.2
013200 01  FILE-RECORD-INFORMATION-REC.                                 IX1104.2
013300     05  FILE-RECORD-INFO-SKELETON.                               IX1104.2
013400         10  FILLER          PIC X(48) VALUE                      IX1104.2
013500              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1104.2
013600         10  FILLER          PIC X(46) VALUE                      IX1104.2
013700                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1104.2
013800         10  FILLER          PIC X(26) VALUE                      IX1104.2
013900                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1104.2
014000         10  FILLER          PIC X(37) VALUE                      IX1104.2
014100                         ",RECKEY=                             ". IX1104.2
014200         10  FILLER          PIC X(38) VALUE                      IX1104.2
014300                        ",ALTKEY1=                             ". IX1104.2
014400         10  FILLER          PIC X(38) VALUE                      IX1104.2
014500                        ",ALTKEY2=                             ". IX1104.2
014600         10  FILLER          PIC X(7) VALUE SPACE.                IX1104.2
014700     05  FILE-RECORD-INFO             OCCURS 10.                  IX1104.2
014800         10  FILE-RECORD-INFO-P1-120.                             IX1104.2
014900             15  FILLER      PIC X(5).                            IX1104.2
015000             15  XFILE-NAME  PIC X(6).                            IX1104.2
015100             15  FILLER      PIC X(8).                            IX1104.2
015200             15  XRECORD-NAME  PIC X(6).                          IX1104.2
015300             15  FILLER        PIC X(1).                          IX1104.2
015400             15  REELUNIT-NUMBER  PIC 9(1).                       IX1104.2
015500             15  FILLER           PIC X(7).                       IX1104.2
015600             15  XRECORD-NUMBER   PIC 9(6).                       IX1104.2
015700             15  FILLER           PIC X(6).                       IX1104.2
015800             15  UPDATE-NUMBER    PIC 9(2).                       IX1104.2
015900             15  FILLER           PIC X(5).                       IX1104.2
016000             15  ODO-NUMBER       PIC 9(4).                       IX1104.2
016100             15  FILLER           PIC X(5).                       IX1104.2
016200             15  XPROGRAM-NAME    PIC X(5).                       IX1104.2
016300             15  FILLER           PIC X(7).                       IX1104.2
016400             15  XRECORD-LENGTH   PIC 9(6).                       IX1104.2
016500             15  FILLER           PIC X(7).                       IX1104.2
016600             15  CHARS-OR-RECORDS  PIC X(2).                      IX1104.2
016700             15  FILLER            PIC X(1).                      IX1104.2
016800             15  XBLOCK-SIZE       PIC 9(4).                      IX1104.2
016900             15  FILLER            PIC X(6).                      IX1104.2
017000             15  RECORDS-IN-FILE   PIC 9(6).                      IX1104.2
017100             15  FILLER            PIC X(5).                      IX1104.2
017200             15  XFILE-ORGANIZATION  PIC X(2).                    IX1104.2
017300             15  FILLER              PIC X(6).                    IX1104.2
017400             15  XLABEL-TYPE         PIC X(1).                    IX1104.2
017500         10  FILE-RECORD-INFO-P121-240.                           IX1104.2
017600             15  FILLER              PIC X(8).                    IX1104.2
017700             15  XRECORD-KEY         PIC X(29).                   IX1104.2
017800             15  FILLER              PIC X(9).                    IX1104.2
017900             15  ALTERNATE-KEY1      PIC X(29).                   IX1104.2
018000             15  FILLER              PIC X(9).                    IX1104.2
018100             15  ALTERNATE-KEY2      PIC X(29).                   IX1104.2
018200             15  FILLER              PIC X(7).                    IX1104.2
018300                                                                  IX1104.2
018400 01  TEST-RESULTS.                                                IX1104.2
018500     02 FILLER                   PIC X      VALUE SPACE.          IX1104.2
018600     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1104.2
018700     02 FILLER                   PIC X      VALUE SPACE.          IX1104.2
018800     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1104.2
018900     02 FILLER                   PIC X      VALUE SPACE.          IX1104.2
019000     02  PAR-NAME.                                                IX1104.2
019100       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1104.2
019200       03  PARDOT-X              PIC X      VALUE SPACE.          IX1104.2
019300       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1104.2
019400     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1104.2
019500     02 RE-MARK                  PIC X(61).                       IX1104.2
019600 01  TEST-COMPUTED.                                               IX1104.2
019700     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1104.2
019800     02 FILLER                   PIC X(17)  VALUE                 IX1104.2
019900            "       COMPUTED=".                                   IX1104.2
020000     02 COMPUTED-X.                                               IX1104.2
020100     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1104.2
020200     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1104.2
020300                                 PIC -9(9).9(9).                  IX1104.2
020400     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1104.2
020500     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1104.2
020600     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1104.2
020700     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1104.2
020800         04 COMPUTED-18V0                    PIC -9(18).          IX1104.2
020900         04 FILLER                           PIC X.               IX1104.2
021000     03 FILLER PIC X(50) VALUE SPACE.                             IX1104.2
021100 01  TEST-CORRECT.                                                IX1104.2
021200     02 FILLER PIC X(30) VALUE SPACE.                             IX1104.2
021300     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1104.2
021400     02 CORRECT-X.                                                IX1104.2
021500     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1104.2
021600     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1104.2
021700     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1104.2
021800     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1104.2
021900     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1104.2
022000     03      CR-18V0 REDEFINES CORRECT-A.                         IX1104.2
022100         04 CORRECT-18V0                     PIC -9(18).          IX1104.2
022200         04 FILLER                           PIC X.               IX1104.2
022300     03 FILLER PIC X(2) VALUE SPACE.                              IX1104.2
022400     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1104.2
022500 01  CCVS-C-1.                                                    IX1104.2
022600     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1104.2
022700-    "SS  PARAGRAPH-NAME                                          IX1104.2
022800-    "       REMARKS".                                            IX1104.2
022900     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1104.2
023000 01  CCVS-C-2.                                                    IX1104.2
023100     02 FILLER                     PIC X        VALUE SPACE.      IX1104.2
023200     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1104.2
023300     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1104.2
023400     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1104.2
023500     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1104.2
023600 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1104.2
023700 01  REC-CT                        PIC 99       VALUE ZERO.       IX1104.2
023800 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1104.2
023900 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1104.2
024000 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1104.2
024100 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1104.2
024200 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1104.2
024300 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1104.2
024400 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1104.2
024500 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1104.2
024600 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1104.2
024700 01  CCVS-H-1.                                                    IX1104.2
024800     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1104.2
024900     02  FILLER                    PIC X(42)    VALUE             IX1104.2
025000     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1104.2
025100     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1104.2
025200 01  CCVS-H-2A.                                                   IX1104.2
025300   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1104.2
025400   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1104.2
025500   02  FILLER                        PIC XXXX   VALUE             IX1104.2
025600     "4.2 ".                                                      IX1104.2
025700   02  FILLER                        PIC X(28)  VALUE             IX1104.2
025800            " COPY - NOT FOR DISTRIBUTION".                       IX1104.2
025900   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1104.2
026000                                                                  IX1104.2
026100 01  CCVS-H-2B.                                                   IX1104.2
026200   02  FILLER                        PIC X(15)  VALUE             IX1104.2
026300            "TEST RESULT OF ".                                    IX1104.2
026400   02  TEST-ID                       PIC X(9).                    IX1104.2
026500   02  FILLER                        PIC X(4)   VALUE             IX1104.2
026600            " IN ".                                               IX1104.2
026700   02  FILLER                        PIC X(12)  VALUE             IX1104.2
026800     " HIGH       ".                                              IX1104.2
026900   02  FILLER                        PIC X(22)  VALUE             IX1104.2
027000            " LEVEL VALIDATION FOR ".                             IX1104.2
027100   02  FILLER                        PIC X(58)  VALUE             IX1104.2
027200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1104.2
027300 01  CCVS-H-3.                                                    IX1104.2
027400     02  FILLER                      PIC X(34)  VALUE             IX1104.2
027500            " FOR OFFICIAL USE ONLY    ".                         IX1104.2
027600     02  FILLER                      PIC X(58)  VALUE             IX1104.2
027700     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1104.2
027800     02  FILLER                      PIC X(28)  VALUE             IX1104.2
027900            "  COPYRIGHT   1985 ".                                IX1104.2
028000 01  CCVS-E-1.                                                    IX1104.2
028100     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1104.2
028200     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1104.2
028300     02 ID-AGAIN                     PIC X(9).                    IX1104.2
028400     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1104.2
028500 01  CCVS-E-2.                                                    IX1104.2
028600     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1104.2
028700     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1104.2
028800     02 CCVS-E-2-2.                                               IX1104.2
028900         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1104.2
029000         03 FILLER                   PIC X      VALUE SPACE.      IX1104.2
029100         03 ENDER-DESC               PIC X(44)  VALUE             IX1104.2
029200            "ERRORS ENCOUNTERED".                                 IX1104.2
029300 01  CCVS-E-3.                                                    IX1104.2
029400     02  FILLER                      PIC X(22)  VALUE             IX1104.2
029500            " FOR OFFICIAL USE ONLY".                             IX1104.2
029600     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1104.2
029700     02  FILLER                      PIC X(58)  VALUE             IX1104.2
029800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1104.2
029900     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1104.2
030000     02 FILLER                       PIC X(15)  VALUE             IX1104.2
030100             " COPYRIGHT 1985".                                   IX1104.2
030200 01  CCVS-E-4.                                                    IX1104.2
030300     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1104.2
030400     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1104.2
030500     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1104.2
030600     02 FILLER                       PIC X(40)  VALUE             IX1104.2
030700      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1104.2
030800 01  XXINFO.                                                      IX1104.2
030900     02 FILLER                       PIC X(19)  VALUE             IX1104.2
031000            "*** INFORMATION ***".                                IX1104.2
031100     02 INFO-TEXT.                                                IX1104.2
031200       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1104.2
031300       04 XXCOMPUTED                 PIC X(20).                   IX1104.2
031400       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1104.2
031500       04 XXCORRECT                  PIC X(20).                   IX1104.2
031600     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1104.2
031700 01  HYPHEN-LINE.                                                 IX1104.2
031800     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1104.2
031900     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1104.2
032000-    "*****************************************".                 IX1104.2
032100     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1104.2
032200-    "******************************".                            IX1104.2
032300 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1104.2
032400     "IX110A".                                                    IX1104.2
032500 PROCEDURE DIVISION.                                              IX1104.2
032600 DECLARATIVES.                                                    IX1104.2
032700                                                                  IX1104.2
032800 SECT-IX110-0002 SECTION.                                         IX1104.2
032900     USE AFTER EXCEPTION PROCEDURE ON IX-FS3.                     IX1104.2
033000 INPUT-PROCESS.                                                   IX1104.2
033100     IF STATUS-TEST-10 EQUAL TO 1                                 IX1104.2
033200        GO TO FINAL-CHECK                                         IX1104.2
033300     ELSE                                                         IX1104.2
033400        GO TO DECL-EXIT.                                          IX1104.2
033500 FINAL-CHECK.                                                     IX1104.2
033600     IF     IX-FS3-STAT1 EQUAL TO "1"                             IX1104.2
033700         MOVE 1 TO EOF-FLAG.                                      IX1104.2
033800     IF                                                           IX1104.2
033900            IX-FS3-STAT1 GREATER THAN "1"                         IX1104.2
034000         MOVE 1 TO PERM-ERRORS.                                   IX1104.2
034100 DECL-EXIT.                                                       IX1104.2
034200 END DECLARATIVES.                                                IX1104.2
034300                                                                  IX1104.2
034400                                                                  IX1104.2
034500 CCVS1 SECTION.                                                   IX1104.2
034600 OPEN-FILES.                                                      IX1104.2
034700*    OPEN I-O RAW-DATA.                                           IX1104.2
034800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1104.2
034900*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1104.2
035000*    MOVE "ABORTED " TO C-ABORT.                                  IX1104.2
035100*    ADD 1 TO C-NO-OF-TESTS.                                      IX1104.2
035200*    ACCEPT C-DATE  FROM DATE.                                    IX1104.2
035300*    ACCEPT C-TIME  FROM TIME.                                    IX1104.2
035400*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1104.2
035500*END-E-1.                                                         IX1104.2
035600*    CLOSE RAW-DATA.                                              IX1104.2
035700     OPEN    OUTPUT PRINT-FILE.                                   IX1104.2
035800     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1104.2
035900     MOVE    SPACE TO TEST-RESULTS.                               IX1104.2
036000     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1104.2
036100     MOVE    ZERO TO REC-SKL-SUB.                                 IX1104.2
036200     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1104.2
036300 CCVS-INIT-FILE.                                                  IX1104.2
036400     ADD     1 TO REC-SKL-SUB.                                    IX1104.2
036500     MOVE    FILE-RECORD-INFO-SKELETON                            IX1104.2
036600          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1104.2
036700 CCVS-INIT-EXIT.                                                  IX1104.2
036800     GO TO CCVS1-EXIT.                                            IX1104.2
036900 CLOSE-FILES.                                                     IX1104.2
037000*    OPEN I-O RAW-DATA.                                           IX1104.2
037100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1104.2
037200*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1104.2
037300*    MOVE "OK.     " TO C-ABORT.                                  IX1104.2
037400*    MOVE PASS-COUNTER TO C-OK.                                   IX1104.2
037500*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1104.2
037600*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1104.2
037700*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1104.2
037800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1104.2
037900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1104.2
038000*END-E-2.                                                         IX1104.2
038100*    CLOSE RAW-DATA.                                              IX1104.2
038200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1104.2
038300 TERMINATE-CCVS.                                                  IX1104.2
038400*    EXIT PROGRAM.                                                IX1104.2
038500*TERMINATE-CALL.                                                  IX1104.2
038600     STOP     RUN.                                                IX1104.2
038700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1104.2
038800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1104.2
038900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1104.2
039000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1104.2
039100     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1104.2
039200 PRINT-DETAIL.                                                    IX1104.2
039300     IF REC-CT NOT EQUAL TO ZERO                                  IX1104.2
039400             MOVE "." TO PARDOT-X                                 IX1104.2
039500             MOVE REC-CT TO DOTVALUE.                             IX1104.2
039600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1104.2
039700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1104.2
039800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1104.2
039900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1104.2
040000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1104.2
040100     MOVE SPACE TO CORRECT-X.                                     IX1104.2
040200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1104.2
040300     MOVE     SPACE TO RE-MARK.                                   IX1104.2
040400 HEAD-ROUTINE.                                                    IX1104.2
040500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1104.2
040600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1104.2
040700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1104.2
040800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1104.2
040900 COLUMN-NAMES-ROUTINE.                                            IX1104.2
041000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1104.2
041100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1104.2
041200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1104.2
041300 END-ROUTINE.                                                     IX1104.2
041400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1104.2
041500 END-RTN-EXIT.                                                    IX1104.2
041600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1104.2
041700 END-ROUTINE-1.                                                   IX1104.2
041800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1104.2
041900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1104.2
042000      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1104.2
042100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1104.2
042200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1104.2
042300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1104.2
042400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1104.2
042500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1104.2
042600  END-ROUTINE-12.                                                 IX1104.2
042700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1104.2
042800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1104.2
042900         MOVE "NO " TO ERROR-TOTAL                                IX1104.2
043000         ELSE                                                     IX1104.2
043100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1104.2
043200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1104.2
043300     PERFORM WRITE-LINE.                                          IX1104.2
043400 END-ROUTINE-13.                                                  IX1104.2
043500     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1104.2
043600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1104.2
043700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1104.2
043800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1104.2
043900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1104.2
044000      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1104.2
044100          MOVE "NO " TO ERROR-TOTAL                               IX1104.2
044200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1104.2
044300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1104.2
044400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1104.2
044500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1104.2
044600 WRITE-LINE.                                                      IX1104.2
044700     ADD 1 TO RECORD-COUNT.                                       IX1104.2
044800*    IF RECORD-COUNT GREATER 42                                   IX1104.2
044900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1104.2
045000*        MOVE SPACE TO DUMMY-RECORD                               IX1104.2
045100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1104.2
045200*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1104.2
045300*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1104.2
045400*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1104.2
045500*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1104.2
045600*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1104.2
045700*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1104.2
045800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1104.2
045900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1104.2
046000*        MOVE ZERO TO RECORD-COUNT.                               IX1104.2
046100     PERFORM WRT-LN.                                              IX1104.2
046200 WRT-LN.                                                          IX1104.2
046300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1104.2
046400     MOVE SPACE TO DUMMY-RECORD.                                  IX1104.2
046500 BLANK-LINE-PRINT.                                                IX1104.2
046600     PERFORM WRT-LN.                                              IX1104.2
046700 FAIL-ROUTINE.                                                    IX1104.2
046800     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1104.2
046900            GO TO   FAIL-ROUTINE-WRITE.                           IX1104.2
047000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1104.2
047100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1104.2
047200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1104.2
047300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1104.2
047400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1104.2
047500     GO TO  FAIL-ROUTINE-EX.                                      IX1104.2
047600 FAIL-ROUTINE-WRITE.                                              IX1104.2
047700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1104.2
047800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1104.2
047900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1104.2
048000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1104.2
048100 FAIL-ROUTINE-EX. EXIT.                                           IX1104.2
048200 BAIL-OUT.                                                        IX1104.2
048300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1104.2
048400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1104.2
048500 BAIL-OUT-WRITE.                                                  IX1104.2
048600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1104.2
048700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1104.2
048800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1104.2
048900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1104.2
049000 BAIL-OUT-EX. EXIT.                                               IX1104.2
049100 CCVS1-EXIT.                                                      IX1104.2
049200     EXIT.                                                        IX1104.2
049300                                                                  IX1104.2
049400 SECT-IX110A-0003 SECTION.                                        IX1104.2
049500 SEQ-INIT-010.                                                    IX1104.2
049600                                                                  IX1104.2
049700******************************************************************IX1104.2
049800*   TEST  1                                                      *IX1104.2
049900*         OPEN I-O                        00 EXPECTED            *IX1104.2
050000*         IX-3, 1.3.4 (1) a                                      *IX1104.2
050100*    STATUS 00 CHECK ON OUTPUT FILE IX-FS3                       *IX1104.2
050200*    THE OUTPUT STATEMENT IS SUCCESSFULLY EXECUTED               *IX1104.2
050300******************************************************************IX1104.2
050400 OPN-INIT-GF-01-0.                                                IX1104.2
050500     MOVE 1 TO STATUS-TEST-00.                                    IX1104.2
050600     MOVE SPACES TO IX-FS3-STATUS.                                IX1104.2
050700     MOVE "OPEN I-O:    00 EXP." TO FEATURE.                      IX1104.2
050800     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1104.2
050900     OPEN                                                         IX1104.2
051000        I-O    IX-FS3.                                            IX1104.2
051100     IF IX-FS3-STATUS EQUAL TO "00"                               IX1104.2
051200         GO TO OPN-PASS-GF-01-0.                                  IX1104.2
051300 OPN-FAIL-GF-01-0.                                                IX1104.2
051400     MOVE "IX-3, 1.3.4, (1) a. " TO RE-MARK.                      IX1104.2
051500     PERFORM FAIL.                                                IX1104.2
051600     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1104.2
051700     MOVE "00" TO CORRECT-X.                                      IX1104.2
051800     GO TO OPN-WRITE-GF-01-0.                                     IX1104.2
051900 OPN-PASS-GF-01-0.                                                IX1104.2
052000     PERFORM PASS.                                                IX1104.2
052100 OPN-WRITE-GF-01-0.                                               IX1104.2
052200     PERFORM PRINT-DETAIL.                                        IX1104.2
052300                                                                  IX1104.2
052400******************************************************************IX1104.2
052500*   TEST  2                                                      *IX1104.2
052600*         WRITE     (DUPLICATE PRIME RECORD KEY)  22 EXPECTED    *IX1104.2
052700*         IX-4, 1.3.4 (3) b                                      *IX1104.2
052800*EXISTING KEYS: FROM 000000001 TO 000000050; WRITE: 000000010    *IX1104.2
052900******************************************************************IX1104.2
053000 WRI-INIT-GF-01-0.                                                IX1104.2
053100     MOVE SPACES TO IX-FS3-STATUS.                                IX1104.2
053200     MOVE 0 TO STATUS-TEST-00.                                    IX1104.2
053300     MOVE "WRITE: (DUP) 22 EXP." TO FEATURE.                      IX1104.2
053400     MOVE "WRI-TEST-GF-01-0" TO PAR-NAME.                         IX1104.2
053500 WRI-TEST-GF-01-0.                                                IX1104.2
053600     MOVE SPACES TO IX-FS3-REC-120-240.                           IX1104.2
053700     MOVE "RECORD-KEY000000010END-OF-KEY" TO IX-FS3-KEY.          IX1104.2
053800     WRITE IX-FS3R1-F-G-240.                                      IX1104.2
053900     IF IX-FS3-STATUS      = "22"                                 IX1104.2
054000         MOVE 1 TO STATUS-TEST-00                                 IX1104.2
054100         GO TO WRI-PASS-GF-01-0.                                  IX1104.2
054200 WRI-FAIL-GF-01-0.                                                IX1104.2
054300     MOVE "IX-4, 1.3.4, (3) b. " TO RE-MARK.                      IX1104.2
054400     PERFORM FAIL.                                                IX1104.2
054500     MOVE  IX-FS3-STATUS      TO COMPUTED-A.                      IX1104.2
054600     MOVE "22" TO CORRECT-X.                                      IX1104.2
054700     GO TO WRI-WRITE-GF-01-0.                                     IX1104.2
054800 WRI-PASS-GF-01-0.                                                IX1104.2
054900     PERFORM PASS.                                                IX1104.2
055000 WRI-WRITE-GF-01-0.                                               IX1104.2
055100     PERFORM PRINT-DETAIL.                                        IX1104.2
055200                                                                  IX1104.2
055300******************************************************************IX1104.2
055400*   TEST  3                                                      *IX1104.2
055500*         REWRITE  (DUPLICATE PRIMARY RECORD KEY)   22 EXPECTED  *IX1104.2
055600*         IX-4, 1.3.4 (3) b                                      *IX1104.2
055700*    KEY:        000000049                                       *IX1104.2
055800******************************************************************IX1104.2
055900 RWR-INIT-GF-01-0.                                                IX1104.2
056000     MOVE SPACES TO IX-FS3-STATUS.                                IX1104.2
056100     MOVE ZERO TO STATUS-TEST-00.                                 IX1104.2
056200     MOVE "REWRITE:00 / 22 EXP." TO FEATURE.                      IX1104.2
056300     MOVE "RWR-TEST-GF-01-0" TO PAR-NAME.                         IX1104.2
056400     MOVE "RECORD-KEY000000049END-OF-KEY" TO IX-FS3-KEY.          IX1104.2
056500 RWR-TEST-GF-01-0.                                                IX1104.2
056600     READ IX-FS3    INVALID KEY   GO TO RWR-TEST-GF-01-1.         IX1104.2
056700     MOVE SPACES TO IX-FS3-REC-120-240.                           IX1104.2
056800     MOVE "RECORD-KEY000000039END-OF-KEY" TO IX-FS3-KEY.          IX1104.2
056900     REWRITE IX-FS3R1-F-G-240 INVALID KEY GO TO RWR-TEST-GF-01-1. IX1104.2
057000 RWR-TEST-GF-01-1.                                                IX1104.2
057100     IF IX-FS3-STATUS  = "00"                                     IX1104.2
057200         GO TO RWR-PASS-GF-01-0.                                  IX1104.2
057300     IF IX-FS3-STATUS  = "22"                                     IX1104.2
057400         GO TO RWR-PASS-GF-01-0.                                  IX1104.2
057500 RWR-FAIL-GF-01-0.                                                IX1104.2
057600     MOVE "IX-4, 1.3.4, (3) b. " TO RE-MARK.                      IX1104.2
057700     PERFORM FAIL.                                                IX1104.2
057800     MOVE  IX-FS3-STATUS      TO COMPUTED-A.                      IX1104.2
057900     MOVE "00 OR 22" TO CORRECT-X.                                IX1104.2
058000     GO TO RWR-WRITE-GF-01-0.                                     IX1104.2
058100 RWR-PASS-GF-01-0.                                                IX1104.2
058200     PERFORM PASS.                                                IX1104.2
058300 RWR-WRITE-GF-01-0.                                               IX1104.2
058400     PERFORM PRINT-DETAIL.                                        IX1104.2
058500                                                                  IX1104.2
058600******************************************************************IX1104.2
058700*   TEST  4                                                      *IX1104.2
058800*         READ  (A RECORD THAT DOES NOT EXIST)   23 EXPECTED     *IX1104.2
058900*         IX-4, 1.3.4 (3) c 1)     KEY:   000000100              *IX1104.2
059000******************************************************************IX1104.2
059100 REA-INIT-GF-01-0.                                                IX1104.2
059200     MOVE SPACES TO IX-FS3-STATUS.                                IX1104.2
059300     MOVE "READ:        23 EXP." TO FEATURE.                      IX1104.2
059400     MOVE "REA-TEST-GF-01-0" TO PAR-NAME.                         IX1104.2
059500 REA-TEST-GF-01-0.                                                IX1104.2
059600     MOVE "RECORD-KEY000000100END-OF-KEY" TO IX-FS3-KEY.          IX1104.2
059700     READ  IX-FS3 INVALID KEY GO TO REA-TEST-GF-01-1.             IX1104.2
059800 REA-TEST-GF-01-1.                                                IX1104.2
059900     IF IX-FS3-STATUS = "23"                                      IX1104.2
060000         GO TO REA-PASS-GF-01-0.                                  IX1104.2
060100 REA-FAIL-GF-01-0.                                                IX1104.2
060200     MOVE "IX-3, 1.3.4, (3) c 1)" TO RE-MARK.                     IX1104.2
060300     PERFORM FAIL.                                                IX1104.2
060400     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1104.2
060500     MOVE "23" TO CORRECT-X.                                      IX1104.2
060600     GO TO REA-WRITE-GF-01-0.                                     IX1104.2
060700 REA-PASS-GF-01-0.                                                IX1104.2
060800     PERFORM PASS.                                                IX1104.2
060900 REA-WRITE-GF-01-0.                                               IX1104.2
061000     PERFORM PRINT-DETAIL.                                        IX1104.2
061100                                                                  IX1104.2
061200     CLOSE IX-FS3.                                                IX1104.2
061300                                                                  IX1104.2
061400 TERMINATE-ROUTINE.                                               IX1104.2
061500     EXIT.                                                        IX1104.2
061600                                                                  IX1104.2
061700 CCVS-EXIT SECTION.                                               IX1104.2
061800 CCVS-999999.                                                     IX1104.2
061900     GO TO CLOSE-FILES.                                           IX1104.2
      *END-OF,IX110A                                                                  
