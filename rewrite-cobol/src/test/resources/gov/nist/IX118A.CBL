      *HEADER,COBOL,IX113A,SUBPRG,IX118A                                              
000100 IDENTIFICATION DIVISION.                                         IX1184.2
000200 PROGRAM-ID.                                                      IX1184.2
000300     IX118A.                                                      IX1184.2
000400****************************************************************  IX1184.2
000500*                                                              *  IX1184.2
000600*    VALIDATION FOR:-                                          *  IX1184.2
000700*                                                              *  IX1184.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1184.2
000900*                                                              *  IX1184.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1184.2
001100*                                                              *  IX1184.2
001200****************************************************************  IX1184.2
001300*                                                                 IX1184.2
001400*    THIS ROUTINE USES THE MASS STORAGE FILE IX-FS3 CREATED IN    IX1184.2
001500*    IX113A.                                                      IX1184.2
001600*    THE FILE IS OPENED I-O AND THE STATUS CHECKED (00 EXPECTED), IX1184.2
001700*    CLOSED AND THE STATUS CHECKED (00 EXPECTED) THEN THE FILE IS IX1184.2
001800*    OPENED TWICE, AT WHICH POINT THE DECLARATIVES                IX1184.2
001900*    SECTION SHOULD BE ACTIONED AND THE FILE STATUS SHOULD BE 41  IX1184.2
002000*    STANDARD REF. IX-5 1.3.4 (5) A                               IX1184.2
002100*                                                                 IX1184.2
002200*    X-CARDS USED IN THIS PROGRAM:                                IX1184.2
002300*                                                                 IX1184.2
002400*                 XXXXX024                                        IX1184.2
002500*                 XXXXX055.                                       IX1184.2
002600*         P       XXXXX062.                                       IX1184.2
002700*                 XXXXX082.                                       IX1184.2
002800*                 XXXXX083.                                       IX1184.2
002900*         C       XXXXX084                                        IX1184.2
003000*                                                                 IX1184.2
003100*                                                                 IX1184.2
003200 ENVIRONMENT DIVISION.                                            IX1184.2
003300 CONFIGURATION SECTION.                                           IX1184.2
003400 SOURCE-COMPUTER.                                                 IX1184.2
003500     XXXXX082.                                                    IX1184.2
003600 OBJECT-COMPUTER.                                                 IX1184.2
003700     XXXXX083.                                                    IX1184.2
003800 INPUT-OUTPUT SECTION.                                            IX1184.2
003900 FILE-CONTROL.                                                    IX1184.2
004000*    SELECT RAW-DATA   ASSIGN TO                                  IX1184.2
004100*    XXXXX062                                                     IX1184.2
004200*           ORGANIZATION IS INDEXED                               IX1184.2
004300*           ACCESS MODE IS RANDOM                                 IX1184.2
004400*           RECORD KEY IS RAW-DATA-KEY.                           IX1184.2
004500*                                                                 IX1184.2
004600     SELECT PRINT-FILE ASSIGN TO                                  IX1184.2
004700     XXXXX055.                                                    IX1184.2
004800*                                                                 IX1184.2
004900     SELECT IX-FS3 ASSIGN                                         IX1184.2
005000     XXXXX024                                                     IX1184.2
005100     ORGANIZATION IS INDEXED                                      IX1184.2
005200     ACCESS MODE IS SEQUENTIAL                                    IX1184.2
005300     RECORD KEY IS IX-FS3-KEY                                     IX1184.2
005400     FILE STATUS IS IX-FS3-STATUS.                                IX1184.2
005500                                                                  IX1184.2
005600 DATA DIVISION.                                                   IX1184.2
005700                                                                  IX1184.2
005800 FILE SECTION.                                                    IX1184.2
005900*                                                                 IX1184.2
006000*FD  RAW-DATA.                                                    IX1184.2
006100*                                                                 IX1184.2
006200*01  RAW-DATA-SATZ.                                               IX1184.2
006300*    05  RAW-DATA-KEY        PIC X(6).                            IX1184.2
006400*    05  C-DATE              PIC 9(6).                            IX1184.2
006500*    05  C-TIME              PIC 9(8).                            IX1184.2
006600*    05  C-NO-OF-TESTS       PIC 99.                              IX1184.2
006700*    05  C-OK                PIC 999.                             IX1184.2
006800*    05  C-ALL               PIC 999.                             IX1184.2
006900*    05  C-FAIL              PIC 999.                             IX1184.2
007000*    05  C-DELETED           PIC 999.                             IX1184.2
007100*    05  C-INSPECT           PIC 999.                             IX1184.2
007200*    05  C-NOTE              PIC X(13).                           IX1184.2
007300*    05  C-INDENT            PIC X.                               IX1184.2
007400*    05  C-ABORT             PIC X(8).                            IX1184.2
007500                                                                  IX1184.2
007600 FD  PRINT-FILE.                                                  IX1184.2
007700                                                                  IX1184.2
007800 01  PRINT-REC               PIC X(120).                          IX1184.2
007900                                                                  IX1184.2
008000 01  DUMMY-RECORD            PIC X(120).                          IX1184.2
008100                                                                  IX1184.2
008200 FD  IX-FS3                                                       IX1184.2
008300C       DATA RECORDS IX-FS3R1-F-G-240                             IX1184.2
008400C       LABEL RECORD STANDARD                                     IX1184.2
008500        RECORD 240                                                IX1184.2
008600        BLOCK CONTAINS 2 RECORDS.                                 IX1184.2
008700                                                                  IX1184.2
008800 01  IX-FS3R1-F-G-240.                                            IX1184.2
008900     05  IX-FS3-REC-120      PIC X(120).                          IX1184.2
009000     05  IX-FS3-REC-120-240.                                      IX1184.2
009100         10  FILLER          PIC X(8).                            IX1184.2
009200         10  IX-FS3-KEY      PIC X(29).                           IX1184.2
009300         10  FILLER          PIC X(9).                            IX1184.2
009400         10  IX-FS3-ALTER-KEY  PIC X(29).                         IX1184.2
009500         10  FILLER            PIC X(45).                         IX1184.2
009600                                                                  IX1184.2
009700                                                                  IX1184.2
009800 WORKING-STORAGE SECTION.                                         IX1184.2
009900                                                                  IX1184.2
010000 01  GRP-0101.                                                    IX1184.2
010100     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1184.2
010200     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1184.2
010300     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1184.2
010400                                                                  IX1184.2
010500 01  GRP-0102.                                                    IX1184.2
010600     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1184.2
010700     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1184.2
010800     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1184.2
010900                                                                  IX1184.2
011000 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1184.2
011100                                                                  IX1184.2
011200 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1184.2
011300                                                                  IX1184.2
011400 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1184.2
011500                                                                  IX1184.2
011600 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1184.2
011700                                                                  IX1184.2
011800 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1184.2
011900                                                                  IX1184.2
012000 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1184.2
012100                                                                  IX1184.2
012200 01  STATUS-TEST-10          PIC 9 VALUE ZERO.                    IX1184.2
012300 01  STATUS-TEST-READ        PIC 9 VALUE ZERO.                    IX1184.2
012400                                                                  IX1184.2
012500 01  IX-FS3-STATUS.                                               IX1184.2
012600     05  IX-FS3-STAT1        PIC X.                               IX1184.2
012700     05  IX-FS3-STAT2        PIC X.                               IX1184.2
012800                                                                  IX1184.2
012900 01  COUNT-OF-RECS           PIC 9(5).                            IX1184.2
013000                                                                  IX1184.2
013100 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1184.2
013200                                                                  IX1184.2
013300 01  FILE-RECORD-INFORMATION-REC.                                 IX1184.2
013400     05  FILE-RECORD-INFO-SKELETON.                               IX1184.2
013500         10  FILLER          PIC X(48) VALUE                      IX1184.2
013600              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1184.2
013700         10  FILLER          PIC X(46) VALUE                      IX1184.2
013800                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1184.2
013900         10  FILLER          PIC X(26) VALUE                      IX1184.2
014000                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1184.2
014100         10  FILLER          PIC X(37) VALUE                      IX1184.2
014200                         ",RECKEY=                             ". IX1184.2
014300         10  FILLER          PIC X(38) VALUE                      IX1184.2
014400                        ",ALTKEY1=                             ". IX1184.2
014500         10  FILLER          PIC X(38) VALUE                      IX1184.2
014600                        ",ALTKEY2=                             ". IX1184.2
014700         10  FILLER          PIC X(7) VALUE SPACE.                IX1184.2
014800     05  FILE-RECORD-INFO             OCCURS 10.                  IX1184.2
014900         10  FILE-RECORD-INFO-P1-120.                             IX1184.2
015000             15  FILLER      PIC X(5).                            IX1184.2
015100             15  XFILE-NAME  PIC X(6).                            IX1184.2
015200             15  FILLER      PIC X(8).                            IX1184.2
015300             15  XRECORD-NAME  PIC X(6).                          IX1184.2
015400             15  FILLER        PIC X(1).                          IX1184.2
015500             15  REELUNIT-NUMBER  PIC 9(1).                       IX1184.2
015600             15  FILLER           PIC X(7).                       IX1184.2
015700             15  XRECORD-NUMBER   PIC 9(6).                       IX1184.2
015800             15  FILLER           PIC X(6).                       IX1184.2
015900             15  UPDATE-NUMBER    PIC 9(2).                       IX1184.2
016000             15  FILLER           PIC X(5).                       IX1184.2
016100             15  ODO-NUMBER       PIC 9(4).                       IX1184.2
016200             15  FILLER           PIC X(5).                       IX1184.2
016300             15  XPROGRAM-NAME    PIC X(5).                       IX1184.2
016400             15  FILLER           PIC X(7).                       IX1184.2
016500             15  XRECORD-LENGTH   PIC 9(6).                       IX1184.2
016600             15  FILLER           PIC X(7).                       IX1184.2
016700             15  CHARS-OR-RECORDS  PIC X(2).                      IX1184.2
016800             15  FILLER            PIC X(1).                      IX1184.2
016900             15  XBLOCK-SIZE       PIC 9(4).                      IX1184.2
017000             15  FILLER            PIC X(6).                      IX1184.2
017100             15  RECORDS-IN-FILE   PIC 9(6).                      IX1184.2
017200             15  FILLER            PIC X(5).                      IX1184.2
017300             15  XFILE-ORGANIZATION  PIC X(2).                    IX1184.2
017400             15  FILLER              PIC X(6).                    IX1184.2
017500             15  XLABEL-TYPE         PIC X(1).                    IX1184.2
017600         10  FILE-RECORD-INFO-P121-240.                           IX1184.2
017700             15  FILLER              PIC X(8).                    IX1184.2
017800             15  XRECORD-KEY         PIC X(29).                   IX1184.2
017900             15  FILLER              PIC X(9).                    IX1184.2
018000             15  ALTERNATE-KEY1      PIC X(29).                   IX1184.2
018100             15  FILLER              PIC X(9).                    IX1184.2
018200             15  ALTERNATE-KEY2      PIC X(29).                   IX1184.2
018300             15  FILLER              PIC X(7).                    IX1184.2
018400                                                                  IX1184.2
018500 01  TEST-RESULTS.                                                IX1184.2
018600     02 FILLER                   PIC X      VALUE SPACE.          IX1184.2
018700     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1184.2
018800     02 FILLER                   PIC X      VALUE SPACE.          IX1184.2
018900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1184.2
019000     02 FILLER                   PIC X      VALUE SPACE.          IX1184.2
019100     02  PAR-NAME.                                                IX1184.2
019200       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1184.2
019300       03  PARDOT-X              PIC X      VALUE SPACE.          IX1184.2
019400       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1184.2
019500     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1184.2
019600     02 RE-MARK                  PIC X(61).                       IX1184.2
019700 01  TEST-COMPUTED.                                               IX1184.2
019800     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1184.2
019900     02 FILLER                   PIC X(17)  VALUE                 IX1184.2
020000            "       COMPUTED=".                                   IX1184.2
020100     02 COMPUTED-X.                                               IX1184.2
020200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1184.2
020300     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1184.2
020400                                 PIC -9(9).9(9).                  IX1184.2
020500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1184.2
020600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1184.2
020700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1184.2
020800     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1184.2
020900         04 COMPUTED-18V0                    PIC -9(18).          IX1184.2
021000         04 FILLER                           PIC X.               IX1184.2
021100     03 FILLER PIC X(50) VALUE SPACE.                             IX1184.2
021200 01  TEST-CORRECT.                                                IX1184.2
021300     02 FILLER PIC X(30) VALUE SPACE.                             IX1184.2
021400     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1184.2
021500     02 CORRECT-X.                                                IX1184.2
021600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1184.2
021700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1184.2
021800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1184.2
021900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1184.2
022000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1184.2
022100     03      CR-18V0 REDEFINES CORRECT-A.                         IX1184.2
022200         04 CORRECT-18V0                     PIC -9(18).          IX1184.2
022300         04 FILLER                           PIC X.               IX1184.2
022400     03 FILLER PIC X(2) VALUE SPACE.                              IX1184.2
022500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1184.2
022600 01  CCVS-C-1.                                                    IX1184.2
022700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1184.2
022800-    "SS  PARAGRAPH-NAME                                          IX1184.2
022900-    "       REMARKS".                                            IX1184.2
023000     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1184.2
023100 01  CCVS-C-2.                                                    IX1184.2
023200     02 FILLER                     PIC X        VALUE SPACE.      IX1184.2
023300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1184.2
023400     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1184.2
023500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1184.2
023600     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1184.2
023700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1184.2
023800 01  REC-CT                        PIC 99       VALUE ZERO.       IX1184.2
023900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1184.2
024000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1184.2
024100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1184.2
024200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1184.2
024300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1184.2
024400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1184.2
024500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1184.2
024600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1184.2
024700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1184.2
024800 01  CCVS-H-1.                                                    IX1184.2
024900     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1184.2
025000     02  FILLER                    PIC X(42)    VALUE             IX1184.2
025100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1184.2
025200     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1184.2
025300 01  CCVS-H-2A.                                                   IX1184.2
025400   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1184.2
025500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1184.2
025600   02  FILLER                        PIC XXXX   VALUE             IX1184.2
025700     "4.2 ".                                                      IX1184.2
025800   02  FILLER                        PIC X(28)  VALUE             IX1184.2
025900            " COPY - NOT FOR DISTRIBUTION".                       IX1184.2
026000   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1184.2
026100                                                                  IX1184.2
026200 01  CCVS-H-2B.                                                   IX1184.2
026300   02  FILLER                        PIC X(15)  VALUE             IX1184.2
026400            "TEST RESULT OF ".                                    IX1184.2
026500   02  TEST-ID                       PIC X(9).                    IX1184.2
026600   02  FILLER                        PIC X(4)   VALUE             IX1184.2
026700            " IN ".                                               IX1184.2
026800   02  FILLER                        PIC X(12)  VALUE             IX1184.2
026900     " HIGH       ".                                              IX1184.2
027000   02  FILLER                        PIC X(22)  VALUE             IX1184.2
027100            " LEVEL VALIDATION FOR ".                             IX1184.2
027200   02  FILLER                        PIC X(58)  VALUE             IX1184.2
027300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1184.2
027400 01  CCVS-H-3.                                                    IX1184.2
027500     02  FILLER                      PIC X(34)  VALUE             IX1184.2
027600            " FOR OFFICIAL USE ONLY    ".                         IX1184.2
027700     02  FILLER                      PIC X(58)  VALUE             IX1184.2
027800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1184.2
027900     02  FILLER                      PIC X(28)  VALUE             IX1184.2
028000            "  COPYRIGHT   1985 ".                                IX1184.2
028100 01  CCVS-E-1.                                                    IX1184.2
028200     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1184.2
028300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1184.2
028400     02 ID-AGAIN                     PIC X(9).                    IX1184.2
028500     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1184.2
028600 01  CCVS-E-2.                                                    IX1184.2
028700     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1184.2
028800     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1184.2
028900     02 CCVS-E-2-2.                                               IX1184.2
029000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1184.2
029100         03 FILLER                   PIC X      VALUE SPACE.      IX1184.2
029200         03 ENDER-DESC               PIC X(44)  VALUE             IX1184.2
029300            "ERRORS ENCOUNTERED".                                 IX1184.2
029400 01  CCVS-E-3.                                                    IX1184.2
029500     02  FILLER                      PIC X(22)  VALUE             IX1184.2
029600            " FOR OFFICIAL USE ONLY".                             IX1184.2
029700     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1184.2
029800     02  FILLER                      PIC X(58)  VALUE             IX1184.2
029900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1184.2
030000     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1184.2
030100     02 FILLER                       PIC X(15)  VALUE             IX1184.2
030200             " COPYRIGHT 1985".                                   IX1184.2
030300 01  CCVS-E-4.                                                    IX1184.2
030400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1184.2
030500     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1184.2
030600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1184.2
030700     02 FILLER                       PIC X(40)  VALUE             IX1184.2
030800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1184.2
030900 01  XXINFO.                                                      IX1184.2
031000     02 FILLER                       PIC X(19)  VALUE             IX1184.2
031100            "*** INFORMATION ***".                                IX1184.2
031200     02 INFO-TEXT.                                                IX1184.2
031300       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1184.2
031400       04 XXCOMPUTED                 PIC X(20).                   IX1184.2
031500       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1184.2
031600       04 XXCORRECT                  PIC X(20).                   IX1184.2
031700     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1184.2
031800 01  HYPHEN-LINE.                                                 IX1184.2
031900     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1184.2
032000     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1184.2
032100-    "*****************************************".                 IX1184.2
032200     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1184.2
032300-    "******************************".                            IX1184.2
032400 01  TEST-NO                         PIC 99.                      IX1184.2
032500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1184.2
032600     "IX118A".                                                    IX1184.2
032700 PROCEDURE DIVISION.                                              IX1184.2
032800 DECLARATIVES.                                                    IX1184.2
032900                                                                  IX1184.2
033000 SECT-IX105-0002 SECTION.                                         IX1184.2
033100     USE AFTER EXCEPTION PROCEDURE ON IX-FS3.                     IX1184.2
033200 INPUT-PROCESS.                                                   IX1184.2
033300     IF TEST-NO = 5                                               IX1184.2
033400        GO TO D-C-TEST-GF-01-1.                                   IX1184.2
033500     IF STATUS-TEST-10 EQUAL TO 1                                 IX1184.2
033600        IF  IX-FS3-STAT1 EQUAL TO "1"                             IX1184.2
033700            MOVE 1 TO EOF-FLAG                                    IX1184.2
033800        ELSE                                                      IX1184.2
033900           IF  IX-FS3-STAT1 GREATER THAN "1"                      IX1184.2
034000           MOVE 1 TO PERM-ERRORS.                                 IX1184.2
034100     GO TO DECL-EXIT.                                             IX1184.2
034200 D-C-TEST-GF-01-1.                                                IX1184.2
034300     IF IX-FS3-STATUS EQUAL TO "41"                               IX1184.2
034400         GO TO D-C-PASS-GF-01-0.                                  IX1184.2
034500 D-C-FAIL-GF-01-0.                                                IX1184.2
034600     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1184.2
034700     MOVE "41" TO CORRECT-X.                                      IX1184.2
034800     MOVE "IX-5, 1.3.4, (5) A" TO RE-MARK.                        IX1184.2
034900     PERFORM D-FAIL.                                              IX1184.2
035000     GO TO D-C-WRITE-GF-01-0.                                     IX1184.2
035100 D-C-PASS-GF-01-0.                                                IX1184.2
035200     PERFORM D-PASS.                                              IX1184.2
035300 D-C-WRITE-GF-01-0.                                               IX1184.2
035400     PERFORM D-PRINT-DETAIL.                                      IX1184.2
035500 D-CLOSE-FILES.                                                   IX1184.2
035600*    OPEN I-O RAW-DATA.                                           IX1184.2
035700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1184.2
035800*    READ RAW-DATA INVALID KEY GO TO D-END-E-2.                   IX1184.2
035900*    MOVE "OK.     " TO C-ABORT.                                  IX1184.2
036000*    MOVE PASS-COUNTER TO C-OK.                                   IX1184.2
036100*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1184.2
036200*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1184.2
036300*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1184.2
036400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1184.2
036500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO D-END-E-2.           IX1184.2
036600*D-END-E-2.                                                       IX1184.2
036700*    CLOSE RAW-DATA.                                              IX1184.2
036800     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 IX1184.2
036900     CLOSE PRINT-FILE.                                            IX1184.2
037000 D-TERMINATE-CCVS.                                                IX1184.2
037100*    EXIT PROGRAM.                                                IX1184.2
037200*D-TERMINATE-CALL.                                                IX1184.2
037300     STOP     RUN.                                                IX1184.2
037400 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         IX1184.2
037500 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        IX1184.2
037600 D-PRINT-DETAIL.                                                  IX1184.2
037700     IF   REC-CT NOT EQUAL TO ZERO                                IX1184.2
037800          MOVE "." TO PARDOT-X                                    IX1184.2
037900          MOVE REC-CT TO DOTVALUE.                                IX1184.2
038000     MOVE TEST-RESULTS TO PRINT-REC.                              IX1184.2
038100     PERFORM D-WRITE-LINE.                                        IX1184.2
038200     IF   P-OR-F EQUAL TO "FAIL*"                                 IX1184.2
038300          PERFORM D-WRITE-LINE                                    IX1184.2
038400          PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX           IX1184.2
038500     ELSE                                                         IX1184.2
038600          PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.                  IX1184.2
038700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1184.2
038800     MOVE SPACE TO CORRECT-X.                                     IX1184.2
038900     IF   REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.           IX1184.2
039000     MOVE SPACE TO RE-MARK.                                       IX1184.2
039100 D-END-ROUTINE.                                                   IX1184.2
039200     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            IX1184.2
039300     PERFORM D-WRITE-LINE 5 TIMES.                                IX1184.2
039400 D-END-RTN-EXIT.                                                  IX1184.2
039500     MOVE CCVS-E-1 TO DUMMY-RECORD.                               IX1184.2
039600     PERFORM D-WRITE-LINE 2 TIMES.                                IX1184.2
039700 D-END-ROUTINE-1.                                                 IX1184.2
039800     ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO       IX1184.2
039900     ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.                IX1184.2
040000     ADD PASS-COUNTER TO ERROR-HOLD.                              IX1184.2
040100     MOVE PASS-COUNTER TO CCVS-E-4-1.                             IX1184.2
040200     MOVE ERROR-HOLD TO CCVS-E-4-2.                               IX1184.2
040300     MOVE CCVS-E-4 TO CCVS-E-2-2.                                 IX1184.2
040400     MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.          IX1184.2
040500  D-END-ROUTINE-12.                                               IX1184.2
040600     MOVE "TEST(S) FAILED" TO ENDER-DESC.                         IX1184.2
040700     IF  ERROR-COUNTER IS EQUAL TO ZERO                           IX1184.2
040800         MOVE "NO " TO ERROR-TOTAL                                IX1184.2
040900     ELSE                                                         IX1184.2
041000         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1184.2
041100     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            IX1184.2
041200     PERFORM D-WRITE-LINE.                                        IX1184.2
041300 D-END-ROUTINE-13.                                                IX1184.2
041400     IF  DELETE-COUNTER IS EQUAL TO ZERO                          IX1184.2
041500         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1184.2
041600         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1184.2
041700     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1184.2
041800     MOVE CCVS-E-2 TO DUMMY-RECORD.                               IX1184.2
041900     PERFORM D-WRITE-LINE.                                        IX1184.2
042000     IF   INSPECT-COUNTER EQUAL TO ZERO                           IX1184.2
042100          MOVE "NO " TO ERROR-TOTAL                               IX1184.2
042200     ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                    IX1184.2
042300     MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.             IX1184.2
042400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1184.2
042500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1184.2
042600 D-WRITE-LINE.                                                    IX1184.2
042700     ADD 1 TO RECORD-COUNT.                                       IX1184.2
042800*    IF RECORD-COUNT GREATER 42                                   IX1184.2
042900*       MOVE DUMMY-RECORD TO DUMMY-HOLD                           IX1184.2
043000*       MOVE SPACE TO DUMMY-RECORD                                IX1184.2
043100*       WRITE DUMMY-RECORD AFTER ADVANCING PAGE                   IX1184.2
043200*       MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1184.2
043300*       MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1184.2
043400*       MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1184.2
043500*       MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1184.2
043600*       MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1184.2
043700*       MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1184.2
043800*       MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN         IX1184.2
043900*       MOVE DUMMY-HOLD TO DUMMY-RECORD                           IX1184.2
044000*       MOVE ZERO TO RECORD-COUNT.                                IX1184.2
044100     PERFORM D-WRT-LN.                                            IX1184.2
044200 D-WRT-LN.                                                        IX1184.2
044300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1184.2
044400     MOVE SPACE TO DUMMY-RECORD.                                  IX1184.2
044500 D-FAIL-ROUTINE.                                                  IX1184.2
044600     IF   COMPUTED-X NOT EQUAL TO SPACE                           IX1184.2
044700          GO TO D-FAIL-ROUTINE-WRITE.                             IX1184.2
044800     IF   CORRECT-X NOT EQUAL TO SPACE GO TO D-FAIL-ROUTINE-WRITE.IX1184.2
044900     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1184.2
045000     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    IX1184.2
045100     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1184.2
045200     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1184.2
045300     GO TO D-FAIL-ROUTINE-EX.                                     IX1184.2
045400 D-FAIL-ROUTINE-WRITE.                                            IX1184.2
045500     MOVE TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE         IX1184.2
045600     MOVE ANSI-REFERENCE TO COR-ANSI-REFERENCE.                   IX1184.2
045700     MOVE TEST-CORRECT TO PRINT-REC PERFORM D-WRITE-LINE 2 TIMES. IX1184.2
045800     MOVE SPACES TO COR-ANSI-REFERENCE.                           IX1184.2
045900 D-FAIL-ROUTINE-EX. EXIT.                                         IX1184.2
046000 D-BAIL-OUT.                                                      IX1184.2
046100     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    IX1184.2
046200     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            IX1184.2
046300 D-BAIL-OUT-WRITE.                                                IX1184.2
046400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1184.2
046500     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1184.2
046600     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1184.2
046700     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1184.2
046800 D-BAIL-OUT-EX. EXIT.                                             IX1184.2
046900 DECL-EXIT.  EXIT.                                                IX1184.2
047000 END DECLARATIVES.                                                IX1184.2
047100                                                                  IX1184.2
047200                                                                  IX1184.2
047300 CCVS1 SECTION.                                                   IX1184.2
047400 OPEN-FILES.                                                      IX1184.2
047500*    OPEN I-O RAW-DATA.                                           IX1184.2
047600*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1184.2
047700*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1184.2
047800*    MOVE "ABORTED " TO C-ABORT.                                  IX1184.2
047900*    ADD 1 TO C-NO-OF-TESTS.                                      IX1184.2
048000*    ACCEPT C-DATE  FROM DATE.                                    IX1184.2
048100*    ACCEPT C-TIME  FROM TIME.                                    IX1184.2
048200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1184.2
048300*END-E-1.                                                         IX1184.2
048400*    CLOSE RAW-DATA.                                              IX1184.2
048500     OPEN    OUTPUT PRINT-FILE.                                   IX1184.2
048600     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1184.2
048700     MOVE    SPACE TO TEST-RESULTS.                               IX1184.2
048800     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1184.2
048900     MOVE    ZERO TO REC-SKL-SUB.                                 IX1184.2
049000     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1184.2
049100 CCVS-INIT-FILE.                                                  IX1184.2
049200     ADD     1 TO REC-SKL-SUB.                                    IX1184.2
049300     MOVE    FILE-RECORD-INFO-SKELETON                            IX1184.2
049400          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1184.2
049500 CCVS-INIT-EXIT.                                                  IX1184.2
049600     GO TO CCVS1-EXIT.                                            IX1184.2
049700 CLOSE-FILES.                                                     IX1184.2
049800*    OPEN I-O RAW-DATA.                                           IX1184.2
049900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1184.2
050000*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1184.2
050100*    MOVE "OK.     " TO C-ABORT.                                  IX1184.2
050200*    MOVE PASS-COUNTER TO C-OK.                                   IX1184.2
050300*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1184.2
050400*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1184.2
050500*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1184.2
050600*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1184.2
050700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1184.2
050800*END-E-2.                                                         IX1184.2
050900*    CLOSE RAW-DATA.                                              IX1184.2
051000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1184.2
051100 TERMINATE-CCVS.                                                  IX1184.2
051200*    EXIT PROGRAM.                                                IX1184.2
051300*TERMINATE-CALL.                                                  IX1184.2
051400     STOP     RUN.                                                IX1184.2
051500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1184.2
051600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1184.2
051700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1184.2
051800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1184.2
051900     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1184.2
052000 PRINT-DETAIL.                                                    IX1184.2
052100     IF REC-CT NOT EQUAL TO ZERO                                  IX1184.2
052200             MOVE "." TO PARDOT-X                                 IX1184.2
052300             MOVE REC-CT TO DOTVALUE.                             IX1184.2
052400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1184.2
052500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1184.2
052600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1184.2
052700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1184.2
052800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1184.2
052900     MOVE SPACE TO CORRECT-X.                                     IX1184.2
053000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1184.2
053100     MOVE     SPACE TO RE-MARK.                                   IX1184.2
053200 HEAD-ROUTINE.                                                    IX1184.2
053300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1184.2
053400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1184.2
053500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1184.2
053600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1184.2
053700 COLUMN-NAMES-ROUTINE.                                            IX1184.2
053800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1184.2
053900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1184.2
054000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1184.2
054100 END-ROUTINE.                                                     IX1184.2
054200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1184.2
054300 END-RTN-EXIT.                                                    IX1184.2
054400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1184.2
054500 END-ROUTINE-1.                                                   IX1184.2
054600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1184.2
054700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1184.2
054800      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1184.2
054900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1184.2
055000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1184.2
055100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1184.2
055200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1184.2
055300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1184.2
055400  END-ROUTINE-12.                                                 IX1184.2
055500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1184.2
055600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1184.2
055700         MOVE "NO " TO ERROR-TOTAL                                IX1184.2
055800         ELSE                                                     IX1184.2
055900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1184.2
056000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1184.2
056100     PERFORM WRITE-LINE.                                          IX1184.2
056200 END-ROUTINE-13.                                                  IX1184.2
056300     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1184.2
056400         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1184.2
056500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1184.2
056600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1184.2
056700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1184.2
056800      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1184.2
056900          MOVE "NO " TO ERROR-TOTAL                               IX1184.2
057000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1184.2
057100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1184.2
057200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1184.2
057300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1184.2
057400 WRITE-LINE.                                                      IX1184.2
057500     ADD 1 TO RECORD-COUNT.                                       IX1184.2
057600*    IF RECORD-COUNT GREATER 42                                   IX1184.2
057700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1184.2
057800*        MOVE SPACE TO DUMMY-RECORD                               IX1184.2
057900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1184.2
058000*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1184.2
058100*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1184.2
058200*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1184.2
058300*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1184.2
058400*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1184.2
058500*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1184.2
058600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1184.2
058700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1184.2
058800*        MOVE ZERO TO RECORD-COUNT.                               IX1184.2
058900     PERFORM WRT-LN.                                              IX1184.2
059000 WRT-LN.                                                          IX1184.2
059100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1184.2
059200     MOVE SPACE TO DUMMY-RECORD.                                  IX1184.2
059300 BLANK-LINE-PRINT.                                                IX1184.2
059400     PERFORM WRT-LN.                                              IX1184.2
059500 FAIL-ROUTINE.                                                    IX1184.2
059600     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1184.2
059700            GO TO   FAIL-ROUTINE-WRITE.                           IX1184.2
059800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1184.2
059900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1184.2
060000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1184.2
060100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1184.2
060200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1184.2
060300     GO TO  FAIL-ROUTINE-EX.                                      IX1184.2
060400 FAIL-ROUTINE-WRITE.                                              IX1184.2
060500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1184.2
060600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1184.2
060700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1184.2
060800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1184.2
060900 FAIL-ROUTINE-EX. EXIT.                                           IX1184.2
061000 BAIL-OUT.                                                        IX1184.2
061100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1184.2
061200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1184.2
061300 BAIL-OUT-WRITE.                                                  IX1184.2
061400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1184.2
061500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1184.2
061600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1184.2
061700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1184.2
061800 BAIL-OUT-EX. EXIT.                                               IX1184.2
061900 CCVS1-EXIT.                                                      IX1184.2
062000     EXIT.                                                        IX1184.2
062100                                                                  IX1184.2
062200 SECT-IX118A-0003 SECTION.                                        IX1184.2
062300 SEQ-INIT-010.                                                    IX1184.2
062400     MOVE ZERO TO TEST-NO.                                        IX1184.2
062500     MOVE "IX-FS3" TO XFILE-NAME (1).                             IX1184.2
062600     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IX1184.2
062700     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IX1184.2
062800     MOVE 000240 TO XRECORD-LENGTH (1).                           IX1184.2
062900     MOVE "RC" TO CHARS-OR-RECORDS (1).                           IX1184.2
063000     MOVE 0002 TO XBLOCK-SIZE (1).                                IX1184.2
063100     MOVE 000050 TO RECORDS-IN-FILE (1).                          IX1184.2
063200     MOVE "IX" TO XFILE-ORGANIZATION (1).                         IX1184.2
063300     MOVE "S" TO XLABEL-TYPE (1).                                 IX1184.2
063400     MOVE 000001 TO XRECORD-NUMBER (1).                           IX1184.2
063500     MOVE 0 TO COUNT-OF-RECS.                                     IX1184.2
063600                                                                  IX1184.2
063700******************************************************************IX1184.2
063800*   TEST  1                                                      *IX1184.2
063900*         OPEN OUTPUT ...                 00 EXPECTED            *IX1184.2
064000*         IX-3, 1.3.4 (1) A                                      *IX1184.2
064100*    STATUS 00 CHECK ON OUTPUT FILE IX-FS3                       *IX1184.2
064200*    THE OUTPUT STATEMENT IS SUCCESSFULLY EXECUTED               *IX1184.2
064300******************************************************************IX1184.2
064400 OPN-INIT-GF-01-0.                                                IX1184.2
064500     MOVE 1 TO STATUS-TEST-00.                                    IX1184.2
064600     MOVE SPACES TO IX-FS3-STATUS.                                IX1184.2
064700     MOVE "OPEN I-O   : 00 EXP." TO FEATURE.                      IX1184.2
064800     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1184.2
064900     OPEN                                                         IX1184.2
065000        I-O    IX-FS3.                                            IX1184.2
065100     IF IX-FS3-STATUS EQUAL TO "00"                               IX1184.2
065200         GO TO OPN-PASS-GF-01-0.                                  IX1184.2
065300 OPN-FAIL-GF-01-0.                                                IX1184.2
065400     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1184.2
065500     PERFORM FAIL.                                                IX1184.2
065600     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1184.2
065700     MOVE "00" TO CORRECT-X.                                      IX1184.2
065800     GO TO OPN-WRITE-GF-01-0.                                     IX1184.2
065900 OPN-PASS-GF-01-0.                                                IX1184.2
066000     PERFORM PASS.                                                IX1184.2
066100 OPN-WRITE-GF-01-0.                                               IX1184.2
066200     PERFORM PRINT-DETAIL.                                        IX1184.2
066300******************************************************************IX1184.2
066400*   TEST  4                                                      *IX1184.2
066500*         CLOSE I-O                       00 EXPECTED            *IX1184.2
066600*         IX-3, 1.3.4 (1) A                                      *IX1184.2
066700******************************************************************IX1184.2
066800 CLO-INIT-GF-01-0.                                                IX1184.2
066900     MOVE SPACES TO IX-FS3-STATUS.                                IX1184.2
067000     MOVE "CLOSE I-O   :00 EXP." TO FEATURE.                      IX1184.2
067100     MOVE "CLO-TEST-GF-01-0" TO PAR-NAME.                         IX1184.2
067200 CLO-TEST-GF-01-0.                                                IX1184.2
067300     CLOSE IX-FS3.                                                IX1184.2
067400     IF IX-FS3-STATUS = "00"                                      IX1184.2
067500         GO TO CLO-PASS-GF-01-0.                                  IX1184.2
067600 CLO-FAIL-GF-01-0.                                                IX1184.2
067700     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1184.2
067800     PERFORM FAIL.                                                IX1184.2
067900     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1184.2
068000     MOVE "00" TO CORRECT-X.                                      IX1184.2
068100     GO TO CLO-WRITE-GF-01-0.                                     IX1184.2
068200 CLO-PASS-GF-01-0.                                                IX1184.2
068300     PERFORM PASS.                                                IX1184.2
068400 CLO-WRITE-GF-01-0.                                               IX1184.2
068500     PERFORM PRINT-DETAIL.                                        IX1184.2
068600                                                                  IX1184.2
068700******************************************************************IX1184.2
068800*    A INDEXED FILE WITH 50 RECORDS HAS BEEN CREATED.            *IX1184.2
068900******************************************************************IX1184.2
069000                                                                  IX1184.2
069100******************************************************************IX1184.2
069200*   TEST  5                                                      *IX1184.2
069300*         OPEN FOR A FILE ALREADY IN  OPEN MODE                  *IX1184.2
069400*         FILE STATUS 41 EXPECTED IX-5, 1.3.4 (5) A              *IX1184.2
069500******************************************************************IX1184.2
069600 OPN-TEST-GF-02-0.                                                IX1184.2
069700     MOVE  5 TO TEST-NO.                                          IX1184.2
069800     MOVE SPACES TO IX-FS3-STATUS.                                IX1184.2
069900     MOVE "OPEN         41 EXP." TO FEATURE                       IX1184.2
070000     MOVE "OPN-TEST-GF-02-0" TO PAR-NAME.                         IX1184.2
070100     OPEN INPUT IX-FS3.                                           IX1184.2
070200     OPEN INPUT IX-FS3.                                           IX1184.2
070300 OPN-TEST-GF-02-1.                                                IX1184.2
070400     IF IX-FS3-STATUS EQUAL TO "41"                               IX1184.2
070500        MOVE "SHOULD HAVE EXECUTED DECLARATIVES IX-3,1.3.4(4)"    IX1184.2
070600          TO RE-MARK                                              IX1184.2
070700        GO TO OPN-WRITE-GF-02-0.                                  IX1184.2
070800 OPN-FAIL-GF-02-0.                                                IX1184.2
070900     MOVE "IX-5, 1.3.4, (5) A" TO RE-MARK.                        IX1184.2
071000 OPN-WRITE-GF-02-0.                                               IX1184.2
071100     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1184.2
071200     MOVE "41" TO CORRECT-X.                                      IX1184.2
071300     PERFORM FAIL.                                                IX1184.2
071400     PERFORM PRINT-DETAIL.                                        IX1184.2
071500     CLOSE IX-FS3.                                                IX1184.2
071600                                                                  IX1184.2
071700 TERMINATE-ROUTINE.                                               IX1184.2
071800     EXIT.                                                        IX1184.2
071900                                                                  IX1184.2
072000 CCVS-EXIT SECTION.                                               IX1184.2
072100 CCVS-999999.                                                     IX1184.2
072200     GO TO CLOSE-FILES.                                           IX1184.2
      *END-OF,IX118A                                                                  
