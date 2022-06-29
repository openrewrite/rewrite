      *HEADER,COBOL,IX113A,SUBPRG,IX115A                                              
000100 IDENTIFICATION DIVISION.                                         IX1154.2
000200 PROGRAM-ID.                                                      IX1154.2
000300     IX115A.                                                      IX1154.2
000400****************************************************************  IX1154.2
000500*                                                              *  IX1154.2
000600*    VALIDATION FOR:-                                          *  IX1154.2
000700*                                                              *  IX1154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1154.2
000900*                                                              *  IX1154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1154.2
001100*                                                              *  IX1154.2
001200****************************************************************  IX1154.2
001300*                                                                 IX1154.2
001400*    THIS ROUTINE USES THE MASS STORAGE FILE IX-FS3 CREATED IN    IX1154.2
001500*    IX113A.                                                      IX1154.2
001600*    THE FILE IS OPENED I-O AND THE STATUS CHECKED (00 EXPECTED)  IX1154.2
001700*    THEN CLOSED AND THE STATUS CHECKED AGAIN (00 EXPECTED).  AN  IX1154.2
001800*    ATTEMPT IS THEN MADE TO WRITE A RECORD TO THE CLOSED FILE    IX1154.2
001900*    AT WHICH POINT THE USE AFTER STANDARD EXCEPTION PROCEDURE   *IX1154.2
002000*    STATEMENTS IN THE DECLARATIVES SHOULD BE EXECUTED AND THE    IX1154.2
002100*    FILE STATUS SHOULD BE 48 (IX-5, 1.3.4 (5) G.                 IX1154.2
002200*                                                                 IX1154.2
002300*    4. X-CARDS USED IN THIS PROGRAM:                             IX1154.2
002400*                                                                 IX1154.2
002500*                 XXXXX024                                        IX1154.2
002600*                 XXXXX055.                                       IX1154.2
002700*         P       XXXXX062.                                       IX1154.2
002800*                 XXXXX082.                                       IX1154.2
002900*                 XXXXX083.                                       IX1154.2
003000*         C       XXXXX084                                        IX1154.2
003100*                                                                 IX1154.2
003200*                                                                 IX1154.2
003300 ENVIRONMENT DIVISION.                                            IX1154.2
003400 CONFIGURATION SECTION.                                           IX1154.2
003500 SOURCE-COMPUTER.                                                 IX1154.2
003600     XXXXX082.                                                    IX1154.2
003700 OBJECT-COMPUTER.                                                 IX1154.2
003800     XXXXX083.                                                    IX1154.2
003900 INPUT-OUTPUT SECTION.                                            IX1154.2
004000 FILE-CONTROL.                                                    IX1154.2
004100*    SELECT RAW-DATA   ASSIGN TO                                  IX1154.2
004200*    XXXXX062                                                     IX1154.2
004300*           ORGANIZATION IS INDEXED                               IX1154.2
004400*           ACCESS MODE IS RANDOM                                 IX1154.2
004500*           RECORD KEY IS RAW-DATA-KEY.                           IX1154.2
004600*                                                                 IX1154.2
004700     SELECT PRINT-FILE ASSIGN TO                                  IX1154.2
004800     XXXXX055.                                                    IX1154.2
004900*                                                                 IX1154.2
005000     SELECT IX-FS3 ASSIGN                                         IX1154.2
005100     XXXXX024                                                     IX1154.2
005200     ORGANIZATION IS INDEXED                                      IX1154.2
005300     ACCESS MODE IS SEQUENTIAL                                    IX1154.2
005400     RECORD KEY IS IX-FS3-KEY                                     IX1154.2
005500     FILE STATUS IS IX-FS3-STATUS.                                IX1154.2
005600                                                                  IX1154.2
005700 DATA DIVISION.                                                   IX1154.2
005800                                                                  IX1154.2
005900 FILE SECTION.                                                    IX1154.2
006000*                                                                 IX1154.2
006100*FD  RAW-DATA.                                                    IX1154.2
006200*                                                                 IX1154.2
006300*01  RAW-DATA-SATZ.                                               IX1154.2
006400*    05  RAW-DATA-KEY        PIC X(6).                            IX1154.2
006500*    05  C-DATE              PIC 9(6).                            IX1154.2
006600*    05  C-TIME              PIC 9(8).                            IX1154.2
006700*    05  C-NO-OF-TESTS       PIC 99.                              IX1154.2
006800*    05  C-OK                PIC 999.                             IX1154.2
006900*    05  C-ALL               PIC 999.                             IX1154.2
007000*    05  C-FAIL              PIC 999.                             IX1154.2
007100*    05  C-DELETED           PIC 999.                             IX1154.2
007200*    05  C-INSPECT           PIC 999.                             IX1154.2
007300*    05  C-NOTE              PIC X(13).                           IX1154.2
007400*    05  C-INDENT            PIC X.                               IX1154.2
007500*    05  C-ABORT             PIC X(8).                            IX1154.2
007600                                                                  IX1154.2
007700 FD  PRINT-FILE.                                                  IX1154.2
007800                                                                  IX1154.2
007900 01  PRINT-REC               PIC X(120).                          IX1154.2
008000                                                                  IX1154.2
008100 01  DUMMY-RECORD            PIC X(120).                          IX1154.2
008200                                                                  IX1154.2
008300 FD  IX-FS3                                                       IX1154.2
008400C       DATA RECORDS IX-FS3R1-F-G-240                             IX1154.2
008500C       LABEL RECORD STANDARD                                     IX1154.2
008600        RECORD 240                                                IX1154.2
008700        BLOCK CONTAINS 2 RECORDS.                                 IX1154.2
008800                                                                  IX1154.2
008900 01  IX-FS3R1-F-G-240.                                            IX1154.2
009000     05  IX-FS3-REC-120      PIC X(120).                          IX1154.2
009100     05  IX-FS3-REC-120-240.                                      IX1154.2
009200         10  FILLER          PIC X(8).                            IX1154.2
009300         10  IX-FS3-KEY      PIC X(29).                           IX1154.2
009400         10  FILLER          PIC X(9).                            IX1154.2
009500         10  IX-FS3-ALTER-KEY  PIC X(29).                         IX1154.2
009600         10  FILLER            PIC X(45).                         IX1154.2
009700                                                                  IX1154.2
009800                                                                  IX1154.2
009900 WORKING-STORAGE SECTION.                                         IX1154.2
010000                                                                  IX1154.2
010100 01  GRP-0101.                                                    IX1154.2
010200     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1154.2
010300     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1154.2
010400     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1154.2
010500                                                                  IX1154.2
010600 01  GRP-0102.                                                    IX1154.2
010700     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1154.2
010800     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1154.2
010900     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1154.2
011000                                                                  IX1154.2
011100 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1154.2
011200                                                                  IX1154.2
011300 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1154.2
011400                                                                  IX1154.2
011500 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1154.2
011600                                                                  IX1154.2
011700 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1154.2
011800                                                                  IX1154.2
011900 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1154.2
012000                                                                  IX1154.2
012100 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1154.2
012200                                                                  IX1154.2
012300 01  STATUS-TEST-10          PIC 9 VALUE ZERO.                    IX1154.2
012400 01  STATUS-TEST-READ        PIC 9 VALUE ZERO.                    IX1154.2
012500                                                                  IX1154.2
012600 01  IX-FS3-STATUS.                                               IX1154.2
012700     05  IX-FS3-STAT1        PIC X.                               IX1154.2
012800     05  IX-FS3-STAT2        PIC X.                               IX1154.2
012900                                                                  IX1154.2
013000 01  COUNT-OF-RECS           PIC 9(5).                            IX1154.2
013100                                                                  IX1154.2
013200 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1154.2
013300                                                                  IX1154.2
013400 01  FILE-RECORD-INFORMATION-REC.                                 IX1154.2
013500     05  FILE-RECORD-INFO-SKELETON.                               IX1154.2
013600         10  FILLER          PIC X(48) VALUE                      IX1154.2
013700              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1154.2
013800         10  FILLER          PIC X(46) VALUE                      IX1154.2
013900                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1154.2
014000         10  FILLER          PIC X(26) VALUE                      IX1154.2
014100                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1154.2
014200         10  FILLER          PIC X(37) VALUE                      IX1154.2
014300                         ",RECKEY=                             ". IX1154.2
014400         10  FILLER          PIC X(38) VALUE                      IX1154.2
014500                        ",ALTKEY1=                             ". IX1154.2
014600         10  FILLER          PIC X(38) VALUE                      IX1154.2
014700                        ",ALTKEY2=                             ". IX1154.2
014800         10  FILLER          PIC X(7) VALUE SPACE.                IX1154.2
014900     05  FILE-RECORD-INFO             OCCURS 10.                  IX1154.2
015000         10  FILE-RECORD-INFO-P1-120.                             IX1154.2
015100             15  FILLER      PIC X(5).                            IX1154.2
015200             15  XFILE-NAME  PIC X(6).                            IX1154.2
015300             15  FILLER      PIC X(8).                            IX1154.2
015400             15  XRECORD-NAME  PIC X(6).                          IX1154.2
015500             15  FILLER        PIC X(1).                          IX1154.2
015600             15  REELUNIT-NUMBER  PIC 9(1).                       IX1154.2
015700             15  FILLER           PIC X(7).                       IX1154.2
015800             15  XRECORD-NUMBER   PIC 9(6).                       IX1154.2
015900             15  FILLER           PIC X(6).                       IX1154.2
016000             15  UPDATE-NUMBER    PIC 9(2).                       IX1154.2
016100             15  FILLER           PIC X(5).                       IX1154.2
016200             15  ODO-NUMBER       PIC 9(4).                       IX1154.2
016300             15  FILLER           PIC X(5).                       IX1154.2
016400             15  XPROGRAM-NAME    PIC X(5).                       IX1154.2
016500             15  FILLER           PIC X(7).                       IX1154.2
016600             15  XRECORD-LENGTH   PIC 9(6).                       IX1154.2
016700             15  FILLER           PIC X(7).                       IX1154.2
016800             15  CHARS-OR-RECORDS  PIC X(2).                      IX1154.2
016900             15  FILLER            PIC X(1).                      IX1154.2
017000             15  XBLOCK-SIZE       PIC 9(4).                      IX1154.2
017100             15  FILLER            PIC X(6).                      IX1154.2
017200             15  RECORDS-IN-FILE   PIC 9(6).                      IX1154.2
017300             15  FILLER            PIC X(5).                      IX1154.2
017400             15  XFILE-ORGANIZATION  PIC X(2).                    IX1154.2
017500             15  FILLER              PIC X(6).                    IX1154.2
017600             15  XLABEL-TYPE         PIC X(1).                    IX1154.2
017700         10  FILE-RECORD-INFO-P121-240.                           IX1154.2
017800             15  FILLER              PIC X(8).                    IX1154.2
017900             15  XRECORD-KEY         PIC X(29).                   IX1154.2
018000             15  FILLER              PIC X(9).                    IX1154.2
018100             15  ALTERNATE-KEY1      PIC X(29).                   IX1154.2
018200             15  FILLER              PIC X(9).                    IX1154.2
018300             15  ALTERNATE-KEY2      PIC X(29).                   IX1154.2
018400             15  FILLER              PIC X(7).                    IX1154.2
018500                                                                  IX1154.2
018600 01  TEST-RESULTS.                                                IX1154.2
018700     02 FILLER                   PIC X      VALUE SPACE.          IX1154.2
018800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1154.2
018900     02 FILLER                   PIC X      VALUE SPACE.          IX1154.2
019000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1154.2
019100     02 FILLER                   PIC X      VALUE SPACE.          IX1154.2
019200     02  PAR-NAME.                                                IX1154.2
019300       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1154.2
019400       03  PARDOT-X              PIC X      VALUE SPACE.          IX1154.2
019500       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1154.2
019600     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1154.2
019700     02 RE-MARK                  PIC X(61).                       IX1154.2
019800 01  TEST-COMPUTED.                                               IX1154.2
019900     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1154.2
020000     02 FILLER                   PIC X(17)  VALUE                 IX1154.2
020100            "       COMPUTED=".                                   IX1154.2
020200     02 COMPUTED-X.                                               IX1154.2
020300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1154.2
020400     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1154.2
020500                                 PIC -9(9).9(9).                  IX1154.2
020600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1154.2
020700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1154.2
020800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1154.2
020900     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1154.2
021000         04 COMPUTED-18V0                    PIC -9(18).          IX1154.2
021100         04 FILLER                           PIC X.               IX1154.2
021200     03 FILLER PIC X(50) VALUE SPACE.                             IX1154.2
021300 01  TEST-CORRECT.                                                IX1154.2
021400     02 FILLER PIC X(30) VALUE SPACE.                             IX1154.2
021500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1154.2
021600     02 CORRECT-X.                                                IX1154.2
021700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1154.2
021800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1154.2
021900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1154.2
022000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1154.2
022100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1154.2
022200     03      CR-18V0 REDEFINES CORRECT-A.                         IX1154.2
022300         04 CORRECT-18V0                     PIC -9(18).          IX1154.2
022400         04 FILLER                           PIC X.               IX1154.2
022500     03 FILLER PIC X(2) VALUE SPACE.                              IX1154.2
022600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1154.2
022700 01  CCVS-C-1.                                                    IX1154.2
022800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1154.2
022900-    "SS  PARAGRAPH-NAME                                          IX1154.2
023000-    "       REMARKS".                                            IX1154.2
023100     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1154.2
023200 01  CCVS-C-2.                                                    IX1154.2
023300     02 FILLER                     PIC X        VALUE SPACE.      IX1154.2
023400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1154.2
023500     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1154.2
023600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1154.2
023700     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1154.2
023800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1154.2
023900 01  REC-CT                        PIC 99       VALUE ZERO.       IX1154.2
024000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1154.2
024100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1154.2
024200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1154.2
024300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1154.2
024400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1154.2
024500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1154.2
024600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1154.2
024700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1154.2
024800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1154.2
024900 01  CCVS-H-1.                                                    IX1154.2
025000     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1154.2
025100     02  FILLER                    PIC X(42)    VALUE             IX1154.2
025200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1154.2
025300     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1154.2
025400 01  CCVS-H-2A.                                                   IX1154.2
025500   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1154.2
025600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1154.2
025700   02  FILLER                        PIC XXXX   VALUE             IX1154.2
025800     "4.2 ".                                                      IX1154.2
025900   02  FILLER                        PIC X(28)  VALUE             IX1154.2
026000            " COPY - NOT FOR DISTRIBUTION".                       IX1154.2
026100   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1154.2
026200                                                                  IX1154.2
026300 01  CCVS-H-2B.                                                   IX1154.2
026400   02  FILLER                        PIC X(15)  VALUE             IX1154.2
026500            "TEST RESULT OF ".                                    IX1154.2
026600   02  TEST-ID                       PIC X(9).                    IX1154.2
026700   02  FILLER                        PIC X(4)   VALUE             IX1154.2
026800            " IN ".                                               IX1154.2
026900   02  FILLER                        PIC X(12)  VALUE             IX1154.2
027000     " HIGH       ".                                              IX1154.2
027100   02  FILLER                        PIC X(22)  VALUE             IX1154.2
027200            " LEVEL VALIDATION FOR ".                             IX1154.2
027300   02  FILLER                        PIC X(58)  VALUE             IX1154.2
027400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1154.2
027500 01  CCVS-H-3.                                                    IX1154.2
027600     02  FILLER                      PIC X(34)  VALUE             IX1154.2
027700            " FOR OFFICIAL USE ONLY    ".                         IX1154.2
027800     02  FILLER                      PIC X(58)  VALUE             IX1154.2
027900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1154.2
028000     02  FILLER                      PIC X(28)  VALUE             IX1154.2
028100            "  COPYRIGHT   1985 ".                                IX1154.2
028200 01  CCVS-E-1.                                                    IX1154.2
028300     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1154.2
028400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1154.2
028500     02 ID-AGAIN                     PIC X(9).                    IX1154.2
028600     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1154.2
028700 01  CCVS-E-2.                                                    IX1154.2
028800     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1154.2
028900     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1154.2
029000     02 CCVS-E-2-2.                                               IX1154.2
029100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1154.2
029200         03 FILLER                   PIC X      VALUE SPACE.      IX1154.2
029300         03 ENDER-DESC               PIC X(44)  VALUE             IX1154.2
029400            "ERRORS ENCOUNTERED".                                 IX1154.2
029500 01  CCVS-E-3.                                                    IX1154.2
029600     02  FILLER                      PIC X(22)  VALUE             IX1154.2
029700            " FOR OFFICIAL USE ONLY".                             IX1154.2
029800     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1154.2
029900     02  FILLER                      PIC X(58)  VALUE             IX1154.2
030000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1154.2
030100     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1154.2
030200     02 FILLER                       PIC X(15)  VALUE             IX1154.2
030300             " COPYRIGHT 1985".                                   IX1154.2
030400 01  CCVS-E-4.                                                    IX1154.2
030500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1154.2
030600     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1154.2
030700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1154.2
030800     02 FILLER                       PIC X(40)  VALUE             IX1154.2
030900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1154.2
031000 01  XXINFO.                                                      IX1154.2
031100     02 FILLER                       PIC X(19)  VALUE             IX1154.2
031200            "*** INFORMATION ***".                                IX1154.2
031300     02 INFO-TEXT.                                                IX1154.2
031400       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1154.2
031500       04 XXCOMPUTED                 PIC X(20).                   IX1154.2
031600       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1154.2
031700       04 XXCORRECT                  PIC X(20).                   IX1154.2
031800     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1154.2
031900 01  HYPHEN-LINE.                                                 IX1154.2
032000     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1154.2
032100     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1154.2
032200-    "*****************************************".                 IX1154.2
032300     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1154.2
032400-    "******************************".                            IX1154.2
032500 01  TEST-NO                         PIC 99.                      IX1154.2
032600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1154.2
032700     "IX115A".                                                    IX1154.2
032800 PROCEDURE DIVISION.                                              IX1154.2
032900 DECLARATIVES.                                                    IX1154.2
033000                                                                  IX1154.2
033100 SECT-IX105-0002 SECTION.                                         IX1154.2
033200     USE AFTER EXCEPTION PROCEDURE ON IX-FS3.                     IX1154.2
033300 INPUT-PROCESS.                                                   IX1154.2
033400     IF TEST-NO = 5                                               IX1154.2
033500        GO TO D-C-TEST-GF-01-1.                                   IX1154.2
033600     IF STATUS-TEST-10 EQUAL TO 1                                 IX1154.2
033700        IF  IX-FS3-STAT1 EQUAL TO "1"                             IX1154.2
033800            MOVE 1 TO EOF-FLAG                                    IX1154.2
033900        ELSE                                                      IX1154.2
034000           IF  IX-FS3-STAT1 GREATER THAN "1"                      IX1154.2
034100           MOVE 1 TO PERM-ERRORS.                                 IX1154.2
034200     GO TO DECL-EXIT.                                             IX1154.2
034300 D-C-TEST-GF-01-1.                                                IX1154.2
034400     IF IX-FS3-STATUS EQUAL TO "48"                               IX1154.2
034500         GO TO D-C-PASS-GF-01-0.                                  IX1154.2
034600 D-C-FAIL-GF-01-0.                                                IX1154.2
034700     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1154.2
034800     MOVE "48" TO CORRECT-X.                                      IX1154.2
034900     MOVE "IX-5, 1.3.4, (5) G" TO RE-MARK.                        IX1154.2
035000     PERFORM D-FAIL.                                              IX1154.2
035100     GO TO D-C-WRITE-GF-01-0.                                     IX1154.2
035200 D-C-PASS-GF-01-0.                                                IX1154.2
035300     PERFORM D-PASS.                                              IX1154.2
035400 D-C-WRITE-GF-01-0.                                               IX1154.2
035500     PERFORM D-PRINT-DETAIL.                                      IX1154.2
035600 D-CLOSE-FILES.                                                   IX1154.2
035700*    OPEN I-O RAW-DATA.                                           IX1154.2
035800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1154.2
035900*    READ RAW-DATA INVALID KEY GO TO D-END-E-2.                   IX1154.2
036000*    MOVE "OK.     " TO C-ABORT.                                  IX1154.2
036100*    MOVE PASS-COUNTER TO C-OK.                                   IX1154.2
036200*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1154.2
036300*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1154.2
036400*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1154.2
036500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1154.2
036600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO D-END-E-2.           IX1154.2
036700*D-END-E-2.                                                       IX1154.2
036800*    CLOSE RAW-DATA.                                              IX1154.2
036900     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 IX1154.2
037000     CLOSE PRINT-FILE.                                            IX1154.2
037100 D-TERMINATE-CCVS.                                                IX1154.2
037200*    EXIT PROGRAM.                                                IX1154.2
037300*D-TERMINATE-CALL.                                                IX1154.2
037400     STOP     RUN.                                                IX1154.2
037500 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         IX1154.2
037600 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        IX1154.2
037700 D-PRINT-DETAIL.                                                  IX1154.2
037800     IF   REC-CT NOT EQUAL TO ZERO                                IX1154.2
037900          MOVE "." TO PARDOT-X                                    IX1154.2
038000          MOVE REC-CT TO DOTVALUE.                                IX1154.2
038100     MOVE TEST-RESULTS TO PRINT-REC.                              IX1154.2
038200     PERFORM D-WRITE-LINE.                                        IX1154.2
038300     IF   P-OR-F EQUAL TO "FAIL*"                                 IX1154.2
038400          PERFORM D-WRITE-LINE                                    IX1154.2
038500          PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX           IX1154.2
038600     ELSE                                                         IX1154.2
038700          PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.                  IX1154.2
038800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1154.2
038900     MOVE SPACE TO CORRECT-X.                                     IX1154.2
039000     IF   REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.           IX1154.2
039100     MOVE SPACE TO RE-MARK.                                       IX1154.2
039200 D-END-ROUTINE.                                                   IX1154.2
039300     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            IX1154.2
039400     PERFORM D-WRITE-LINE 5 TIMES.                                IX1154.2
039500 D-END-RTN-EXIT.                                                  IX1154.2
039600     MOVE CCVS-E-1 TO DUMMY-RECORD.                               IX1154.2
039700     PERFORM D-WRITE-LINE 2 TIMES.                                IX1154.2
039800 D-END-ROUTINE-1.                                                 IX1154.2
039900     ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO       IX1154.2
040000     ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.                IX1154.2
040100     ADD PASS-COUNTER TO ERROR-HOLD.                              IX1154.2
040200     MOVE PASS-COUNTER TO CCVS-E-4-1.                             IX1154.2
040300     MOVE ERROR-HOLD TO CCVS-E-4-2.                               IX1154.2
040400     MOVE CCVS-E-4 TO CCVS-E-2-2.                                 IX1154.2
040500     MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.          IX1154.2
040600  D-END-ROUTINE-12.                                               IX1154.2
040700     MOVE "TEST(S) FAILED" TO ENDER-DESC.                         IX1154.2
040800     IF  ERROR-COUNTER IS EQUAL TO ZERO                           IX1154.2
040900         MOVE "NO " TO ERROR-TOTAL                                IX1154.2
041000     ELSE                                                         IX1154.2
041100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1154.2
041200     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            IX1154.2
041300     PERFORM D-WRITE-LINE.                                        IX1154.2
041400 D-END-ROUTINE-13.                                                IX1154.2
041500     IF  DELETE-COUNTER IS EQUAL TO ZERO                          IX1154.2
041600         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1154.2
041700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1154.2
041800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1154.2
041900     MOVE CCVS-E-2 TO DUMMY-RECORD.                               IX1154.2
042000     PERFORM D-WRITE-LINE.                                        IX1154.2
042100     IF   INSPECT-COUNTER EQUAL TO ZERO                           IX1154.2
042200          MOVE "NO " TO ERROR-TOTAL                               IX1154.2
042300     ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                    IX1154.2
042400     MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.             IX1154.2
042500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1154.2
042600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1154.2
042700 D-WRITE-LINE.                                                    IX1154.2
042800     ADD 1 TO RECORD-COUNT.                                       IX1154.2
042900*    IF RECORD-COUNT GREATER 42                                   IX1154.2
043000*       MOVE DUMMY-RECORD TO DUMMY-HOLD                           IX1154.2
043100*       MOVE SPACE TO DUMMY-RECORD                                IX1154.2
043200*       WRITE DUMMY-RECORD AFTER ADVANCING PAGE                   IX1154.2
043300*       MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1154.2
043400*       MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1154.2
043500*       MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1154.2
043600*       MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1154.2
043700*       MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1154.2
043800*       MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1154.2
043900*       MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN         IX1154.2
044000*       MOVE DUMMY-HOLD TO DUMMY-RECORD                           IX1154.2
044100*       MOVE ZERO TO RECORD-COUNT.                                IX1154.2
044200     PERFORM D-WRT-LN.                                            IX1154.2
044300 D-WRT-LN.                                                        IX1154.2
044400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1154.2
044500     MOVE SPACE TO DUMMY-RECORD.                                  IX1154.2
044600 D-FAIL-ROUTINE.                                                  IX1154.2
044700     IF   COMPUTED-X NOT EQUAL TO SPACE                           IX1154.2
044800          GO TO D-FAIL-ROUTINE-WRITE.                             IX1154.2
044900     IF   CORRECT-X NOT EQUAL TO SPACE GO TO D-FAIL-ROUTINE-WRITE.IX1154.2
045000     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1154.2
045100     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    IX1154.2
045200     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1154.2
045300     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1154.2
045400     GO TO D-FAIL-ROUTINE-EX.                                     IX1154.2
045500 D-FAIL-ROUTINE-WRITE.                                            IX1154.2
045600     MOVE TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE         IX1154.2
045700     MOVE ANSI-REFERENCE TO COR-ANSI-REFERENCE.                   IX1154.2
045800     MOVE TEST-CORRECT TO PRINT-REC PERFORM D-WRITE-LINE 2 TIMES. IX1154.2
045900     MOVE SPACES TO COR-ANSI-REFERENCE.                           IX1154.2
046000 D-FAIL-ROUTINE-EX. EXIT.                                         IX1154.2
046100 D-BAIL-OUT.                                                      IX1154.2
046200     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    IX1154.2
046300     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            IX1154.2
046400 D-BAIL-OUT-WRITE.                                                IX1154.2
046500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1154.2
046600     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1154.2
046700     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1154.2
046800     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1154.2
046900 D-BAIL-OUT-EX. EXIT.                                             IX1154.2
047000 DECL-EXIT.  EXIT.                                                IX1154.2
047100 END DECLARATIVES.                                                IX1154.2
047200                                                                  IX1154.2
047300                                                                  IX1154.2
047400 CCVS1 SECTION.                                                   IX1154.2
047500 OPEN-FILES.                                                      IX1154.2
047600*    OPEN I-O RAW-DATA.                                           IX1154.2
047700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1154.2
047800*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1154.2
047900*    MOVE "ABORTED " TO C-ABORT.                                  IX1154.2
048000*    ADD 1 TO C-NO-OF-TESTS.                                      IX1154.2
048100*    ACCEPT C-DATE  FROM DATE.                                    IX1154.2
048200*    ACCEPT C-TIME  FROM TIME.                                    IX1154.2
048300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1154.2
048400*END-E-1.                                                         IX1154.2
048500*    CLOSE RAW-DATA.                                              IX1154.2
048600     OPEN    OUTPUT PRINT-FILE.                                   IX1154.2
048700     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1154.2
048800     MOVE    SPACE TO TEST-RESULTS.                               IX1154.2
048900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1154.2
049000     MOVE    ZERO TO REC-SKL-SUB.                                 IX1154.2
049100     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1154.2
049200 CCVS-INIT-FILE.                                                  IX1154.2
049300     ADD     1 TO REC-SKL-SUB.                                    IX1154.2
049400     MOVE    FILE-RECORD-INFO-SKELETON                            IX1154.2
049500          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1154.2
049600 CCVS-INIT-EXIT.                                                  IX1154.2
049700     GO TO CCVS1-EXIT.                                            IX1154.2
049800 CLOSE-FILES.                                                     IX1154.2
049900*    OPEN I-O RAW-DATA.                                           IX1154.2
050000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1154.2
050100*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1154.2
050200*    MOVE "OK.     " TO C-ABORT.                                  IX1154.2
050300*    MOVE PASS-COUNTER TO C-OK.                                   IX1154.2
050400*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1154.2
050500*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1154.2
050600*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1154.2
050700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1154.2
050800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1154.2
050900*END-E-2.                                                         IX1154.2
051000*    CLOSE RAW-DATA.                                              IX1154.2
051100     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1154.2
051200 TERMINATE-CCVS.                                                  IX1154.2
051300*    EXIT PROGRAM.                                                IX1154.2
051400*TERMINATE-CALL.                                                  IX1154.2
051500     STOP     RUN.                                                IX1154.2
051600 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1154.2
051700 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1154.2
051800 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1154.2
051900 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1154.2
052000     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1154.2
052100 PRINT-DETAIL.                                                    IX1154.2
052200     IF REC-CT NOT EQUAL TO ZERO                                  IX1154.2
052300             MOVE "." TO PARDOT-X                                 IX1154.2
052400             MOVE REC-CT TO DOTVALUE.                             IX1154.2
052500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1154.2
052600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1154.2
052700        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1154.2
052800          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1154.2
052900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1154.2
053000     MOVE SPACE TO CORRECT-X.                                     IX1154.2
053100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1154.2
053200     MOVE     SPACE TO RE-MARK.                                   IX1154.2
053300 HEAD-ROUTINE.                                                    IX1154.2
053400     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1154.2
053500     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1154.2
053600     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1154.2
053700     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1154.2
053800 COLUMN-NAMES-ROUTINE.                                            IX1154.2
053900     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1154.2
054000     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1154.2
054100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1154.2
054200 END-ROUTINE.                                                     IX1154.2
054300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1154.2
054400 END-RTN-EXIT.                                                    IX1154.2
054500     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1154.2
054600 END-ROUTINE-1.                                                   IX1154.2
054700      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1154.2
054800      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1154.2
054900      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1154.2
055000*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1154.2
055100      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1154.2
055200      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1154.2
055300      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1154.2
055400      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1154.2
055500  END-ROUTINE-12.                                                 IX1154.2
055600      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1154.2
055700     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1154.2
055800         MOVE "NO " TO ERROR-TOTAL                                IX1154.2
055900         ELSE                                                     IX1154.2
056000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1154.2
056100     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1154.2
056200     PERFORM WRITE-LINE.                                          IX1154.2
056300 END-ROUTINE-13.                                                  IX1154.2
056400     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1154.2
056500         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1154.2
056600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1154.2
056700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1154.2
056800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1154.2
056900      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1154.2
057000          MOVE "NO " TO ERROR-TOTAL                               IX1154.2
057100      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1154.2
057200      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1154.2
057300      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1154.2
057400     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1154.2
057500 WRITE-LINE.                                                      IX1154.2
057600     ADD 1 TO RECORD-COUNT.                                       IX1154.2
057700*    IF RECORD-COUNT GREATER 42                                   IX1154.2
057800*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1154.2
057900*        MOVE SPACE TO DUMMY-RECORD                               IX1154.2
058000*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1154.2
058100*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1154.2
058200*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1154.2
058300*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1154.2
058400*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1154.2
058500*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1154.2
058600*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1154.2
058700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1154.2
058800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1154.2
058900*        MOVE ZERO TO RECORD-COUNT.                               IX1154.2
059000     PERFORM WRT-LN.                                              IX1154.2
059100 WRT-LN.                                                          IX1154.2
059200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1154.2
059300     MOVE SPACE TO DUMMY-RECORD.                                  IX1154.2
059400 BLANK-LINE-PRINT.                                                IX1154.2
059500     PERFORM WRT-LN.                                              IX1154.2
059600 FAIL-ROUTINE.                                                    IX1154.2
059700     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1154.2
059800            GO TO   FAIL-ROUTINE-WRITE.                           IX1154.2
059900     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1154.2
060000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1154.2
060100     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1154.2
060200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1154.2
060300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1154.2
060400     GO TO  FAIL-ROUTINE-EX.                                      IX1154.2
060500 FAIL-ROUTINE-WRITE.                                              IX1154.2
060600     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1154.2
060700     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1154.2
060800     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1154.2
060900     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1154.2
061000 FAIL-ROUTINE-EX. EXIT.                                           IX1154.2
061100 BAIL-OUT.                                                        IX1154.2
061200     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1154.2
061300     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1154.2
061400 BAIL-OUT-WRITE.                                                  IX1154.2
061500     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1154.2
061600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1154.2
061700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1154.2
061800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1154.2
061900 BAIL-OUT-EX. EXIT.                                               IX1154.2
062000 CCVS1-EXIT.                                                      IX1154.2
062100     EXIT.                                                        IX1154.2
062200                                                                  IX1154.2
062300 SECT-IX115A-0003 SECTION.                                        IX1154.2
062400 SEQ-INIT-010.                                                    IX1154.2
062500     MOVE ZERO TO TEST-NO.                                        IX1154.2
062600     MOVE "IX-FS3" TO XFILE-NAME (1).                             IX1154.2
062700     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IX1154.2
062800     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IX1154.2
062900     MOVE 000240 TO XRECORD-LENGTH (1).                           IX1154.2
063000     MOVE "RC" TO CHARS-OR-RECORDS (1).                           IX1154.2
063100     MOVE 0002 TO XBLOCK-SIZE (1).                                IX1154.2
063200     MOVE 000050 TO RECORDS-IN-FILE (1).                          IX1154.2
063300     MOVE "IX" TO XFILE-ORGANIZATION (1).                         IX1154.2
063400     MOVE "S" TO XLABEL-TYPE (1).                                 IX1154.2
063500     MOVE 000001 TO XRECORD-NUMBER (1).                           IX1154.2
063600     MOVE 0 TO COUNT-OF-RECS.                                     IX1154.2
063700                                                                  IX1154.2
063800******************************************************************IX1154.2
063900*   TEST  1                                                      *IX1154.2
064000*         OPEN I-O                                                IX1154.2
064100*         IX-3, 1.3.4 (1) A                                      *IX1154.2
064200*    STATUS 00 CHECK ON OUTPUT FILE IX-FS3                       *IX1154.2
064300*    THE OPEN   STATEMENT IS SUCCESSFULLY EXECUTED               *IX1154.2
064400******************************************************************IX1154.2
064500 OPN-INIT-GF-01-0.                                                IX1154.2
064600     MOVE 1 TO STATUS-TEST-00.                                    IX1154.2
064700     MOVE SPACES TO IX-FS3-STATUS.                                IX1154.2
064800     MOVE "OPEN I-O   : 00 EXP." TO FEATURE.                      IX1154.2
064900     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1154.2
065000     OPEN                                                         IX1154.2
065100        I-O    IX-FS3.                                            IX1154.2
065200     IF IX-FS3-STATUS EQUAL TO "00"                               IX1154.2
065300         GO TO OPN-PASS-GF-01-0.                                  IX1154.2
065400 OPN-FAIL-GF-01-0.                                                IX1154.2
065500     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1154.2
065600     PERFORM FAIL.                                                IX1154.2
065700     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1154.2
065800     MOVE "00" TO CORRECT-X.                                      IX1154.2
065900     GO TO OPN-WRITE-GF-01-0.                                     IX1154.2
066000 OPN-PASS-GF-01-0.                                                IX1154.2
066100     PERFORM PASS.                                                IX1154.2
066200 OPN-WRITE-GF-01-0.                                               IX1154.2
066300     PERFORM PRINT-DETAIL.                                        IX1154.2
066400******************************************************************IX1154.2
066500*   TEST  4                                                      *IX1154.2
066600*         CLOSE I-O                       00 EXPECTED            *IX1154.2
066700*         IX-3, 1.3.4 (1) A                                      *IX1154.2
066800******************************************************************IX1154.2
066900 CLO-INIT-GF-01-0.                                                IX1154.2
067000     MOVE SPACES TO IX-FS3-STATUS.                                IX1154.2
067100     MOVE "CLOSE I-O   :00 EXP." TO FEATURE.                      IX1154.2
067200     MOVE "CLO-TEST-GF-01-0" TO PAR-NAME.                         IX1154.2
067300 CLO-TEST-GF-01-0.                                                IX1154.2
067400     CLOSE IX-FS3.                                                IX1154.2
067500     IF IX-FS3-STATUS = "00"                                      IX1154.2
067600         GO TO CLO-PASS-GF-01-0.                                  IX1154.2
067700 CLO-FAIL-GF-01-0.                                                IX1154.2
067800     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1154.2
067900     PERFORM FAIL.                                                IX1154.2
068000     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1154.2
068100     MOVE "00" TO CORRECT-X.                                      IX1154.2
068200     GO TO CLO-WRITE-GF-01-0.                                     IX1154.2
068300 CLO-PASS-GF-01-0.                                                IX1154.2
068400     PERFORM PASS.                                                IX1154.2
068500 CLO-WRITE-GF-01-0.                                               IX1154.2
068600     PERFORM PRINT-DETAIL.                                        IX1154.2
068700                                                                  IX1154.2
068800******************************************************************IX1154.2
068900*    A INDEXED FILE WITH 50 RECORDS HAS BEEN CREATED.            *IX1154.2
069000******************************************************************IX1154.2
069100                                                                  IX1154.2
069200******************************************************************IX1154.2
069300*   TEST  5                                                      *IX1154.2
069400*         WRITE...  A FILE NOT IN THE OPEN MODE                  *IX1154.2
069500*         FILE STATUS 48 EXPECTED IX-5, 1.3.4 (5) G              *IX1154.2
069600******************************************************************IX1154.2
069700 WRI-TEST-GF-01-0.                                                IX1154.2
069800     MOVE  5 TO TEST-NO.                                          IX1154.2
069900     MOVE SPACES TO IX-FS3-STATUS.                                IX1154.2
070000     MOVE "WRITE.       48 EXP." TO FEATURE                       IX1154.2
070100     MOVE "WRI-TEST-GF-01-0" TO PAR-NAME.                         IX1154.2
070200     WRITE IX-FS3R1-F-G-240.                                      IX1154.2
070300 WRI-TEST-GF-01-1.                                                IX1154.2
070400     IF IX-FS3-STATUS EQUAL TO "48"                               IX1154.2
070500        MOVE "SHOULD HAVE EXECUTED DECLARATIVES IX-3,1.3.4(4)"    IX1154.2
070600          TO RE-MARK                                              IX1154.2
070700        GO TO WRI-WRITE-GF-01-0.                                  IX1154.2
070800 WRI-FAIL-GF-01-0.                                                IX1154.2
070900     MOVE "IX-5, 1.3.4, (5) G" TO RE-MARK.                        IX1154.2
071000 WRI-WRITE-GF-01-0.                                               IX1154.2
071100     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1154.2
071200     MOVE "48" TO CORRECT-X.                                      IX1154.2
071300     PERFORM FAIL.                                                IX1154.2
071400     PERFORM PRINT-DETAIL.                                        IX1154.2
071500                                                                  IX1154.2
071600 TERMINATE-ROUTINE.                                               IX1154.2
071700     EXIT.                                                        IX1154.2
071800                                                                  IX1154.2
071900 CCVS-EXIT SECTION.                                               IX1154.2
072000 CCVS-999999.                                                     IX1154.2
072100     GO TO CLOSE-FILES.                                           IX1154.2
      *END-OF,IX115A                                                                  
