      *HEADER,COBOL,IX113A,SUBPRG,IX120A                                              
000100 IDENTIFICATION DIVISION.                                         IX1204.2
000200 PROGRAM-ID.                                                      IX1204.2
000300     IX120A.                                                      IX1204.2
000400****************************************************************  IX1204.2
000500*                                                              *  IX1204.2
000600*    VALIDATION FOR:-                                          *  IX1204.2
000700*                                                              *  IX1204.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1204.2
000900*                                                              *  IX1204.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1204.2
001100*                                                              *  IX1204.2
001200****************************************************************  IX1204.2
001300*                                                                 IX1204.2
001400*    THIS ROUTINE USES THE MASS STORAGE FILE IX-FS3 CREATED IN    IX1204.2
001500*    IX113A.                                                      IX1204.2
001600*    THE FILE IS OPENED I-O AND THE STATUS CHECKED (00 EXPECTED), IX1204.2
001700*    THE FILE IS THEN READ UNTIL THE AT END CONDITION IS REACHED  IX1204.2
001800*    AND THEN READ ONCE MORE.  AN ATTEMPT IS THEN MADE TO REWRITE IX1204.2
001900*    A RECORD, AT WHICH POINT THE DECLARATIVES                    IX1204.2
002000*    SHOULD BE ACTIONED AND THE FILE STATUS SHOULD BE 43 .        IX1204.2
002100*                                                                 IX1204.2
002200*    STANDARD REFERENCE IX-5, 1.3.4 (5) C                         IX1204.2
002300*                                                                 IX1204.2
002400*    X-CARDS USED IN THIS PROGRAM:                                IX1204.2
002500*                                                                 IX1204.2
002600*                 XXXXX024                                        IX1204.2
002700*                 XXXXX055.                                       IX1204.2
002800*         P       XXXXX062.                                       IX1204.2
002900*                 XXXXX082.                                       IX1204.2
003000*                 XXXXX083.                                       IX1204.2
003100*         C       XXXXX084                                        IX1204.2
003200*                                                                 IX1204.2
003300*                                                                 IX1204.2
003400 ENVIRONMENT DIVISION.                                            IX1204.2
003500 CONFIGURATION SECTION.                                           IX1204.2
003600 SOURCE-COMPUTER.                                                 IX1204.2
003700     XXXXX082.                                                    IX1204.2
003800 OBJECT-COMPUTER.                                                 IX1204.2
003900     XXXXX083.                                                    IX1204.2
004000 INPUT-OUTPUT SECTION.                                            IX1204.2
004100 FILE-CONTROL.                                                    IX1204.2
004200*    SELECT RAW-DATA   ASSIGN TO                                  IX1204.2
004300*    XXXXX062                                                     IX1204.2
004400*           ORGANIZATION IS INDEXED                               IX1204.2
004500*           ACCESS MODE IS RANDOM                                 IX1204.2
004600*           RECORD KEY IS RAW-DATA-KEY.                           IX1204.2
004700*                                                                 IX1204.2
004800     SELECT PRINT-FILE ASSIGN TO                                  IX1204.2
004900     XXXXX055.                                                    IX1204.2
005000*                                                                 IX1204.2
005100     SELECT IX-FS3 ASSIGN                                         IX1204.2
005200     XXXXX024                                                     IX1204.2
005300     ORGANIZATION IS INDEXED                                      IX1204.2
005400     ACCESS MODE IS SEQUENTIAL                                    IX1204.2
005500     RECORD KEY IS IX-FS3-KEY                                     IX1204.2
005600     FILE STATUS IS IX-FS3-STATUS.                                IX1204.2
005700                                                                  IX1204.2
005800 DATA DIVISION.                                                   IX1204.2
005900                                                                  IX1204.2
006000 FILE SECTION.                                                    IX1204.2
006100*                                                                 IX1204.2
006200*FD  RAW-DATA.                                                    IX1204.2
006300*                                                                 IX1204.2
006400*01  RAW-DATA-SATZ.                                               IX1204.2
006500*    05  RAW-DATA-KEY        PIC X(6).                            IX1204.2
006600*    05  C-DATE              PIC 9(6).                            IX1204.2
006700*    05  C-TIME              PIC 9(8).                            IX1204.2
006800*    05  C-NO-OF-TESTS       PIC 99.                              IX1204.2
006900*    05  C-OK                PIC 999.                             IX1204.2
007000*    05  C-ALL               PIC 999.                             IX1204.2
007100*    05  C-FAIL              PIC 999.                             IX1204.2
007200*    05  C-DELETED           PIC 999.                             IX1204.2
007300*    05  C-INSPECT           PIC 999.                             IX1204.2
007400*    05  C-NOTE              PIC X(13).                           IX1204.2
007500*    05  C-INDENT            PIC X.                               IX1204.2
007600*    05  C-ABORT             PIC X(8).                            IX1204.2
007700                                                                  IX1204.2
007800 FD  PRINT-FILE.                                                  IX1204.2
007900                                                                  IX1204.2
008000 01  PRINT-REC               PIC X(120).                          IX1204.2
008100                                                                  IX1204.2
008200 01  DUMMY-RECORD            PIC X(120).                          IX1204.2
008300                                                                  IX1204.2
008400 FD  IX-FS3                                                       IX1204.2
008500C       DATA RECORDS IX-FS3R1-F-G-240                             IX1204.2
008600C       LABEL RECORD STANDARD                                     IX1204.2
008700        RECORD 240                                                IX1204.2
008800        BLOCK CONTAINS 2 RECORDS.                                 IX1204.2
008900                                                                  IX1204.2
009000 01  IX-FS3R1-F-G-240.                                            IX1204.2
009100     05  IX-FS3-REC-120      PIC X(120).                          IX1204.2
009200     05  IX-FS3-REC-120-240.                                      IX1204.2
009300         10  FILLER          PIC X(8).                            IX1204.2
009400         10  IX-FS3-KEY      PIC X(29).                           IX1204.2
009500         10  FILLER          PIC X(9).                            IX1204.2
009600         10  IX-FS3-ALTER-KEY  PIC X(29).                         IX1204.2
009700         10  FILLER            PIC X(45).                         IX1204.2
009800                                                                  IX1204.2
009900                                                                  IX1204.2
010000 WORKING-STORAGE SECTION.                                         IX1204.2
010100                                                                  IX1204.2
010200 01  GRP-0101.                                                    IX1204.2
010300     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1204.2
010400     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1204.2
010500     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1204.2
010600                                                                  IX1204.2
010700 01  GRP-0102.                                                    IX1204.2
010800     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1204.2
010900     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1204.2
011000     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1204.2
011100                                                                  IX1204.2
011200 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1204.2
011300                                                                  IX1204.2
011400 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1204.2
011500                                                                  IX1204.2
011600 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1204.2
011700                                                                  IX1204.2
011800 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1204.2
011900                                                                  IX1204.2
012000 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1204.2
012100                                                                  IX1204.2
012200 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1204.2
012300                                                                  IX1204.2
012400 01  STATUS-TEST-10          PIC 9 VALUE ZERO.                    IX1204.2
012500 01  STATUS-TEST-READ        PIC 9 VALUE ZERO.                    IX1204.2
012600                                                                  IX1204.2
012700 01  IX-FS3-STATUS.                                               IX1204.2
012800     05  IX-FS3-STAT1        PIC X.                               IX1204.2
012900     05  IX-FS3-STAT2        PIC X.                               IX1204.2
013000                                                                  IX1204.2
013100 01  COUNT-OF-RECS           PIC 9(5).                            IX1204.2
013200                                                                  IX1204.2
013300 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1204.2
013400                                                                  IX1204.2
013500 01  FILE-RECORD-INFORMATION-REC.                                 IX1204.2
013600     05  FILE-RECORD-INFO-SKELETON.                               IX1204.2
013700         10  FILLER          PIC X(48) VALUE                      IX1204.2
013800              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1204.2
013900         10  FILLER          PIC X(46) VALUE                      IX1204.2
014000                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1204.2
014100         10  FILLER          PIC X(26) VALUE                      IX1204.2
014200                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1204.2
014300         10  FILLER          PIC X(37) VALUE                      IX1204.2
014400                         ",RECKEY=                             ". IX1204.2
014500         10  FILLER          PIC X(38) VALUE                      IX1204.2
014600                        ",ALTKEY1=                             ". IX1204.2
014700         10  FILLER          PIC X(38) VALUE                      IX1204.2
014800                        ",ALTKEY2=                             ". IX1204.2
014900         10  FILLER          PIC X(7) VALUE SPACE.                IX1204.2
015000     05  FILE-RECORD-INFO             OCCURS 10.                  IX1204.2
015100         10  FILE-RECORD-INFO-P1-120.                             IX1204.2
015200             15  FILLER      PIC X(5).                            IX1204.2
015300             15  XFILE-NAME  PIC X(6).                            IX1204.2
015400             15  FILLER      PIC X(8).                            IX1204.2
015500             15  XRECORD-NAME  PIC X(6).                          IX1204.2
015600             15  FILLER        PIC X(1).                          IX1204.2
015700             15  REELUNIT-NUMBER  PIC 9(1).                       IX1204.2
015800             15  FILLER           PIC X(7).                       IX1204.2
015900             15  XRECORD-NUMBER   PIC 9(6).                       IX1204.2
016000             15  FILLER           PIC X(6).                       IX1204.2
016100             15  UPDATE-NUMBER    PIC 9(2).                       IX1204.2
016200             15  FILLER           PIC X(5).                       IX1204.2
016300             15  ODO-NUMBER       PIC 9(4).                       IX1204.2
016400             15  FILLER           PIC X(5).                       IX1204.2
016500             15  XPROGRAM-NAME    PIC X(5).                       IX1204.2
016600             15  FILLER           PIC X(7).                       IX1204.2
016700             15  XRECORD-LENGTH   PIC 9(6).                       IX1204.2
016800             15  FILLER           PIC X(7).                       IX1204.2
016900             15  CHARS-OR-RECORDS  PIC X(2).                      IX1204.2
017000             15  FILLER            PIC X(1).                      IX1204.2
017100             15  XBLOCK-SIZE       PIC 9(4).                      IX1204.2
017200             15  FILLER            PIC X(6).                      IX1204.2
017300             15  RECORDS-IN-FILE   PIC 9(6).                      IX1204.2
017400             15  FILLER            PIC X(5).                      IX1204.2
017500             15  XFILE-ORGANIZATION  PIC X(2).                    IX1204.2
017600             15  FILLER              PIC X(6).                    IX1204.2
017700             15  XLABEL-TYPE         PIC X(1).                    IX1204.2
017800         10  FILE-RECORD-INFO-P121-240.                           IX1204.2
017900             15  FILLER              PIC X(8).                    IX1204.2
018000             15  XRECORD-KEY         PIC X(29).                   IX1204.2
018100             15  FILLER              PIC X(9).                    IX1204.2
018200             15  ALTERNATE-KEY1      PIC X(29).                   IX1204.2
018300             15  FILLER              PIC X(9).                    IX1204.2
018400             15  ALTERNATE-KEY2      PIC X(29).                   IX1204.2
018500             15  FILLER              PIC X(7).                    IX1204.2
018600                                                                  IX1204.2
018700 01  TEST-RESULTS.                                                IX1204.2
018800     02 FILLER                   PIC X      VALUE SPACE.          IX1204.2
018900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1204.2
019000     02 FILLER                   PIC X      VALUE SPACE.          IX1204.2
019100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1204.2
019200     02 FILLER                   PIC X      VALUE SPACE.          IX1204.2
019300     02  PAR-NAME.                                                IX1204.2
019400       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1204.2
019500       03  PARDOT-X              PIC X      VALUE SPACE.          IX1204.2
019600       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1204.2
019700     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1204.2
019800     02 RE-MARK                  PIC X(61).                       IX1204.2
019900 01  TEST-COMPUTED.                                               IX1204.2
020000     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1204.2
020100     02 FILLER                   PIC X(17)  VALUE                 IX1204.2
020200            "       COMPUTED=".                                   IX1204.2
020300     02 COMPUTED-X.                                               IX1204.2
020400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1204.2
020500     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1204.2
020600                                 PIC -9(9).9(9).                  IX1204.2
020700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1204.2
020800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1204.2
020900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1204.2
021000     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1204.2
021100         04 COMPUTED-18V0                    PIC -9(18).          IX1204.2
021200         04 FILLER                           PIC X.               IX1204.2
021300     03 FILLER PIC X(50) VALUE SPACE.                             IX1204.2
021400 01  TEST-CORRECT.                                                IX1204.2
021500     02 FILLER PIC X(30) VALUE SPACE.                             IX1204.2
021600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1204.2
021700     02 CORRECT-X.                                                IX1204.2
021800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1204.2
021900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1204.2
022000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1204.2
022100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1204.2
022200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1204.2
022300     03      CR-18V0 REDEFINES CORRECT-A.                         IX1204.2
022400         04 CORRECT-18V0                     PIC -9(18).          IX1204.2
022500         04 FILLER                           PIC X.               IX1204.2
022600     03 FILLER PIC X(2) VALUE SPACE.                              IX1204.2
022700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1204.2
022800 01  CCVS-C-1.                                                    IX1204.2
022900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1204.2
023000-    "SS  PARAGRAPH-NAME                                          IX1204.2
023100-    "       REMARKS".                                            IX1204.2
023200     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1204.2
023300 01  CCVS-C-2.                                                    IX1204.2
023400     02 FILLER                     PIC X        VALUE SPACE.      IX1204.2
023500     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1204.2
023600     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1204.2
023700     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1204.2
023800     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1204.2
023900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1204.2
024000 01  REC-CT                        PIC 99       VALUE ZERO.       IX1204.2
024100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1204.2
024200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1204.2
024300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1204.2
024400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1204.2
024500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1204.2
024600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1204.2
024700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1204.2
024800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1204.2
024900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1204.2
025000 01  CCVS-H-1.                                                    IX1204.2
025100     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1204.2
025200     02  FILLER                    PIC X(42)    VALUE             IX1204.2
025300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1204.2
025400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1204.2
025500 01  CCVS-H-2A.                                                   IX1204.2
025600   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1204.2
025700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1204.2
025800   02  FILLER                        PIC XXXX   VALUE             IX1204.2
025900     "4.2 ".                                                      IX1204.2
026000   02  FILLER                        PIC X(28)  VALUE             IX1204.2
026100            " COPY - NOT FOR DISTRIBUTION".                       IX1204.2
026200   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1204.2
026300                                                                  IX1204.2
026400 01  CCVS-H-2B.                                                   IX1204.2
026500   02  FILLER                        PIC X(15)  VALUE             IX1204.2
026600            "TEST RESULT OF ".                                    IX1204.2
026700   02  TEST-ID                       PIC X(9).                    IX1204.2
026800   02  FILLER                        PIC X(4)   VALUE             IX1204.2
026900            " IN ".                                               IX1204.2
027000   02  FILLER                        PIC X(12)  VALUE             IX1204.2
027100     " HIGH       ".                                              IX1204.2
027200   02  FILLER                        PIC X(22)  VALUE             IX1204.2
027300            " LEVEL VALIDATION FOR ".                             IX1204.2
027400   02  FILLER                        PIC X(58)  VALUE             IX1204.2
027500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1204.2
027600 01  CCVS-H-3.                                                    IX1204.2
027700     02  FILLER                      PIC X(34)  VALUE             IX1204.2
027800            " FOR OFFICIAL USE ONLY    ".                         IX1204.2
027900     02  FILLER                      PIC X(58)  VALUE             IX1204.2
028000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1204.2
028100     02  FILLER                      PIC X(28)  VALUE             IX1204.2
028200            "  COPYRIGHT   1985 ".                                IX1204.2
028300 01  CCVS-E-1.                                                    IX1204.2
028400     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1204.2
028500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1204.2
028600     02 ID-AGAIN                     PIC X(9).                    IX1204.2
028700     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1204.2
028800 01  CCVS-E-2.                                                    IX1204.2
028900     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1204.2
029000     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1204.2
029100     02 CCVS-E-2-2.                                               IX1204.2
029200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1204.2
029300         03 FILLER                   PIC X      VALUE SPACE.      IX1204.2
029400         03 ENDER-DESC               PIC X(44)  VALUE             IX1204.2
029500            "ERRORS ENCOUNTERED".                                 IX1204.2
029600 01  CCVS-E-3.                                                    IX1204.2
029700     02  FILLER                      PIC X(22)  VALUE             IX1204.2
029800            " FOR OFFICIAL USE ONLY".                             IX1204.2
029900     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1204.2
030000     02  FILLER                      PIC X(58)  VALUE             IX1204.2
030100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1204.2
030200     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1204.2
030300     02 FILLER                       PIC X(15)  VALUE             IX1204.2
030400             " COPYRIGHT 1985".                                   IX1204.2
030500 01  CCVS-E-4.                                                    IX1204.2
030600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1204.2
030700     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1204.2
030800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1204.2
030900     02 FILLER                       PIC X(40)  VALUE             IX1204.2
031000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1204.2
031100 01  XXINFO.                                                      IX1204.2
031200     02 FILLER                       PIC X(19)  VALUE             IX1204.2
031300            "*** INFORMATION ***".                                IX1204.2
031400     02 INFO-TEXT.                                                IX1204.2
031500       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1204.2
031600       04 XXCOMPUTED                 PIC X(20).                   IX1204.2
031700       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1204.2
031800       04 XXCORRECT                  PIC X(20).                   IX1204.2
031900     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1204.2
032000 01  HYPHEN-LINE.                                                 IX1204.2
032100     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1204.2
032200     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1204.2
032300-    "*****************************************".                 IX1204.2
032400     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1204.2
032500-    "******************************".                            IX1204.2
032600 01  TEST-NO                         PIC 99.                      IX1204.2
032700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1204.2
032800     "IX120A".                                                    IX1204.2
032900 PROCEDURE DIVISION.                                              IX1204.2
033000 DECLARATIVES.                                                    IX1204.2
033100                                                                  IX1204.2
033200 SECT-IX105-0002 SECTION.                                         IX1204.2
033300     USE AFTER EXCEPTION PROCEDURE ON IX-FS3.                     IX1204.2
033400 INPUT-PROCESS.                                                   IX1204.2
033500     IF TEST-NO = 5                                               IX1204.2
033600        GO TO D-C-TEST-GF-01-1.                                   IX1204.2
033700     IF STATUS-TEST-10 EQUAL TO 1                                 IX1204.2
033800        IF  IX-FS3-STAT1 EQUAL TO "1"                             IX1204.2
033900            MOVE 1 TO EOF-FLAG                                    IX1204.2
034000        ELSE                                                      IX1204.2
034100           IF  IX-FS3-STAT1 GREATER THAN "1"                      IX1204.2
034200           MOVE 1 TO PERM-ERRORS.                                 IX1204.2
034300     GO TO DECL-EXIT.                                             IX1204.2
034400 D-C-TEST-GF-01-1.                                                IX1204.2
034500     IF IX-FS3-STATUS EQUAL TO "43"                               IX1204.2
034600         GO TO D-C-PASS-GF-01-0.                                  IX1204.2
034700 D-C-FAIL-GF-01-0.                                                IX1204.2
034800     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1204.2
034900     MOVE "43" TO CORRECT-X.                                      IX1204.2
035000     MOVE "IX-5, 1.3.4, (5) C" TO RE-MARK.                        IX1204.2
035100     PERFORM D-FAIL.                                              IX1204.2
035200     GO TO D-C-WRITE-GF-01-0.                                     IX1204.2
035300 D-C-PASS-GF-01-0.                                                IX1204.2
035400     PERFORM D-PASS.                                              IX1204.2
035500 D-C-WRITE-GF-01-0.                                               IX1204.2
035600     PERFORM D-PRINT-DETAIL.                                      IX1204.2
035700 D-CLOSE-FILES.                                                   IX1204.2
035800     CLOSE IX-FS3.                                                IX1204.2
035900*    OPEN I-O RAW-DATA.                                           IX1204.2
036000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1204.2
036100*    READ RAW-DATA INVALID KEY GO TO D-END-E-2.                   IX1204.2
036200*    MOVE "OK.     " TO C-ABORT.                                  IX1204.2
036300*    MOVE PASS-COUNTER TO C-OK.                                   IX1204.2
036400*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1204.2
036500*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1204.2
036600*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1204.2
036700*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1204.2
036800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO D-END-E-2.           IX1204.2
036900*D-END-E-2.                                                       IX1204.2
037000*    CLOSE RAW-DATA.                                              IX1204.2
037100     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 IX1204.2
037200     CLOSE PRINT-FILE.                                            IX1204.2
037300 D-TERMINATE-CCVS.                                                IX1204.2
037400*    EXIT PROGRAM.                                                IX1204.2
037500*D-TERMINATE-CALL.                                                IX1204.2
037600     STOP     RUN.                                                IX1204.2
037700 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         IX1204.2
037800 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        IX1204.2
037900 D-PRINT-DETAIL.                                                  IX1204.2
038000     IF   REC-CT NOT EQUAL TO ZERO                                IX1204.2
038100          MOVE "." TO PARDOT-X                                    IX1204.2
038200          MOVE REC-CT TO DOTVALUE.                                IX1204.2
038300     MOVE TEST-RESULTS TO PRINT-REC.                              IX1204.2
038400     PERFORM D-WRITE-LINE.                                        IX1204.2
038500     IF   P-OR-F EQUAL TO "FAIL*"                                 IX1204.2
038600          PERFORM D-WRITE-LINE                                    IX1204.2
038700          PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX           IX1204.2
038800     ELSE                                                         IX1204.2
038900          PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.                  IX1204.2
039000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1204.2
039100     MOVE SPACE TO CORRECT-X.                                     IX1204.2
039200     IF   REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.           IX1204.2
039300     MOVE SPACE TO RE-MARK.                                       IX1204.2
039400 D-END-ROUTINE.                                                   IX1204.2
039500     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            IX1204.2
039600     PERFORM D-WRITE-LINE 5 TIMES.                                IX1204.2
039700 D-END-RTN-EXIT.                                                  IX1204.2
039800     MOVE CCVS-E-1 TO DUMMY-RECORD.                               IX1204.2
039900     PERFORM D-WRITE-LINE 2 TIMES.                                IX1204.2
040000 D-END-ROUTINE-1.                                                 IX1204.2
040100     ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO       IX1204.2
040200     ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.                IX1204.2
040300     ADD PASS-COUNTER TO ERROR-HOLD.                              IX1204.2
040400     MOVE PASS-COUNTER TO CCVS-E-4-1.                             IX1204.2
040500     MOVE ERROR-HOLD TO CCVS-E-4-2.                               IX1204.2
040600     MOVE CCVS-E-4 TO CCVS-E-2-2.                                 IX1204.2
040700     MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.          IX1204.2
040800  D-END-ROUTINE-12.                                               IX1204.2
040900     MOVE "TEST(S) FAILED" TO ENDER-DESC.                         IX1204.2
041000     IF  ERROR-COUNTER IS EQUAL TO ZERO                           IX1204.2
041100         MOVE "NO " TO ERROR-TOTAL                                IX1204.2
041200     ELSE                                                         IX1204.2
041300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1204.2
041400     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            IX1204.2
041500     PERFORM D-WRITE-LINE.                                        IX1204.2
041600 D-END-ROUTINE-13.                                                IX1204.2
041700     IF  DELETE-COUNTER IS EQUAL TO ZERO                          IX1204.2
041800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1204.2
041900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1204.2
042000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1204.2
042100     MOVE CCVS-E-2 TO DUMMY-RECORD.                               IX1204.2
042200     PERFORM D-WRITE-LINE.                                        IX1204.2
042300     IF   INSPECT-COUNTER EQUAL TO ZERO                           IX1204.2
042400          MOVE "NO " TO ERROR-TOTAL                               IX1204.2
042500     ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                    IX1204.2
042600     MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.             IX1204.2
042700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1204.2
042800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1204.2
042900 D-WRITE-LINE.                                                    IX1204.2
043000     ADD 1 TO RECORD-COUNT.                                       IX1204.2
043100*    IF RECORD-COUNT GREATER 42                                   IX1204.2
043200*       MOVE DUMMY-RECORD TO DUMMY-HOLD                           IX1204.2
043300*       MOVE SPACE TO DUMMY-RECORD                                IX1204.2
043400*       WRITE DUMMY-RECORD AFTER ADVANCING PAGE                   IX1204.2
043500*       MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1204.2
043600*       MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1204.2
043700*       MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1204.2
043800*       MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1204.2
043900*       MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1204.2
044000*       MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1204.2
044100*       MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN         IX1204.2
044200*       MOVE DUMMY-HOLD TO DUMMY-RECORD                           IX1204.2
044300*       MOVE ZERO TO RECORD-COUNT.                                IX1204.2
044400     PERFORM D-WRT-LN.                                            IX1204.2
044500 D-WRT-LN.                                                        IX1204.2
044600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1204.2
044700     MOVE SPACE TO DUMMY-RECORD.                                  IX1204.2
044800 D-FAIL-ROUTINE.                                                  IX1204.2
044900     IF   COMPUTED-X NOT EQUAL TO SPACE                           IX1204.2
045000          GO TO D-FAIL-ROUTINE-WRITE.                             IX1204.2
045100     IF   CORRECT-X NOT EQUAL TO SPACE GO TO D-FAIL-ROUTINE-WRITE.IX1204.2
045200     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1204.2
045300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    IX1204.2
045400     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1204.2
045500     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1204.2
045600     GO TO D-FAIL-ROUTINE-EX.                                     IX1204.2
045700 D-FAIL-ROUTINE-WRITE.                                            IX1204.2
045800     MOVE TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE         IX1204.2
045900     MOVE ANSI-REFERENCE TO COR-ANSI-REFERENCE.                   IX1204.2
046000     MOVE TEST-CORRECT TO PRINT-REC PERFORM D-WRITE-LINE 2 TIMES. IX1204.2
046100     MOVE SPACES TO COR-ANSI-REFERENCE.                           IX1204.2
046200 D-FAIL-ROUTINE-EX. EXIT.                                         IX1204.2
046300 D-BAIL-OUT.                                                      IX1204.2
046400     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    IX1204.2
046500     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            IX1204.2
046600 D-BAIL-OUT-WRITE.                                                IX1204.2
046700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1204.2
046800     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1204.2
046900     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1204.2
047000     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1204.2
047100 D-BAIL-OUT-EX. EXIT.                                             IX1204.2
047200 DECL-EXIT.  EXIT.                                                IX1204.2
047300 END DECLARATIVES.                                                IX1204.2
047400                                                                  IX1204.2
047500                                                                  IX1204.2
047600 CCVS1 SECTION.                                                   IX1204.2
047700 OPEN-FILES.                                                      IX1204.2
047800*    OPEN I-O RAW-DATA.                                           IX1204.2
047900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1204.2
048000*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1204.2
048100*    MOVE "ABORTED " TO C-ABORT.                                  IX1204.2
048200*    ADD 1 TO C-NO-OF-TESTS.                                      IX1204.2
048300*    ACCEPT C-DATE  FROM DATE.                                    IX1204.2
048400*    ACCEPT C-TIME  FROM TIME.                                    IX1204.2
048500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1204.2
048600*END-E-1.                                                         IX1204.2
048700*    CLOSE RAW-DATA.                                              IX1204.2
048800     OPEN    OUTPUT PRINT-FILE.                                   IX1204.2
048900     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1204.2
049000     MOVE    SPACE TO TEST-RESULTS.                               IX1204.2
049100     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1204.2
049200     MOVE    ZERO TO REC-SKL-SUB.                                 IX1204.2
049300     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1204.2
049400 CCVS-INIT-FILE.                                                  IX1204.2
049500     ADD     1 TO REC-SKL-SUB.                                    IX1204.2
049600     MOVE    FILE-RECORD-INFO-SKELETON                            IX1204.2
049700          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1204.2
049800 CCVS-INIT-EXIT.                                                  IX1204.2
049900     GO TO CCVS1-EXIT.                                            IX1204.2
050000 CLOSE-FILES.                                                     IX1204.2
050100*    OPEN I-O RAW-DATA.                                           IX1204.2
050200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1204.2
050300*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1204.2
050400*    MOVE "OK.     " TO C-ABORT.                                  IX1204.2
050500*    MOVE PASS-COUNTER TO C-OK.                                   IX1204.2
050600*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1204.2
050700*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1204.2
050800*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1204.2
050900*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1204.2
051000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1204.2
051100*END-E-2.                                                         IX1204.2
051200*    CLOSE RAW-DATA.                                              IX1204.2
051300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1204.2
051400 TERMINATE-CCVS.                                                  IX1204.2
051500*    EXIT PROGRAM.                                                IX1204.2
051600*TERMINATE-CALL.                                                  IX1204.2
051700     STOP     RUN.                                                IX1204.2
051800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1204.2
051900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1204.2
052000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1204.2
052100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1204.2
052200     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1204.2
052300 PRINT-DETAIL.                                                    IX1204.2
052400     IF REC-CT NOT EQUAL TO ZERO                                  IX1204.2
052500             MOVE "." TO PARDOT-X                                 IX1204.2
052600             MOVE REC-CT TO DOTVALUE.                             IX1204.2
052700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1204.2
052800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1204.2
052900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1204.2
053000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1204.2
053100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1204.2
053200     MOVE SPACE TO CORRECT-X.                                     IX1204.2
053300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1204.2
053400     MOVE     SPACE TO RE-MARK.                                   IX1204.2
053500 HEAD-ROUTINE.                                                    IX1204.2
053600     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1204.2
053700     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1204.2
053800     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1204.2
053900     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1204.2
054000 COLUMN-NAMES-ROUTINE.                                            IX1204.2
054100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1204.2
054200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1204.2
054300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1204.2
054400 END-ROUTINE.                                                     IX1204.2
054500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1204.2
054600 END-RTN-EXIT.                                                    IX1204.2
054700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1204.2
054800 END-ROUTINE-1.                                                   IX1204.2
054900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1204.2
055000      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1204.2
055100      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1204.2
055200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1204.2
055300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1204.2
055400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1204.2
055500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1204.2
055600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1204.2
055700  END-ROUTINE-12.                                                 IX1204.2
055800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1204.2
055900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1204.2
056000         MOVE "NO " TO ERROR-TOTAL                                IX1204.2
056100         ELSE                                                     IX1204.2
056200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1204.2
056300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1204.2
056400     PERFORM WRITE-LINE.                                          IX1204.2
056500 END-ROUTINE-13.                                                  IX1204.2
056600     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1204.2
056700         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1204.2
056800         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1204.2
056900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1204.2
057000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1204.2
057100      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1204.2
057200          MOVE "NO " TO ERROR-TOTAL                               IX1204.2
057300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1204.2
057400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1204.2
057500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1204.2
057600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1204.2
057700 WRITE-LINE.                                                      IX1204.2
057800     ADD 1 TO RECORD-COUNT.                                       IX1204.2
057900*    IF RECORD-COUNT GREATER 42                                   IX1204.2
058000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1204.2
058100*        MOVE SPACE TO DUMMY-RECORD                               IX1204.2
058200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1204.2
058300*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1204.2
058400*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1204.2
058500*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1204.2
058600*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1204.2
058700*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1204.2
058800*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1204.2
058900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1204.2
059000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1204.2
059100*        MOVE ZERO TO RECORD-COUNT.                               IX1204.2
059200     PERFORM WRT-LN.                                              IX1204.2
059300 WRT-LN.                                                          IX1204.2
059400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1204.2
059500     MOVE SPACE TO DUMMY-RECORD.                                  IX1204.2
059600 BLANK-LINE-PRINT.                                                IX1204.2
059700     PERFORM WRT-LN.                                              IX1204.2
059800 FAIL-ROUTINE.                                                    IX1204.2
059900     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1204.2
060000            GO TO   FAIL-ROUTINE-WRITE.                           IX1204.2
060100     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1204.2
060200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1204.2
060300     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1204.2
060400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1204.2
060500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1204.2
060600     GO TO  FAIL-ROUTINE-EX.                                      IX1204.2
060700 FAIL-ROUTINE-WRITE.                                              IX1204.2
060800     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1204.2
060900     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1204.2
061000     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1204.2
061100     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1204.2
061200 FAIL-ROUTINE-EX. EXIT.                                           IX1204.2
061300 BAIL-OUT.                                                        IX1204.2
061400     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1204.2
061500     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1204.2
061600 BAIL-OUT-WRITE.                                                  IX1204.2
061700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1204.2
061800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1204.2
061900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1204.2
062000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1204.2
062100 BAIL-OUT-EX. EXIT.                                               IX1204.2
062200 CCVS1-EXIT.                                                      IX1204.2
062300     EXIT.                                                        IX1204.2
062400                                                                  IX1204.2
062500 SECT-IX120A-0003 SECTION.                                        IX1204.2
062600 SEQ-INIT-010.                                                    IX1204.2
062700     MOVE ZERO TO TEST-NO.                                        IX1204.2
062800     MOVE "IX-FS3" TO XFILE-NAME (1).                             IX1204.2
062900     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IX1204.2
063000     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IX1204.2
063100     MOVE 000240 TO XRECORD-LENGTH (1).                           IX1204.2
063200     MOVE "RC" TO CHARS-OR-RECORDS (1).                           IX1204.2
063300     MOVE 0002 TO XBLOCK-SIZE (1).                                IX1204.2
063400     MOVE 000050 TO RECORDS-IN-FILE (1).                          IX1204.2
063500     MOVE "IX" TO XFILE-ORGANIZATION (1).                         IX1204.2
063600     MOVE "S" TO XLABEL-TYPE (1).                                 IX1204.2
063700     MOVE 000001 TO XRECORD-NUMBER (1).                           IX1204.2
063800     MOVE 0 TO COUNT-OF-RECS.                                     IX1204.2
063900                                                                  IX1204.2
064000******************************************************************IX1204.2
064100*   TEST  1                                                      *IX1204.2
064200*         OPEN OUTPUT ...                 00 EXPECTED            *IX1204.2
064300*         IX-3, 1.3.4 (1) A                                      *IX1204.2
064400*    STATUS 00 CHECK ON OUTPUT FILE IX-FS3                       *IX1204.2
064500*    THE OUTPUT STATEMENT IS SUCCESSFULLY EXECUTED               *IX1204.2
064600******************************************************************IX1204.2
064700 OPN-INIT-GF-01-0.                                                IX1204.2
064800     MOVE 1 TO STATUS-TEST-00.                                    IX1204.2
064900     MOVE SPACES TO IX-FS3-STATUS.                                IX1204.2
065000     MOVE "OPEN I-O   : 00 EXP." TO FEATURE.                      IX1204.2
065100     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1204.2
065200     OPEN                                                         IX1204.2
065300        I-O    IX-FS3.                                            IX1204.2
065400     IF IX-FS3-STATUS EQUAL TO "00"                               IX1204.2
065500         GO TO OPN-PASS-GF-01-0.                                  IX1204.2
065600 OPN-FAIL-GF-01-0.                                                IX1204.2
065700     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1204.2
065800     PERFORM FAIL.                                                IX1204.2
065900     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1204.2
066000     MOVE "00" TO CORRECT-X.                                      IX1204.2
066100     GO TO OPN-WRITE-GF-01-0.                                     IX1204.2
066200 OPN-PASS-GF-01-0.                                                IX1204.2
066300     PERFORM PASS.                                                IX1204.2
066400 OPN-WRITE-GF-01-0.                                               IX1204.2
066500     PERFORM PRINT-DETAIL.                                        IX1204.2
066600******************************************************************IX1204.2
066700*   TEST  5                                                      *IX1204.2
066800*         REWRITE WHERE THE LAST EXECUTED I-O STATEMENT PRIOR TO *IX1204.2
066900*         THE REWRITE WAS NOT A SUCCESSFULLY EXECUTED READ        IX1204.2
067000*         STATEMENT.       STATUS 43 EXPECTED.                    IX1204.2
067100*         IX-3, 1.3.4 (3) A                                      *IX1204.2
067200******************************************************************IX1204.2
067300 RWR-INIT-GF-01-0.                                                IX1204.2
067400     MOVE  5 TO TEST-NO.                                          IX1204.2
067500     MOVE SPACES TO IX-FS3-STATUS.                                IX1204.2
067600     MOVE 0 TO STATUS-TEST-00.                                    IX1204.2
067700     MOVE "REWRITE:     43 EXP." TO FEATURE.                      IX1204.2
067800     MOVE "RWR-TEST-GF-01-0" TO PAR-NAME.                         IX1204.2
067900*RWR-READ-GF-01-0.                                                IX1204.2
068000*    READ IX-FS3 AT END GO TO RWR-TEST-GF-01-0.                   IX1204.2
068100*    GO TO RWR-READ-GF-01-0.                                      IX1204.2
068200*RWR-TEST-GF-01-0.                                                IX1204.2
068300*    READ IX-FS3 AT END GO TO RWR-TEST-GF-01-1.                   IX1204.2
068400*    MOVE FILE-RECORD-INFO (1) TO IX-FS3R1-F-G-240.               IX1204.2
068500 RWR-TEST-GF-01-1.                                                IX1204.2
068600     REWRITE IX-FS3R1-F-G-240.                                    IX1204.2
068700     IF IX-FS3-STATUS EQUAL TO "43"                               IX1204.2
068800        MOVE "SHOULD HAVE EXECUTED DECLARATIVES IX-3,1.3.4(4)"    IX1204.2
068900          TO RE-MARK                                              IX1204.2
069000        GO TO RWR-WRITE-GF-01-0.                                  IX1204.2
069100 RWR-FAIL-GF-01-0.                                                IX1204.2
069200     MOVE "IX-5, 1.3.4, (5) C" TO RE-MARK.                        IX1204.2
069300 RWR-WRITE-GF-01-0.                                               IX1204.2
069400     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1204.2
069500     MOVE "43" TO CORRECT-X.                                      IX1204.2
069600     PERFORM FAIL.                                                IX1204.2
069700     PERFORM PRINT-DETAIL.                                        IX1204.2
069800     CLOSE IX-FS3.                                                IX1204.2
069900                                                                  IX1204.2
070000 TERMINATE-ROUTINE.                                               IX1204.2
070100     EXIT.                                                        IX1204.2
070200                                                                  IX1204.2
070300 CCVS-EXIT SECTION.                                               IX1204.2
070400 CCVS-999999.                                                     IX1204.2
070500     GO TO CLOSE-FILES.                                           IX1204.2
      *END-OF,IX120A                                                                  
