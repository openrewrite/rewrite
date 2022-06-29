      *HEADER,COBOL,IX113A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1134.2
000200 PROGRAM-ID.                                                      IX1134.2
000300     IX113A.                                                      IX1134.2
000400****************************************************************  IX1134.2
000500*                                                              *  IX1134.2
000600*    VALIDATION FOR:-                                          *  IX1134.2
000700*                                                              *  IX1134.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1134.2
000900*                                                              *  IX1134.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1134.2
001100*                                                              *  IX1134.2
001200****************************************************************  IX1134.2
001300*                                                                 IX1134.2
001400*    1.  THE  ROUTINE  CREATES  THE  MASS  STORAGE  FILE  IX-FS3  IX1134.2
001500*        CONTAINING  50  RECORDS. EACH BLOCK CONTAINS 2 RECORDS,  IX1134.2
001600*        EACH  RECORD  CONTAINS  240 CHARACTERS, ORGANIZATION IS  IX1134.2
001700*        INDEXED,  ACCESS  IS SEQUENTIAL.                         IX1134.2
001800*                                                                 IX1134.2
001900*    2. THE ROUTINE READS THE CREATED FILE,VERIFIES IT AND       *IX1134.2
002000*       CHECKS THE FILE STATUS CODES:                             IX1134.2
002100*           00  -  AFTER OPEN OUTPUT                              IX1134.2
002200*           00  -  AFTER WRITE                                    IX1134.2
002300*           00  -  AFTER CLOSE OUTPUT                             IX1134.2
002400*           42  -  AFTER CLOSE OUTPUT                             IX1134.2
002500*                                                                 IX1134.2
002600*    4. X-CARDS USED IN THIS PROGRAM:                             IX1134.2
002700*                                                                 IX1134.2
002800*                 XXXXX024                                        IX1134.2
002900*                 XXXXX055.                                       IX1134.2
003000*         P       XXXXX062.                                       IX1134.2
003100*                 XXXXX082.                                       IX1134.2
003200*                 XXXXX083.                                       IX1134.2
003300*         C       XXXXX084                                        IX1134.2
003400*                                                                 IX1134.2
003500*                                                                 IX1134.2
003600 ENVIRONMENT DIVISION.                                            IX1134.2
003700 CONFIGURATION SECTION.                                           IX1134.2
003800 SOURCE-COMPUTER.                                                 IX1134.2
003900     XXXXX082.                                                    IX1134.2
004000 OBJECT-COMPUTER.                                                 IX1134.2
004100     XXXXX083.                                                    IX1134.2
004200 INPUT-OUTPUT SECTION.                                            IX1134.2
004300 FILE-CONTROL.                                                    IX1134.2
004400*    SELECT RAW-DATA   ASSIGN TO                                  IX1134.2
004500*    XXXXX062                                                     IX1134.2
004600*           ORGANIZATION IS INDEXED                               IX1134.2
004700*           ACCESS MODE IS RANDOM                                 IX1134.2
004800*           RECORD KEY IS RAW-DATA-KEY.                           IX1134.2
004900*                                                                 IX1134.2
005000     SELECT PRINT-FILE ASSIGN TO                                  IX1134.2
005100     XXXXX055.                                                    IX1134.2
005200*                                                                 IX1134.2
005300     SELECT IX-FS3 ASSIGN                                         IX1134.2
005400     XXXXX024                                                     IX1134.2
005500     ORGANIZATION IS INDEXED                                      IX1134.2
005600     ACCESS MODE IS SEQUENTIAL                                    IX1134.2
005700     RECORD KEY IS IX-FS3-KEY                                     IX1134.2
005800     FILE STATUS IS IX-FS3-STATUS.                                IX1134.2
005900                                                                  IX1134.2
006000 DATA DIVISION.                                                   IX1134.2
006100                                                                  IX1134.2
006200 FILE SECTION.                                                    IX1134.2
006300*                                                                 IX1134.2
006400*FD  RAW-DATA.                                                    IX1134.2
006500*                                                                 IX1134.2
006600*01  RAW-DATA-SATZ.                                               IX1134.2
006700*    05  RAW-DATA-KEY        PIC X(6).                            IX1134.2
006800*    05  C-DATE              PIC 9(6).                            IX1134.2
006900*    05  C-TIME              PIC 9(8).                            IX1134.2
007000*    05  C-NO-OF-TESTS       PIC 99.                              IX1134.2
007100*    05  C-OK                PIC 999.                             IX1134.2
007200*    05  C-ALL               PIC 999.                             IX1134.2
007300*    05  C-FAIL              PIC 999.                             IX1134.2
007400*    05  C-DELETED           PIC 999.                             IX1134.2
007500*    05  C-INSPECT           PIC 999.                             IX1134.2
007600*    05  C-NOTE              PIC X(13).                           IX1134.2
007700*    05  C-INDENT            PIC X.                               IX1134.2
007800*    05  C-ABORT             PIC X(8).                            IX1134.2
007900                                                                  IX1134.2
008000 FD  PRINT-FILE.                                                  IX1134.2
008100                                                                  IX1134.2
008200 01  PRINT-REC               PIC X(120).                          IX1134.2
008300                                                                  IX1134.2
008400 01  DUMMY-RECORD            PIC X(120).                          IX1134.2
008500                                                                  IX1134.2
008600 FD  IX-FS3                                                       IX1134.2
008700C       DATA RECORDS IX-FS3R1-F-G-240                             IX1134.2
008800C       LABEL RECORD STANDARD                                     IX1134.2
008900        RECORD 240                                                IX1134.2
009000        BLOCK CONTAINS 2 RECORDS.                                 IX1134.2
009100                                                                  IX1134.2
009200 01  IX-FS3R1-F-G-240.                                            IX1134.2
009300     05  IX-FS3-REC-120      PIC X(120).                          IX1134.2
009400     05  IX-FS3-REC-120-240.                                      IX1134.2
009500         10  FILLER          PIC X(8).                            IX1134.2
009600         10  IX-FS3-KEY      PIC X(29).                           IX1134.2
009700         10  FILLER          PIC X(9).                            IX1134.2
009800         10  IX-FS3-ALTER-KEY  PIC X(29).                         IX1134.2
009900         10  FILLER            PIC X(45).                         IX1134.2
010000                                                                  IX1134.2
010100                                                                  IX1134.2
010200 WORKING-STORAGE SECTION.                                         IX1134.2
010300                                                                  IX1134.2
010400 01  GRP-0101.                                                    IX1134.2
010500     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1134.2
010600     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1134.2
010700     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1134.2
010800                                                                  IX1134.2
010900 01  GRP-0102.                                                    IX1134.2
011000     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1134.2
011100     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1134.2
011200     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1134.2
011300                                                                  IX1134.2
011400 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1134.2
011500                                                                  IX1134.2
011600 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1134.2
011700                                                                  IX1134.2
011800 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1134.2
011900                                                                  IX1134.2
012000 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1134.2
012100                                                                  IX1134.2
012200 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1134.2
012300                                                                  IX1134.2
012400 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1134.2
012500                                                                  IX1134.2
012600 01  STATUS-TEST-10          PIC 9 VALUE ZERO.                    IX1134.2
012700 01  STATUS-TEST-READ        PIC 9 VALUE ZERO.                    IX1134.2
012800                                                                  IX1134.2
012900 01  IX-FS3-STATUS.                                               IX1134.2
013000     05  IX-FS3-STAT1        PIC X.                               IX1134.2
013100     05  IX-FS3-STAT2        PIC X.                               IX1134.2
013200                                                                  IX1134.2
013300 01  COUNT-OF-RECS           PIC 9(5).                            IX1134.2
013400                                                                  IX1134.2
013500 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1134.2
013600                                                                  IX1134.2
013700 01  FILE-RECORD-INFORMATION-REC.                                 IX1134.2
013800     05  FILE-RECORD-INFO-SKELETON.                               IX1134.2
013900         10  FILLER          PIC X(48) VALUE                      IX1134.2
014000              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1134.2
014100         10  FILLER          PIC X(46) VALUE                      IX1134.2
014200                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1134.2
014300         10  FILLER          PIC X(26) VALUE                      IX1134.2
014400                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1134.2
014500         10  FILLER          PIC X(37) VALUE                      IX1134.2
014600                         ",RECKEY=                             ". IX1134.2
014700         10  FILLER          PIC X(38) VALUE                      IX1134.2
014800                        ",ALTKEY1=                             ". IX1134.2
014900         10  FILLER          PIC X(38) VALUE                      IX1134.2
015000                        ",ALTKEY2=                             ". IX1134.2
015100         10  FILLER          PIC X(7) VALUE SPACE.                IX1134.2
015200     05  FILE-RECORD-INFO             OCCURS 10.                  IX1134.2
015300         10  FILE-RECORD-INFO-P1-120.                             IX1134.2
015400             15  FILLER      PIC X(5).                            IX1134.2
015500             15  XFILE-NAME  PIC X(6).                            IX1134.2
015600             15  FILLER      PIC X(8).                            IX1134.2
015700             15  XRECORD-NAME  PIC X(6).                          IX1134.2
015800             15  FILLER        PIC X(1).                          IX1134.2
015900             15  REELUNIT-NUMBER  PIC 9(1).                       IX1134.2
016000             15  FILLER           PIC X(7).                       IX1134.2
016100             15  XRECORD-NUMBER   PIC 9(6).                       IX1134.2
016200             15  FILLER           PIC X(6).                       IX1134.2
016300             15  UPDATE-NUMBER    PIC 9(2).                       IX1134.2
016400             15  FILLER           PIC X(5).                       IX1134.2
016500             15  ODO-NUMBER       PIC 9(4).                       IX1134.2
016600             15  FILLER           PIC X(5).                       IX1134.2
016700             15  XPROGRAM-NAME    PIC X(5).                       IX1134.2
016800             15  FILLER           PIC X(7).                       IX1134.2
016900             15  XRECORD-LENGTH   PIC 9(6).                       IX1134.2
017000             15  FILLER           PIC X(7).                       IX1134.2
017100             15  CHARS-OR-RECORDS  PIC X(2).                      IX1134.2
017200             15  FILLER            PIC X(1).                      IX1134.2
017300             15  XBLOCK-SIZE       PIC 9(4).                      IX1134.2
017400             15  FILLER            PIC X(6).                      IX1134.2
017500             15  RECORDS-IN-FILE   PIC 9(6).                      IX1134.2
017600             15  FILLER            PIC X(5).                      IX1134.2
017700             15  XFILE-ORGANIZATION  PIC X(2).                    IX1134.2
017800             15  FILLER              PIC X(6).                    IX1134.2
017900             15  XLABEL-TYPE         PIC X(1).                    IX1134.2
018000         10  FILE-RECORD-INFO-P121-240.                           IX1134.2
018100             15  FILLER              PIC X(8).                    IX1134.2
018200             15  XRECORD-KEY         PIC X(29).                   IX1134.2
018300             15  FILLER              PIC X(9).                    IX1134.2
018400             15  ALTERNATE-KEY1      PIC X(29).                   IX1134.2
018500             15  FILLER              PIC X(9).                    IX1134.2
018600             15  ALTERNATE-KEY2      PIC X(29).                   IX1134.2
018700             15  FILLER              PIC X(7).                    IX1134.2
018800                                                                  IX1134.2
018900 01  TEST-RESULTS.                                                IX1134.2
019000     02 FILLER                   PIC X      VALUE SPACE.          IX1134.2
019100     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1134.2
019200     02 FILLER                   PIC X      VALUE SPACE.          IX1134.2
019300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1134.2
019400     02 FILLER                   PIC X      VALUE SPACE.          IX1134.2
019500     02  PAR-NAME.                                                IX1134.2
019600       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1134.2
019700       03  PARDOT-X              PIC X      VALUE SPACE.          IX1134.2
019800       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1134.2
019900     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1134.2
020000     02 RE-MARK                  PIC X(61).                       IX1134.2
020100 01  TEST-COMPUTED.                                               IX1134.2
020200     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1134.2
020300     02 FILLER                   PIC X(17)  VALUE                 IX1134.2
020400            "       COMPUTED=".                                   IX1134.2
020500     02 COMPUTED-X.                                               IX1134.2
020600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1134.2
020700     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1134.2
020800                                 PIC -9(9).9(9).                  IX1134.2
020900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1134.2
021000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1134.2
021100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1134.2
021200     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1134.2
021300         04 COMPUTED-18V0                    PIC -9(18).          IX1134.2
021400         04 FILLER                           PIC X.               IX1134.2
021500     03 FILLER PIC X(50) VALUE SPACE.                             IX1134.2
021600 01  TEST-CORRECT.                                                IX1134.2
021700     02 FILLER PIC X(30) VALUE SPACE.                             IX1134.2
021800     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1134.2
021900     02 CORRECT-X.                                                IX1134.2
022000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1134.2
022100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1134.2
022200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1134.2
022300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1134.2
022400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1134.2
022500     03      CR-18V0 REDEFINES CORRECT-A.                         IX1134.2
022600         04 CORRECT-18V0                     PIC -9(18).          IX1134.2
022700         04 FILLER                           PIC X.               IX1134.2
022800     03 FILLER PIC X(2) VALUE SPACE.                              IX1134.2
022900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1134.2
023000 01  CCVS-C-1.                                                    IX1134.2
023100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1134.2
023200-    "SS  PARAGRAPH-NAME                                          IX1134.2
023300-    "       REMARKS".                                            IX1134.2
023400     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1134.2
023500 01  CCVS-C-2.                                                    IX1134.2
023600     02 FILLER                     PIC X        VALUE SPACE.      IX1134.2
023700     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1134.2
023800     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1134.2
023900     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1134.2
024000     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1134.2
024100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1134.2
024200 01  REC-CT                        PIC 99       VALUE ZERO.       IX1134.2
024300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1134.2
024400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1134.2
024500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1134.2
024600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1134.2
024700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1134.2
024800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1134.2
024900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1134.2
025000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1134.2
025100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1134.2
025200 01  CCVS-H-1.                                                    IX1134.2
025300     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1134.2
025400     02  FILLER                    PIC X(42)    VALUE             IX1134.2
025500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1134.2
025600     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1134.2
025700 01  CCVS-H-2A.                                                   IX1134.2
025800   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1134.2
025900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1134.2
026000   02  FILLER                        PIC XXXX   VALUE             IX1134.2
026100     "4.2 ".                                                      IX1134.2
026200   02  FILLER                        PIC X(28)  VALUE             IX1134.2
026300            " COPY - NOT FOR DISTRIBUTION".                       IX1134.2
026400   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1134.2
026500                                                                  IX1134.2
026600 01  CCVS-H-2B.                                                   IX1134.2
026700   02  FILLER                        PIC X(15)  VALUE             IX1134.2
026800            "TEST RESULT OF ".                                    IX1134.2
026900   02  TEST-ID                       PIC X(9).                    IX1134.2
027000   02  FILLER                        PIC X(4)   VALUE             IX1134.2
027100            " IN ".                                               IX1134.2
027200   02  FILLER                        PIC X(12)  VALUE             IX1134.2
027300     " HIGH       ".                                              IX1134.2
027400   02  FILLER                        PIC X(22)  VALUE             IX1134.2
027500            " LEVEL VALIDATION FOR ".                             IX1134.2
027600   02  FILLER                        PIC X(58)  VALUE             IX1134.2
027700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1134.2
027800 01  CCVS-H-3.                                                    IX1134.2
027900     02  FILLER                      PIC X(34)  VALUE             IX1134.2
028000            " FOR OFFICIAL USE ONLY    ".                         IX1134.2
028100     02  FILLER                      PIC X(58)  VALUE             IX1134.2
028200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1134.2
028300     02  FILLER                      PIC X(28)  VALUE             IX1134.2
028400            "  COPYRIGHT   1985 ".                                IX1134.2
028500 01  CCVS-E-1.                                                    IX1134.2
028600     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1134.2
028700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1134.2
028800     02 ID-AGAIN                     PIC X(9).                    IX1134.2
028900     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1134.2
029000 01  CCVS-E-2.                                                    IX1134.2
029100     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1134.2
029200     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1134.2
029300     02 CCVS-E-2-2.                                               IX1134.2
029400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1134.2
029500         03 FILLER                   PIC X      VALUE SPACE.      IX1134.2
029600         03 ENDER-DESC               PIC X(44)  VALUE             IX1134.2
029700            "ERRORS ENCOUNTERED".                                 IX1134.2
029800 01  CCVS-E-3.                                                    IX1134.2
029900     02  FILLER                      PIC X(22)  VALUE             IX1134.2
030000            " FOR OFFICIAL USE ONLY".                             IX1134.2
030100     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1134.2
030200     02  FILLER                      PIC X(58)  VALUE             IX1134.2
030300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1134.2
030400     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1134.2
030500     02 FILLER                       PIC X(15)  VALUE             IX1134.2
030600             " COPYRIGHT 1985".                                   IX1134.2
030700 01  CCVS-E-4.                                                    IX1134.2
030800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1134.2
030900     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1134.2
031000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1134.2
031100     02 FILLER                       PIC X(40)  VALUE             IX1134.2
031200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1134.2
031300 01  XXINFO.                                                      IX1134.2
031400     02 FILLER                       PIC X(19)  VALUE             IX1134.2
031500            "*** INFORMATION ***".                                IX1134.2
031600     02 INFO-TEXT.                                                IX1134.2
031700       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1134.2
031800       04 XXCOMPUTED                 PIC X(20).                   IX1134.2
031900       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1134.2
032000       04 XXCORRECT                  PIC X(20).                   IX1134.2
032100     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1134.2
032200 01  HYPHEN-LINE.                                                 IX1134.2
032300     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1134.2
032400     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1134.2
032500-    "*****************************************".                 IX1134.2
032600     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1134.2
032700-    "******************************".                            IX1134.2
032800 01  TEST-NO                         PIC 99.                      IX1134.2
032900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1134.2
033000     "IX113A".                                                    IX1134.2
033100 PROCEDURE DIVISION.                                              IX1134.2
033200 DECLARATIVES.                                                    IX1134.2
033300                                                                  IX1134.2
033400 SECT-IX105-0002 SECTION.                                         IX1134.2
033500     USE AFTER EXCEPTION PROCEDURE ON IX-FS3.                     IX1134.2
033600 INPUT-PROCESS.                                                   IX1134.2
033700     IF TEST-NO = 5                                               IX1134.2
033800        GO TO D-C-TEST-GF-02-1.                                   IX1134.2
033900     IF STATUS-TEST-10 EQUAL TO 1                                 IX1134.2
034000        IF  IX-FS3-STAT1 EQUAL TO "1"                             IX1134.2
034100            MOVE 1 TO EOF-FLAG                                    IX1134.2
034200        ELSE                                                      IX1134.2
034300           IF  IX-FS3-STAT1 GREATER THAN "1"                      IX1134.2
034400           MOVE 1 TO PERM-ERRORS.                                 IX1134.2
034500     GO TO DECL-EXIT.                                             IX1134.2
034600 D-C-TEST-GF-02-1.                                                IX1134.2
034700     IF IX-FS3-STATUS EQUAL TO "42"                               IX1134.2
034800         GO TO D-C-PASS-GF-02-0.                                  IX1134.2
034900 D-C-FAIL-GF-02-0.                                                IX1134.2
035000     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1134.2
035100     MOVE "42" TO CORRECT-X.                                      IX1134.2
035200     MOVE "IX-5, 1.3.4, (5) B" TO RE-MARK.                        IX1134.2
035300     PERFORM D-FAIL.                                              IX1134.2
035400     GO TO D-C-WRITE-GF-02-0.                                     IX1134.2
035500 D-C-PASS-GF-02-0.                                                IX1134.2
035600     PERFORM D-PASS.                                              IX1134.2
035700 D-C-WRITE-GF-02-0.                                               IX1134.2
035800     PERFORM D-PRINT-DETAIL.                                      IX1134.2
035900 D-CLOSE-FILES.                                                   IX1134.2
036000*    OPEN I-O RAW-DATA.                                           IX1134.2
036100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1134.2
036200*    READ RAW-DATA INVALID KEY GO TO D-END-E-2.                   IX1134.2
036300*    MOVE "OK.     " TO C-ABORT.                                  IX1134.2
036400*    MOVE PASS-COUNTER TO C-OK.                                   IX1134.2
036500*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1134.2
036600*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1134.2
036700*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1134.2
036800*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1134.2
036900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO D-END-E-2.           IX1134.2
037000*D-END-E-2.                                                       IX1134.2
037100*    CLOSE RAW-DATA.                                              IX1134.2
037200     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 IX1134.2
037300     CLOSE PRINT-FILE.                                            IX1134.2
037400 D-TERMINATE-CCVS.                                                IX1134.2
037500*    EXIT PROGRAM.                                                IX1134.2
037600*D-TERMINATE-CALL.                                                IX1134.2
037700     STOP     RUN.                                                IX1134.2
037800 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         IX1134.2
037900 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        IX1134.2
038000 D-PRINT-DETAIL.                                                  IX1134.2
038100     IF   REC-CT NOT EQUAL TO ZERO                                IX1134.2
038200          MOVE "." TO PARDOT-X                                    IX1134.2
038300          MOVE REC-CT TO DOTVALUE.                                IX1134.2
038400     MOVE TEST-RESULTS TO PRINT-REC.                              IX1134.2
038500     PERFORM D-WRITE-LINE.                                        IX1134.2
038600     IF   P-OR-F EQUAL TO "FAIL*"                                 IX1134.2
038700          PERFORM D-WRITE-LINE                                    IX1134.2
038800          PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX           IX1134.2
038900     ELSE                                                         IX1134.2
039000          PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.                  IX1134.2
039100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1134.2
039200     MOVE SPACE TO CORRECT-X.                                     IX1134.2
039300     IF   REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.           IX1134.2
039400     MOVE SPACE TO RE-MARK.                                       IX1134.2
039500 D-END-ROUTINE.                                                   IX1134.2
039600     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            IX1134.2
039700     PERFORM D-WRITE-LINE 5 TIMES.                                IX1134.2
039800 D-END-RTN-EXIT.                                                  IX1134.2
039900     MOVE CCVS-E-1 TO DUMMY-RECORD.                               IX1134.2
040000     PERFORM D-WRITE-LINE 2 TIMES.                                IX1134.2
040100 D-END-ROUTINE-1.                                                 IX1134.2
040200     ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO       IX1134.2
040300     ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.                IX1134.2
040400     ADD PASS-COUNTER TO ERROR-HOLD.                              IX1134.2
040500     MOVE PASS-COUNTER TO CCVS-E-4-1.                             IX1134.2
040600     MOVE ERROR-HOLD TO CCVS-E-4-2.                               IX1134.2
040700     MOVE CCVS-E-4 TO CCVS-E-2-2.                                 IX1134.2
040800     MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.          IX1134.2
040900  D-END-ROUTINE-12.                                               IX1134.2
041000     MOVE "TEST(S) FAILED" TO ENDER-DESC.                         IX1134.2
041100     IF  ERROR-COUNTER IS EQUAL TO ZERO                           IX1134.2
041200         MOVE "NO " TO ERROR-TOTAL                                IX1134.2
041300     ELSE                                                         IX1134.2
041400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1134.2
041500     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            IX1134.2
041600     PERFORM D-WRITE-LINE.                                        IX1134.2
041700 D-END-ROUTINE-13.                                                IX1134.2
041800     IF  DELETE-COUNTER IS EQUAL TO ZERO                          IX1134.2
041900         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1134.2
042000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1134.2
042100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1134.2
042200     MOVE CCVS-E-2 TO DUMMY-RECORD.                               IX1134.2
042300     PERFORM D-WRITE-LINE.                                        IX1134.2
042400     IF   INSPECT-COUNTER EQUAL TO ZERO                           IX1134.2
042500          MOVE "NO " TO ERROR-TOTAL                               IX1134.2
042600     ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                    IX1134.2
042700     MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.             IX1134.2
042800     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1134.2
042900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1134.2
043000 D-WRITE-LINE.                                                    IX1134.2
043100     ADD 1 TO RECORD-COUNT.                                       IX1134.2
043200*    IF RECORD-COUNT GREATER 42                                   IX1134.2
043300*       MOVE DUMMY-RECORD TO DUMMY-HOLD                           IX1134.2
043400*       MOVE SPACE TO DUMMY-RECORD                                IX1134.2
043500*       WRITE DUMMY-RECORD AFTER ADVANCING PAGE                   IX1134.2
043600*       MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1134.2
043700*       MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1134.2
043800*       MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1134.2
043900*       MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1134.2
044000*       MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1134.2
044100*       MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1134.2
044200*       MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN         IX1134.2
044300*       MOVE DUMMY-HOLD TO DUMMY-RECORD                           IX1134.2
044400*       MOVE ZERO TO RECORD-COUNT.                                IX1134.2
044500     PERFORM D-WRT-LN.                                            IX1134.2
044600 D-WRT-LN.                                                        IX1134.2
044700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1134.2
044800     MOVE SPACE TO DUMMY-RECORD.                                  IX1134.2
044900 D-FAIL-ROUTINE.                                                  IX1134.2
045000     IF   COMPUTED-X NOT EQUAL TO SPACE                           IX1134.2
045100          GO TO D-FAIL-ROUTINE-WRITE.                             IX1134.2
045200     IF   CORRECT-X NOT EQUAL TO SPACE GO TO D-FAIL-ROUTINE-WRITE.IX1134.2
045300     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1134.2
045400     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    IX1134.2
045500     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1134.2
045600     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1134.2
045700     GO TO D-FAIL-ROUTINE-EX.                                     IX1134.2
045800 D-FAIL-ROUTINE-WRITE.                                            IX1134.2
045900     MOVE TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE         IX1134.2
046000     MOVE ANSI-REFERENCE TO COR-ANSI-REFERENCE.                   IX1134.2
046100     MOVE TEST-CORRECT TO PRINT-REC PERFORM D-WRITE-LINE 2 TIMES. IX1134.2
046200     MOVE SPACES TO COR-ANSI-REFERENCE.                           IX1134.2
046300 D-FAIL-ROUTINE-EX. EXIT.                                         IX1134.2
046400 D-BAIL-OUT.                                                      IX1134.2
046500     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    IX1134.2
046600     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            IX1134.2
046700 D-BAIL-OUT-WRITE.                                                IX1134.2
046800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1134.2
046900     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1134.2
047000     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1134.2
047100     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1134.2
047200 D-BAIL-OUT-EX. EXIT.                                             IX1134.2
047300 DECL-EXIT.  EXIT.                                                IX1134.2
047400 END DECLARATIVES.                                                IX1134.2
047500                                                                  IX1134.2
047600                                                                  IX1134.2
047700 CCVS1 SECTION.                                                   IX1134.2
047800 OPEN-FILES.                                                      IX1134.2
047900*    OPEN I-O RAW-DATA.                                           IX1134.2
048000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1134.2
048100*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1134.2
048200*    MOVE "ABORTED " TO C-ABORT.                                  IX1134.2
048300*    ADD 1 TO C-NO-OF-TESTS.                                      IX1134.2
048400*    ACCEPT C-DATE  FROM DATE.                                    IX1134.2
048500*    ACCEPT C-TIME  FROM TIME.                                    IX1134.2
048600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1134.2
048700*END-E-1.                                                         IX1134.2
048800*    CLOSE RAW-DATA.                                              IX1134.2
048900     OPEN    OUTPUT PRINT-FILE.                                   IX1134.2
049000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1134.2
049100     MOVE    SPACE TO TEST-RESULTS.                               IX1134.2
049200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1134.2
049300     MOVE    ZERO TO REC-SKL-SUB.                                 IX1134.2
049400     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1134.2
049500 CCVS-INIT-FILE.                                                  IX1134.2
049600     ADD     1 TO REC-SKL-SUB.                                    IX1134.2
049700     MOVE    FILE-RECORD-INFO-SKELETON                            IX1134.2
049800          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1134.2
049900 CCVS-INIT-EXIT.                                                  IX1134.2
050000     GO TO CCVS1-EXIT.                                            IX1134.2
050100 CLOSE-FILES.                                                     IX1134.2
050200*    OPEN I-O RAW-DATA.                                           IX1134.2
050300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1134.2
050400*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1134.2
050500*    MOVE "OK.     " TO C-ABORT.                                  IX1134.2
050600*    MOVE PASS-COUNTER TO C-OK.                                   IX1134.2
050700*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1134.2
050800*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1134.2
050900*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1134.2
051000*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1134.2
051100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1134.2
051200*END-E-2.                                                         IX1134.2
051300*    CLOSE RAW-DATA.                                              IX1134.2
051400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1134.2
051500 TERMINATE-CCVS.                                                  IX1134.2
051600*    EXIT PROGRAM.                                                IX1134.2
051700*TERMINATE-CALL.                                                  IX1134.2
051800     STOP     RUN.                                                IX1134.2
051900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1134.2
052000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1134.2
052100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1134.2
052200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1134.2
052300     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1134.2
052400 PRINT-DETAIL.                                                    IX1134.2
052500     IF REC-CT NOT EQUAL TO ZERO                                  IX1134.2
052600             MOVE "." TO PARDOT-X                                 IX1134.2
052700             MOVE REC-CT TO DOTVALUE.                             IX1134.2
052800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1134.2
052900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1134.2
053000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1134.2
053100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1134.2
053200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1134.2
053300     MOVE SPACE TO CORRECT-X.                                     IX1134.2
053400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1134.2
053500     MOVE     SPACE TO RE-MARK.                                   IX1134.2
053600 HEAD-ROUTINE.                                                    IX1134.2
053700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1134.2
053800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1134.2
053900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1134.2
054000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1134.2
054100 COLUMN-NAMES-ROUTINE.                                            IX1134.2
054200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1134.2
054300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1134.2
054400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1134.2
054500 END-ROUTINE.                                                     IX1134.2
054600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1134.2
054700 END-RTN-EXIT.                                                    IX1134.2
054800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1134.2
054900 END-ROUTINE-1.                                                   IX1134.2
055000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1134.2
055100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1134.2
055200      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1134.2
055300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1134.2
055400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1134.2
055500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1134.2
055600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1134.2
055700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1134.2
055800  END-ROUTINE-12.                                                 IX1134.2
055900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1134.2
056000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1134.2
056100         MOVE "NO " TO ERROR-TOTAL                                IX1134.2
056200         ELSE                                                     IX1134.2
056300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1134.2
056400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1134.2
056500     PERFORM WRITE-LINE.                                          IX1134.2
056600 END-ROUTINE-13.                                                  IX1134.2
056700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1134.2
056800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1134.2
056900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1134.2
057000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1134.2
057100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1134.2
057200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1134.2
057300          MOVE "NO " TO ERROR-TOTAL                               IX1134.2
057400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1134.2
057500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1134.2
057600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1134.2
057700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1134.2
057800 WRITE-LINE.                                                      IX1134.2
057900     ADD 1 TO RECORD-COUNT.                                       IX1134.2
058000*    IF RECORD-COUNT GREATER 42                                   IX1134.2
058100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1134.2
058200*        MOVE SPACE TO DUMMY-RECORD                               IX1134.2
058300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1134.2
058400*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1134.2
058500*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1134.2
058600*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1134.2
058700*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1134.2
058800*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1134.2
058900*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1134.2
059000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1134.2
059100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1134.2
059200*        MOVE ZERO TO RECORD-COUNT.                               IX1134.2
059300     PERFORM WRT-LN.                                              IX1134.2
059400 WRT-LN.                                                          IX1134.2
059500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1134.2
059600     MOVE SPACE TO DUMMY-RECORD.                                  IX1134.2
059700 BLANK-LINE-PRINT.                                                IX1134.2
059800     PERFORM WRT-LN.                                              IX1134.2
059900 FAIL-ROUTINE.                                                    IX1134.2
060000     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1134.2
060100            GO TO   FAIL-ROUTINE-WRITE.                           IX1134.2
060200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1134.2
060300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1134.2
060400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1134.2
060500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1134.2
060600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1134.2
060700     GO TO  FAIL-ROUTINE-EX.                                      IX1134.2
060800 FAIL-ROUTINE-WRITE.                                              IX1134.2
060900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1134.2
061000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1134.2
061100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1134.2
061200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1134.2
061300 FAIL-ROUTINE-EX. EXIT.                                           IX1134.2
061400 BAIL-OUT.                                                        IX1134.2
061500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1134.2
061600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1134.2
061700 BAIL-OUT-WRITE.                                                  IX1134.2
061800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1134.2
061900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1134.2
062000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1134.2
062100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1134.2
062200 BAIL-OUT-EX. EXIT.                                               IX1134.2
062300 CCVS1-EXIT.                                                      IX1134.2
062400     EXIT.                                                        IX1134.2
062500                                                                  IX1134.2
062600 SECT-IX113A-0003 SECTION.                                        IX1134.2
062700 SEQ-INIT-010.                                                    IX1134.2
062800     MOVE ZERO TO TEST-NO.                                        IX1134.2
062900     MOVE "IX-FS3" TO XFILE-NAME (1).                             IX1134.2
063000     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IX1134.2
063100     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IX1134.2
063200     MOVE 000240 TO XRECORD-LENGTH (1).                           IX1134.2
063300     MOVE "RC" TO CHARS-OR-RECORDS (1).                           IX1134.2
063400     MOVE 0002 TO XBLOCK-SIZE (1).                                IX1134.2
063500     MOVE 000050 TO RECORDS-IN-FILE (1).                          IX1134.2
063600     MOVE "IX" TO XFILE-ORGANIZATION (1).                         IX1134.2
063700     MOVE "S" TO XLABEL-TYPE (1).                                 IX1134.2
063800     MOVE 000001 TO XRECORD-NUMBER (1).                           IX1134.2
063900     MOVE 0 TO COUNT-OF-RECS.                                     IX1134.2
064000                                                                  IX1134.2
064100******************************************************************IX1134.2
064200*   TEST  1                                                      *IX1134.2
064300*         OPEN OUTPUT ...                 00 EXPECTED            *IX1134.2
064400*         IX-3, 1.3.4 (1) A                                      *IX1134.2
064500*    STATUS 00 CHECK ON OUTPUT FILE IX-FS3                       *IX1134.2
064600*    THE OUTPUT STATEMENT IS SUCCESSFULLY EXECUTED               *IX1134.2
064700******************************************************************IX1134.2
064800 OPN-INIT-GF-01-0.                                                IX1134.2
064900     MOVE 1 TO STATUS-TEST-00.                                    IX1134.2
065000     MOVE SPACES TO IX-FS3-STATUS.                                IX1134.2
065100     MOVE "OPEN OUTPUT: 00 EXP." TO FEATURE.                      IX1134.2
065200     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1134.2
065300     OPEN                                                         IX1134.2
065400        OUTPUT IX-FS3.                                            IX1134.2
065500     IF IX-FS3-STATUS EQUAL TO "00"                               IX1134.2
065600         GO TO OPN-PASS-GF-01-0.                                  IX1134.2
065700 OPN-FAIL-GF-01-0.                                                IX1134.2
065800     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1134.2
065900     PERFORM FAIL.                                                IX1134.2
066000     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1134.2
066100     MOVE "00" TO CORRECT-X.                                      IX1134.2
066200     GO TO OPN-WRITE-GF-01-0.                                     IX1134.2
066300 OPN-PASS-GF-01-0.                                                IX1134.2
066400     PERFORM PASS.                                                IX1134.2
066500 OPN-WRITE-GF-01-0.                                               IX1134.2
066600     PERFORM PRINT-DETAIL.                                        IX1134.2
066700******************************************************************IX1134.2
066800*   TEST  2                                                      *IX1134.2
066900*         WRITE                           00 EXPECTED            *IX1134.2
067000*         IX-3, 1.3.4 (1) A                                      *IX1134.2
067100*    CREATING A INDEXED FILE WITH 50 RECORDS                     *IX1134.2
067200*    KEY:  FROM  000000001 TO 000000050                          *IX1134.2
067300******************************************************************IX1134.2
067400 WRI-INIT-GF-01-0.                                                IX1134.2
067500     MOVE SPACES TO IX-FS3-STATUS.                                IX1134.2
067600     MOVE 0 TO STATUS-TEST-00.                                    IX1134.2
067700     MOVE "WRITE: 00 EXPECTED" TO FEATURE.                        IX1134.2
067800     MOVE "WRI-TEST-GF-01-0" TO PAR-NAME.                         IX1134.2
067900 WRI-TEST-GF-01-0.                                                IX1134.2
068000     MOVE XRECORD-NUMBER (1) TO GRP-0101-KEY, COUNT-OF-RECS.      IX1134.2
068100     MOVE GRP-0101 TO XRECORD-KEY (1).                            IX1134.2
068200     MOVE GRP-0102 TO ALTERNATE-KEY1 (1).                         IX1134.2
068300*    THE VALUE OF THE ALTERNATE KEY IS 50 TIMES UNCHANGED        *IX1134.2
068400     MOVE FILE-RECORD-INFO (1) TO IX-FS3R1-F-G-240.               IX1134.2
068500     WRITE IX-FS3R1-F-G-240.                                      IX1134.2
068600     IF IX-FS3-STATUS  NOT = "00"                                 IX1134.2
068700         MOVE 1 TO STATUS-TEST-00                                 IX1134.2
068800         GO TO WRI-FAIL-GF-01-0.                                  IX1134.2
068900     IF XRECORD-NUMBER (1) EQUAL TO 50                            IX1134.2
069000         GO TO WRI-TEST-GF-01-1.                                  IX1134.2
069100     ADD 1 TO XRECORD-NUMBER (1).                                 IX1134.2
069200     GO TO WRI-TEST-GF-01-0.                                      IX1134.2
069300 WRI-TEST-GF-01-1.                                                IX1134.2
069400     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IX1134.2
069500         GO TO WRI-PASS-GF-01-0.                                  IX1134.2
069600     MOVE "ERROR IN CREATING FILE" TO RE-MARK.                    IX1134.2
069700 WRI-FAIL-GF-01-0.                                                IX1134.2
069800     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1134.2
069900     PERFORM FAIL.                                                IX1134.2
070000     MOVE "RECORDS WRITTEN =" TO COMPUTED-A.                      IX1134.2
070100     GO TO WRI-WRITE-GF-01-0.                                     IX1134.2
070200 WRI-PASS-GF-01-0.                                                IX1134.2
070300     PERFORM PASS.                                                IX1134.2
070400 WRI-WRITE-GF-01-0.                                               IX1134.2
070500     PERFORM PRINT-DETAIL.                                        IX1134.2
070600     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   IX1134.2
070700     MOVE "CREATE FILE IX-FS3" TO FEATURE.                        IX1134.2
070800     MOVE "WRI-TEST-GF-01-1" TO PAR-NAME.                         IX1134.2
070900     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          IX1134.2
071000     PERFORM PRINT-DETAIL.                                        IX1134.2
071100                                                                  IX1134.2
071200******************************************************************IX1134.2
071300*   TEST  4                                                      *IX1134.2
071400*         CLOSE OUTPUT                    00 EXPECTED            *IX1134.2
071500*         IX-3, 1.3.4 (1) A                                      *IX1134.2
071600******************************************************************IX1134.2
071700 CLO-INIT-GF-01-0.                                                IX1134.2
071800     MOVE SPACES TO IX-FS3-STATUS.                                IX1134.2
071900     MOVE "CLOSE OUTPUT:00 EXP." TO FEATURE.                      IX1134.2
072000     MOVE "CLO-TEST-GF-01-0" TO PAR-NAME.                         IX1134.2
072100 CLO-TEST-GF-01-0.                                                IX1134.2
072200     CLOSE IX-FS3.                                                IX1134.2
072300     IF IX-FS3-STATUS = "00"                                      IX1134.2
072400         GO TO CLO-PASS-GF-01-0.                                  IX1134.2
072500 CLO-FAIL-GF-01-0.                                                IX1134.2
072600     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1134.2
072700     PERFORM FAIL.                                                IX1134.2
072800     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1134.2
072900     MOVE "00" TO CORRECT-X.                                      IX1134.2
073000     GO TO CLO-WRITE-GF-01-0.                                     IX1134.2
073100 CLO-PASS-GF-01-0.                                                IX1134.2
073200     PERFORM PASS.                                                IX1134.2
073300 CLO-WRITE-GF-01-0.                                               IX1134.2
073400     PERFORM PRINT-DETAIL.                                        IX1134.2
073500                                                                  IX1134.2
073600******************************************************************IX1134.2
073700*    A INDEXED FILE WITH 50 RECORDS HAS BEEN CREATED.            *IX1134.2
073800******************************************************************IX1134.2
073900                                                                  IX1134.2
074000******************************************************************IX1134.2
074100*   TEST  5                                                      *IX1134.2
074200*         CLOSE FOR A FILE NOT IN THE OPEN MODE                  *IX1134.2
074300*         FILE STATUS 42 EXPECTED IX-5, 1.3.4 (5) B              *IX1134.2
074400******************************************************************IX1134.2
074500 CLO-TEST-GF-02-0.                                                IX1134.2
074600     MOVE  5 TO TEST-NO.                                          IX1134.2
074700     MOVE SPACES TO IX-FS3-STATUS.                                IX1134.2
074800     MOVE "CLOSE-INPUT: 42 EXP." TO FEATURE                       IX1134.2
074900     MOVE "CLO-TEST-GF-02-0" TO PAR-NAME.                         IX1134.2
075000     CLOSE IX-FS3.                                                IX1134.2
075100 CLO-TEST-GF-02-1.                                                IX1134.2
075200     IF IX-FS3-STATUS EQUAL TO "42"                               IX1134.2
075300        MOVE "SHOULD HAVE EXECUTED DECLARATIVES IX-3,1.3.4(4)"    IX1134.2
075400          TO RE-MARK                                              IX1134.2
075500        GO TO CLO-WRITE-GF-02-0.                                  IX1134.2
075600 CLO-FAIL-GF-02-0.                                                IX1134.2
075700     MOVE "IX-5, 1.3.4, (5) B" TO RE-MARK.                        IX1134.2
075800 CLO-WRITE-GF-02-0.                                               IX1134.2
075900     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1134.2
076000     MOVE "42" TO CORRECT-X.                                      IX1134.2
076100     PERFORM FAIL.                                                IX1134.2
076200     PERFORM PRINT-DETAIL.                                        IX1134.2
076300                                                                  IX1134.2
076400 TERMINATE-ROUTINE.                                               IX1134.2
076500     EXIT.                                                        IX1134.2
076600                                                                  IX1134.2
076700 CCVS-EXIT SECTION.                                               IX1134.2
076800 CCVS-999999.                                                     IX1134.2
076900     GO TO CLOSE-FILES.                                           IX1134.2
      *END-OF,IX113A                                                                  
