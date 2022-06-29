      *HEADER,COBOL,IX212A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2124.2
000200 PROGRAM-ID.                                                      IX2124.2
000300     IX212A.                                                      IX2124.2
000400****************************************************************  IX2124.2
000500*                                                              *  IX2124.2
000600*    VALIDATION FOR:-                                          *  IX2124.2
000700*                                                              *  IX2124.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2124.2
000900*                                                              *  IX2124.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2124.2
001100*                                                              *  IX2124.2
001200****************************************************************  IX2124.2
001300*                                                              *  IX2124.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IX2124.2
001500*                                                              *  IX2124.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IX2124.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IX2124.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IX2124.2
001900*                                                              *  IX2124.2
002000****************************************************************  IX2124.2
002100*                                                              *  IX2124.2
002200*    THIS IS IX212A.                                              IX2124.2
002300*    THIS  PROGRAM  CREATES  A  100  RECORD FIXED LENGTH INDEXED  IX2124.2
002400*    FILE  WHOSE  ACCESS  IS  DYNAMIC  AND CONTAINS 10 ALTERNATE  IX2124.2
002500*    KEYS. THE INDEXED FILE IS MANIPULATED BY THE ALTERNATE KEYS  IX2124.2
002600*    USING THE FOLLOWING VERBS:                                   IX2124.2
002700*         . DELETE                                                IX2124.2
002800*         . READ ... NEXT RECORD                                  IX2124.2
002900*         . READ ... RECORD KEY                                   IX2124.2
003000*         . REWRITE                                               IX2124.2
003100*         . START                                                 IX2124.2
003200*                                                                 IX2124.2
003300 ENVIRONMENT DIVISION.                                            IX2124.2
003400 CONFIGURATION SECTION.                                           IX2124.2
003500 SOURCE-COMPUTER.                                                 IX2124.2
003600     XXXXX082.                                                    IX2124.2
003700 OBJECT-COMPUTER.                                                 IX2124.2
003800     XXXXX083.                                                    IX2124.2
003900 INPUT-OUTPUT SECTION.                                            IX2124.2
004000 FILE-CONTROL.                                                    IX2124.2
004100*    SELECT RAW-DATA   ASSIGN TO                                  IX2124.2
004200*    XXXXX062                                                     IX2124.2
004300*           ORGANIZATION IS INDEXED                               IX2124.2
004400*           ACCESS MODE IS RANDOM                                 IX2124.2
004500*           RECORD KEY IS RAW-DATA-KEY.                           IX2124.2
004600     SELECT PRINT-FILE ASSIGN TO                                  IX2124.2
004700     XXXXX055.                                                    IX2124.2
004800     SELECT IX-FS1                                                IX2124.2
004900     ASSIGN TO                                                    IX2124.2
005000     XXXXX024                                                     IX2124.2
005100*    XXXXX044                                                     IX2124.2
005200     ACCESS MODE IS DYNAMIC                                       IX2124.2
005300     RECORD KEY IS IX-FS1-KEY                                     IX2124.2
005400     ALTERNATE RECORD KEY IS IX-FS1-ALT01                         IX2124.2
005500     ALTERNATE RECORD KEY IS IX-FS1-ALT02                         IX2124.2
005600     ALTERNATE RECORD KEY IS IX-FS1-ALT03                         IX2124.2
005700     ALTERNATE RECORD KEY IS IX-FS1-ALT04                         IX2124.2
005800     ALTERNATE RECORD KEY IS IX-FS1-ALT05                         IX2124.2
005900     ALTERNATE RECORD KEY IS IX-FS1-ALT06                         IX2124.2
006000     ALTERNATE RECORD KEY IS IX-FS1-ALT07                         IX2124.2
006100     ALTERNATE RECORD KEY IS IX-FS1-ALT08                         IX2124.2
006200     ALTERNATE RECORD KEY IS IX-FS1-ALT09                         IX2124.2
006300     ALTERNATE RECORD KEY IS IX-FS1-ALT10                         IX2124.2
006400     ORGANIZATION IS INDEXED.                                     IX2124.2
006500 DATA DIVISION.                                                   IX2124.2
006600 FILE SECTION.                                                    IX2124.2
006700*                                                                 IX2124.2
006800*FD  RAW-DATA.                                                    IX2124.2
006900*                                                                 IX2124.2
007000*01  RAW-DATA-SATZ.                                               IX2124.2
007100*    05  RAW-DATA-KEY        PIC X(6).                            IX2124.2
007200*    05  C-DATE              PIC 9(6).                            IX2124.2
007300*    05  C-TIME              PIC 9(8).                            IX2124.2
007400*    05  C-NO-OF-TESTS       PIC 99.                              IX2124.2
007500*    05  C-OK                PIC 999.                             IX2124.2
007600*    05  C-ALL               PIC 999.                             IX2124.2
007700*    05  C-FAIL              PIC 999.                             IX2124.2
007800*    05  C-DELETED           PIC 999.                             IX2124.2
007900*    05  C-INSPECT           PIC 999.                             IX2124.2
008000*    05  C-NOTE              PIC X(13).                           IX2124.2
008100*    05  C-INDENT            PIC X.                               IX2124.2
008200*    05  C-ABORT             PIC X(8).                            IX2124.2
008300 FD  PRINT-FILE.                                                  IX2124.2
008400 01  PRINT-REC PICTURE X(120).                                    IX2124.2
008500 01  DUMMY-RECORD PICTURE X(120).                                 IX2124.2
008600 FD  IX-FS1                                                       IX2124.2
008700C    LABEL RECORDS ARE STANDARD                                   IX2124.2
008800C    DATA RECORD IS IX-FS1-RECORD                                 IX2124.2
008900     RECORD CONTAINS 116 CHARACTERS.                              IX2124.2
009000 01  IX-FS1-RECORD.                                               IX2124.2
009100     02 IX-FS1-KEY               PIC X(6).                        IX2124.2
009200     02 IX-FS1-ALT01             PIC X(11).                       IX2124.2
009300     02 IX-FS1-ALT02             PIC X(11).                       IX2124.2
009400     02 IX-FS1-ALT03             PIC X(11).                       IX2124.2
009500     02 IX-FS1-ALT04             PIC X(11).                       IX2124.2
009600     02 IX-FS1-ALT05             PIC X(11).                       IX2124.2
009700     02 IX-FS1-ALT06             PIC X(11).                       IX2124.2
009800     02 IX-FS1-ALT07             PIC X(11).                       IX2124.2
009900     02 IX-FS1-ALT08             PIC X(11).                       IX2124.2
010000     02 IX-FS1-ALT09             PIC X(11).                       IX2124.2
010100     02 IX-FS1-ALT10             PIC X(11).                       IX2124.2
010200 WORKING-STORAGE SECTION.                                         IX2124.2
010300 01  RECORD-COUNTER              PIC 999    VALUE ZEROS.          IX2124.2
010400 01  INVKEY-COUNTER              PIC 999    VALUE ZEROS.          IX2124.2
010500 01  WORK-RECORD.                                                 IX2124.2
010600     02 FILLER                   PIC XXX    VALUE "AAA".          IX2124.2
010700     02 COUNTER00                PIC 999.                         IX2124.2
010800     02 FILLER                   PIC XXX    VALUE "CCC".          IX2124.2
010900     02 COUNTER01                PIC 999.                         IX2124.2
011000     02 FILLER                   PIC X(5)   VALUE "ALT01".        IX2124.2
011100     02 FILLER                   PIC XXX    VALUE "EEE".          IX2124.2
011200     02 COUNTER02                PIC 999.                         IX2124.2
011300     02 FILLER                   PIC X(5)   VALUE "ALT02".        IX2124.2
011400     02 FILLER                   PIC XXX    VALUE "GGG".          IX2124.2
011500     02 COUNTER03                PIC 999.                         IX2124.2
011600     02 FILLER                   PIC X(5)   VALUE "ALT03".        IX2124.2
011700     02 FILLER                   PIC XXX    VALUE "III".          IX2124.2
011800     02 COUNTER04                PIC 999.                         IX2124.2
011900     02 FILLER                   PIC X(5)   VALUE "ALT04".        IX2124.2
012000     02 FILLER                   PIC XXX    VALUE "KKK".          IX2124.2
012100     02 COUNTER05                PIC 999.                         IX2124.2
012200     02 FILLER                   PIC X(5)   VALUE "ALT05".        IX2124.2
012300     02 FILLER                   PIC XXX    VALUE "MMM".          IX2124.2
012400     02 COUNTER06                PIC 999.                         IX2124.2
012500     02 FILLER                   PIC X(5)   VALUE "ALT06".        IX2124.2
012600     02 FILLER                   PIC XXX    VALUE "OOO".          IX2124.2
012700     02 COUNTER07                PIC 999.                         IX2124.2
012800     02 FILLER                   PIC X(5)   VALUE "ALT07".        IX2124.2
012900     02 FILLER                   PIC XXX    VALUE "QQQ".          IX2124.2
013000     02 COUNTER08                PIC 999.                         IX2124.2
013100     02 FILLER                   PIC X(5)   VALUE "ALT08".        IX2124.2
013200     02 FILLER                   PIC XXX    VALUE "SSS".          IX2124.2
013300     02 COUNTER09                PIC 999.                         IX2124.2
013400     02 FILLER                   PIC X(5)   VALUE "ALT09".        IX2124.2
013500     02 FILLER                   PIC XXX    VALUE "UUU".          IX2124.2
013600     02 COUNTER10                PIC 999.                         IX2124.2
013700     02 FILLER                   PIC X(5)   VALUE "ALT10".        IX2124.2
013800 01  TEST-RESULTS.                                                IX2124.2
013900     02 FILLER                   PIC X      VALUE SPACE.          IX2124.2
014000     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2124.2
014100     02 FILLER                   PIC X      VALUE SPACE.          IX2124.2
014200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2124.2
014300     02 FILLER                   PIC X      VALUE SPACE.          IX2124.2
014400     02  PAR-NAME.                                                IX2124.2
014500       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2124.2
014600       03  PARDOT-X              PIC X      VALUE SPACE.          IX2124.2
014700       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2124.2
014800     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2124.2
014900     02 RE-MARK                  PIC X(61).                       IX2124.2
015000 01  TEST-COMPUTED.                                               IX2124.2
015100     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2124.2
015200     02 FILLER                   PIC X(17)  VALUE                 IX2124.2
015300            "       COMPUTED=".                                   IX2124.2
015400     02 COMPUTED-X.                                               IX2124.2
015500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2124.2
015600     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2124.2
015700                                 PIC -9(9).9(9).                  IX2124.2
015800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2124.2
015900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2124.2
016000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2124.2
016100     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2124.2
016200         04 COMPUTED-18V0                    PIC -9(18).          IX2124.2
016300         04 FILLER                           PIC X.               IX2124.2
016400     03 FILLER PIC X(50) VALUE SPACE.                             IX2124.2
016500 01  TEST-CORRECT.                                                IX2124.2
016600     02 FILLER PIC X(30) VALUE SPACE.                             IX2124.2
016700     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2124.2
016800     02 CORRECT-X.                                                IX2124.2
016900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2124.2
017000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2124.2
017100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2124.2
017200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2124.2
017300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2124.2
017400     03      CR-18V0 REDEFINES CORRECT-A.                         IX2124.2
017500         04 CORRECT-18V0                     PIC -9(18).          IX2124.2
017600         04 FILLER                           PIC X.               IX2124.2
017700     03 FILLER PIC X(2) VALUE SPACE.                              IX2124.2
017800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2124.2
017900 01  CCVS-C-1.                                                    IX2124.2
018000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2124.2
018100-    "SS  PARAGRAPH-NAME                                          IX2124.2
018200-    "       REMARKS".                                            IX2124.2
018300     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2124.2
018400 01  CCVS-C-2.                                                    IX2124.2
018500     02 FILLER                     PIC X        VALUE SPACE.      IX2124.2
018600     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2124.2
018700     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2124.2
018800     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2124.2
018900     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2124.2
019000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2124.2
019100 01  REC-CT                        PIC 99       VALUE ZERO.       IX2124.2
019200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2124.2
019300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2124.2
019400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2124.2
019500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2124.2
019600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2124.2
019700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2124.2
019800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2124.2
019900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2124.2
020000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2124.2
020100 01  CCVS-H-1.                                                    IX2124.2
020200     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2124.2
020300     02  FILLER                    PIC X(42)    VALUE             IX2124.2
020400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2124.2
020500     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2124.2
020600 01  CCVS-H-2A.                                                   IX2124.2
020700   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2124.2
020800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2124.2
020900   02  FILLER                        PIC XXXX   VALUE             IX2124.2
021000     "4.2 ".                                                      IX2124.2
021100   02  FILLER                        PIC X(28)  VALUE             IX2124.2
021200            " COPY - NOT FOR DISTRIBUTION".                       IX2124.2
021300   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2124.2
021400                                                                  IX2124.2
021500 01  CCVS-H-2B.                                                   IX2124.2
021600   02  FILLER                        PIC X(15)  VALUE             IX2124.2
021700            "TEST RESULT OF ".                                    IX2124.2
021800   02  TEST-ID                       PIC X(9).                    IX2124.2
021900   02  FILLER                        PIC X(4)   VALUE             IX2124.2
022000            " IN ".                                               IX2124.2
022100   02  FILLER                        PIC X(12)  VALUE             IX2124.2
022200     " HIGH       ".                                              IX2124.2
022300   02  FILLER                        PIC X(22)  VALUE             IX2124.2
022400            " LEVEL VALIDATION FOR ".                             IX2124.2
022500   02  FILLER                        PIC X(58)  VALUE             IX2124.2
022600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2124.2
022700 01  CCVS-H-3.                                                    IX2124.2
022800     02  FILLER                      PIC X(34)  VALUE             IX2124.2
022900            " FOR OFFICIAL USE ONLY    ".                         IX2124.2
023000     02  FILLER                      PIC X(58)  VALUE             IX2124.2
023100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2124.2
023200     02  FILLER                      PIC X(28)  VALUE             IX2124.2
023300            "  COPYRIGHT   1985 ".                                IX2124.2
023400 01  CCVS-E-1.                                                    IX2124.2
023500     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2124.2
023600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2124.2
023700     02 ID-AGAIN                     PIC X(9).                    IX2124.2
023800     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2124.2
023900 01  CCVS-E-2.                                                    IX2124.2
024000     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2124.2
024100     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2124.2
024200     02 CCVS-E-2-2.                                               IX2124.2
024300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2124.2
024400         03 FILLER                   PIC X      VALUE SPACE.      IX2124.2
024500         03 ENDER-DESC               PIC X(44)  VALUE             IX2124.2
024600            "ERRORS ENCOUNTERED".                                 IX2124.2
024700 01  CCVS-E-3.                                                    IX2124.2
024800     02  FILLER                      PIC X(22)  VALUE             IX2124.2
024900            " FOR OFFICIAL USE ONLY".                             IX2124.2
025000     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2124.2
025100     02  FILLER                      PIC X(58)  VALUE             IX2124.2
025200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2124.2
025300     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2124.2
025400     02 FILLER                       PIC X(15)  VALUE             IX2124.2
025500             " COPYRIGHT 1985".                                   IX2124.2
025600 01  CCVS-E-4.                                                    IX2124.2
025700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2124.2
025800     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2124.2
025900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2124.2
026000     02 FILLER                       PIC X(40)  VALUE             IX2124.2
026100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2124.2
026200 01  XXINFO.                                                      IX2124.2
026300     02 FILLER                       PIC X(19)  VALUE             IX2124.2
026400            "*** INFORMATION ***".                                IX2124.2
026500     02 INFO-TEXT.                                                IX2124.2
026600       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2124.2
026700       04 XXCOMPUTED                 PIC X(20).                   IX2124.2
026800       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2124.2
026900       04 XXCORRECT                  PIC X(20).                   IX2124.2
027000     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2124.2
027100 01  HYPHEN-LINE.                                                 IX2124.2
027200     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2124.2
027300     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2124.2
027400-    "*****************************************".                 IX2124.2
027500     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2124.2
027600-    "******************************".                            IX2124.2
027700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2124.2
027800     "IX212A".                                                    IX2124.2
027900 PROCEDURE DIVISION.                                              IX2124.2
028000 CCVS1 SECTION.                                                   IX2124.2
028100 OPEN-FILES.                                                      IX2124.2
028200*    OPEN I-O RAW-DATA.                                           IX2124.2
028300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2124.2
028400*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2124.2
028500*    MOVE "ABORTED " TO C-ABORT.                                  IX2124.2
028600*    ADD 1 TO C-NO-OF-TESTS.                                      IX2124.2
028700*    ACCEPT C-DATE  FROM DATE.                                    IX2124.2
028800*    ACCEPT C-TIME  FROM TIME.                                    IX2124.2
028900*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2124.2
029000*END-E-1.                                                         IX2124.2
029100*    CLOSE RAW-DATA.                                              IX2124.2
029200     OPEN    OUTPUT PRINT-FILE.                                   IX2124.2
029300     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2124.2
029400     MOVE    SPACE TO TEST-RESULTS.                               IX2124.2
029500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2124.2
029600     GO TO CCVS1-EXIT.                                            IX2124.2
029700 CLOSE-FILES.                                                     IX2124.2
029800*    OPEN I-O RAW-DATA.                                           IX2124.2
029900*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2124.2
030000*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2124.2
030100*    MOVE "OK.     " TO C-ABORT.                                  IX2124.2
030200*    MOVE PASS-COUNTER TO C-OK.                                   IX2124.2
030300*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2124.2
030400*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2124.2
030500*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2124.2
030600*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2124.2
030700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2124.2
030800*END-E-2.                                                         IX2124.2
030900*    CLOSE RAW-DATA.                                              IX2124.2
031000     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2124.2
031100 TERMINATE-CCVS.                                                  IX2124.2
031200*    EXIT PROGRAM.                                                IX2124.2
031300*TERMINATE-CALL.                                                  IX2124.2
031400     STOP     RUN.                                                IX2124.2
031500 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2124.2
031600 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2124.2
031700 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2124.2
031800 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2124.2
031900     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2124.2
032000 PRINT-DETAIL.                                                    IX2124.2
032100     IF REC-CT NOT EQUAL TO ZERO                                  IX2124.2
032200             MOVE "." TO PARDOT-X                                 IX2124.2
032300             MOVE REC-CT TO DOTVALUE.                             IX2124.2
032400     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2124.2
032500     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2124.2
032600        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2124.2
032700          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2124.2
032800     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2124.2
032900     MOVE SPACE TO CORRECT-X.                                     IX2124.2
033000     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2124.2
033100     MOVE     SPACE TO RE-MARK.                                   IX2124.2
033200 HEAD-ROUTINE.                                                    IX2124.2
033300     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2124.2
033400     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2124.2
033500     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2124.2
033600     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2124.2
033700 COLUMN-NAMES-ROUTINE.                                            IX2124.2
033800     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2124.2
033900     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2124.2
034000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2124.2
034100 END-ROUTINE.                                                     IX2124.2
034200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2124.2
034300 END-RTN-EXIT.                                                    IX2124.2
034400     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2124.2
034500 END-ROUTINE-1.                                                   IX2124.2
034600      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2124.2
034700      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2124.2
034800      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2124.2
034900*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2124.2
035000      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2124.2
035100      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2124.2
035200      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2124.2
035300      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2124.2
035400  END-ROUTINE-12.                                                 IX2124.2
035500      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2124.2
035600     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2124.2
035700         MOVE "NO " TO ERROR-TOTAL                                IX2124.2
035800         ELSE                                                     IX2124.2
035900         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2124.2
036000     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2124.2
036100     PERFORM WRITE-LINE.                                          IX2124.2
036200 END-ROUTINE-13.                                                  IX2124.2
036300     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2124.2
036400         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2124.2
036500         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2124.2
036600     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2124.2
036700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2124.2
036800      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2124.2
036900          MOVE "NO " TO ERROR-TOTAL                               IX2124.2
037000      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2124.2
037100      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2124.2
037200      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2124.2
037300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2124.2
037400 WRITE-LINE.                                                      IX2124.2
037500     ADD 1 TO RECORD-COUNT.                                       IX2124.2
037600*    IF RECORD-COUNT GREATER 42                                   IX2124.2
037700*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2124.2
037800*        MOVE SPACE TO DUMMY-RECORD                               IX2124.2
037900*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2124.2
038000*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2124.2
038100*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2124.2
038200*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2124.2
038300*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2124.2
038400*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2124.2
038500*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2124.2
038600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2124.2
038700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2124.2
038800*        MOVE ZERO TO RECORD-COUNT.                               IX2124.2
038900     PERFORM WRT-LN.                                              IX2124.2
039000 WRT-LN.                                                          IX2124.2
039100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2124.2
039200     MOVE SPACE TO DUMMY-RECORD.                                  IX2124.2
039300 BLANK-LINE-PRINT.                                                IX2124.2
039400     PERFORM WRT-LN.                                              IX2124.2
039500 FAIL-ROUTINE.                                                    IX2124.2
039600     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2124.2
039700            GO TO   FAIL-ROUTINE-WRITE.                           IX2124.2
039800     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2124.2
039900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2124.2
040000     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2124.2
040100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2124.2
040200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2124.2
040300     GO TO  FAIL-ROUTINE-EX.                                      IX2124.2
040400 FAIL-ROUTINE-WRITE.                                              IX2124.2
040500     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2124.2
040600     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2124.2
040700     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2124.2
040800     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2124.2
040900 FAIL-ROUTINE-EX. EXIT.                                           IX2124.2
041000 BAIL-OUT.                                                        IX2124.2
041100     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2124.2
041200     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2124.2
041300 BAIL-OUT-WRITE.                                                  IX2124.2
041400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2124.2
041500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2124.2
041600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2124.2
041700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2124.2
041800 BAIL-OUT-EX. EXIT.                                               IX2124.2
041900 CCVS1-EXIT.                                                      IX2124.2
042000     EXIT.                                                        IX2124.2
042100 WRITE-INIT-GF-01.                                                IX2124.2
042200     OPEN OUTPUT IX-FS1.                                          IX2124.2
042300     PERFORM CREATE-IX-FS1 VARYING RECORD-COUNTER FROM 1 BY 1     IX2124.2
042400        UNTIL RECORD-COUNTER IS GREATER THAN 100.                 IX2124.2
042500     GO TO WRITE-TEST-GF-01.                                      IX2124.2
042600 CREATE-IX-FS1.                                                   IX2124.2
042700     MOVE RECORD-COUNTER TO COUNTER00, COUNTER01, COUNTER02,      IX2124.2
042800        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2124.2
042900        COUNTER08, COUNTER09, COUNTER10.                          IX2124.2
043000     WRITE IX-FS1-RECORD FROM WORK-RECORD INVALID KEY             IX2124.2
043100        ADD 1 TO INVKEY-COUNTER.                                  IX2124.2
043200 WRITE-TEST-GF-01.                                                IX2124.2
043300     MOVE "WRITE INVALID KEY" TO FEATURE.                         IX2124.2
043400     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         IX2124.2
043500     MOVE "CREATE IX-FS1" TO RE-MARK.                             IX2124.2
043600     IF INVKEY-COUNTER = 0                                        IX2124.2
043700        PERFORM PASS                                              IX2124.2
043800        ELSE GO TO WRITE-FAIL-GF-01.                              IX2124.2
043900     GO TO WRITE-WRITE-GF-01.                                     IX2124.2
044000 WRITE-DELETE-GF-01.                                              IX2124.2
044100     PERFORM DE-LETE.                                             IX2124.2
044200     GO TO WRITE-WRITE-GF-01.                                     IX2124.2
044300 WRITE-FAIL-GF-01.                                                IX2124.2
044400     MOVE "IX-41; ONE WRITE FAILED AT LEAST" TO RE-MARK.          IX2124.2
044500     PERFORM FAIL.                                                IX2124.2
044600     MOVE INVKEY-COUNTER TO COMPUTED-18V0.                        IX2124.2
044700     MOVE 0 TO CORRECT-18V0.                                      IX2124.2
044800 WRITE-WRITE-GF-01.                                               IX2124.2
044900     PERFORM PRINT-DETAIL.                                        IX2124.2
045000 READ-TEST-F2-01.                                                 IX2124.2
045100     CLOSE IX-FS1.                                                IX2124.2
045200     OPEN I-O IX-FS1.                                             IX2124.2
045300     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
045400     MOVE "READ-TEST-F2-01" TO PAR-NAME.                          IX2124.2
045500     MOVE "RETRIEVED BY ALTERNATE KEY 1" TO RE-MARK.              IX2124.2
045600     MOVE "CCC012ALT01" TO IX-FS1-ALT01.                          IX2124.2
045700     READ IX-FS1 RECORD                                           IX2124.2
045800                      KEY IS IX-FS1-ALT01                         IX2124.2
045900      INVALID KEY   GO TO READ-INVALID-F2-01.                     IX2124.2
046000     IF IX-FS1-KEY = "AAA012"                                     IX2124.2
046100        PERFORM PASS                                              IX2124.2
046200        ELSE GO TO READ-FAIL-F2-01.                               IX2124.2
046300     GO TO READ-WRITE-F2-01.                                      IX2124.2
046400 READ-DELETE-F2-01.                                               IX2124.2
046500     PERFORM DE-LETE.                                             IX2124.2
046600     GO TO READ-WRITE-F2-01.                                      IX2124.2
046700 READ-INVALID-F2-01.                                              IX2124.2
046800     PERFORM FAIL.                                                IX2124.2
046900     MOVE "IX-28; INVALID KEY CONDITION (IX-6) EXISTS" TO RE-MARK.IX2124.2
047000     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
047100     MOVE "CCC012ALT01" TO CORRECT-A.                             IX2124.2
047200     GO TO READ-WRITE-F2-01.                                      IX2124.2
047300 READ-FAIL-F2-01.                                                 IX2124.2
047400     MOVE "IX-28; RETRIEVED BY ALTERENATE KEY 1" TO RE-MARK.      IX2124.2
047500     PERFORM FAIL.                                                IX2124.2
047600     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
047700     MOVE "AAA012" TO CORRECT-A.                                  IX2124.2
047800 READ-WRITE-F2-01.                                                IX2124.2
047900     PERFORM PRINT-DETAIL.                                        IX2124.2
048000 READ-TEST-F2-02.                                                 IX2124.2
048100     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
048200     MOVE "READ-TEST-F2-02" TO PAR-NAME.                          IX2124.2
048300     MOVE "RETRIEVED BY ALTERNATE KEY 8" TO RE-MARK.              IX2124.2
048400     MOVE "QQQ043ALT08" TO IX-FS1-ALT08.                          IX2124.2
048500     READ IX-FS1 RECORD                                           IX2124.2
048600                      KEY IS IX-FS1-ALT08                         IX2124.2
048700       INVALID KEY  GO TO READ-INVALID-F2-02.                     IX2124.2
048800     IF IX-FS1-KEY = "AAA043"                                     IX2124.2
048900        PERFORM PASS                                              IX2124.2
049000        ELSE GO TO READ-FAIL-F2-02.                               IX2124.2
049100     GO TO READ-WRITE-F2-02.                                      IX2124.2
049200 READ-DELETE-F2-02.                                               IX2124.2
049300     PERFORM DE-LETE.                                             IX2124.2
049400     GO TO READ-WRITE-F2-02.                                      IX2124.2
049500 READ-INVALID-F2-02.                                              IX2124.2
049600     PERFORM FAIL.                                                IX2124.2
049700     MOVE "IX-28; 4.5.2 F2, INVALID KEY CONDITION (IX-6) "        IX2124.2
049800          TO RE-MARK.                                             IX2124.2
049900     MOVE "QQQ043ALT08" TO CORRECT-A.                             IX2124.2
050000     GO TO READ-WRITE-F2-02.                                      IX2124.2
050100 READ-FAIL-F2-02.                                                 IX2124.2
050200     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2124.2
050300     PERFORM FAIL.                                                IX2124.2
050400     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
050500     MOVE "AAA043" TO CORRECT-A.                                  IX2124.2
050600 READ-WRITE-F2-02.                                                IX2124.2
050700     PERFORM PRINT-DETAIL.                                        IX2124.2
050800 DELETE-TEST-GF-01.                                               IX2124.2
050900     MOVE "DELETE...RECORD" TO FEATURE.                           IX2124.2
051000     MOVE "DELETE-TEST-GF-01" TO PAR-NAME.                        IX2124.2
051100     MOVE "DELETES RECORD FOUND IN READ-TEST-F2-02" TO RE-MARK.   IX2124.2
051200     DELETE IX-FS1 RECORD INVALID KEY GO TO DELETE-INVALID-GF-01. IX2124.2
051300     PERFORM PASS.                                                IX2124.2
051400     GO TO DELETE-WRITE-GF-01.                                    IX2124.2
051500 DELETE-DELETE-GF-01.                                             IX2124.2
051600     PERFORM DE-LETE.                                             IX2124.2
051700     GO TO DELETE-WRITE-GF-01.                                    IX2124.2
051800 DELETE-INVALID-GF-01.                                            IX2124.2
051900     MOVE "IX-21; 4.3.2 F2, INVALID KEY CONDITION (IX-6) "        IX2124.2
052000         TO RE-MARK.                                              IX2124.2
052100     PERFORM FAIL.                                                IX2124.2
052200     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
052300     MOVE "AAA043" TO CORRECT-A.                                  IX2124.2
052400 DELETE-WRITE-GF-01.                                              IX2124.2
052500     PERFORM PRINT-DETAIL.                                        IX2124.2
052600 READ-TEST-F2-03.                                                 IX2124.2
052700     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
052800     MOVE "READ-TEST-F2-03" TO PAR-NAME.                          IX2124.2
052900     MOVE "RETRIEVED BY ALTERNATE KEY 4" TO RE-MARK.              IX2124.2
053000     MOVE "III017ALT04" TO IX-FS1-ALT04.                          IX2124.2
053100     READ IX-FS1 RECORD                                           IX2124.2
053200                      KEY IS IX-FS1-ALT04                         IX2124.2
053300      INVALID KEY   GO TO READ-INVALID-F2-03.                     IX2124.2
053400     IF IX-FS1-KEY = "AAA017"                                     IX2124.2
053500        PERFORM PASS                                              IX2124.2
053600        ELSE GO TO READ-FAIL-F2-03.                               IX2124.2
053700     GO TO READ-WRITE-F2-03.                                      IX2124.2
053800 READ-DELETE-F2-03.                                               IX2124.2
053900     PERFORM DE-LETE.                                             IX2124.2
054000     GO TO READ-WRITE-F2-03.                                      IX2124.2
054100 READ-INVALID-F2-03.                                              IX2124.2
054200     MOVE "IX-28; 4.5.2 F2, INVALID KEY CONDITION (IX-6) "        IX2124.2
054300         TO RE-MARK.                                              IX2124.2
054400     PERFORM FAIL.                                                IX2124.2
054500     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
054600     MOVE "III017ALT04" TO CORRECT-A.                             IX2124.2
054700     GO TO READ-WRITE-F2-03.                                      IX2124.2
054800 READ-FAIL-F2-03.                                                 IX2124.2
054900     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2124.2
055000     PERFORM FAIL.                                                IX2124.2
055100     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
055200     MOVE "AAA017" TO CORRECT-A.                                  IX2124.2
055300 READ-WRITE-F2-03.                                                IX2124.2
055400     PERFORM PRINT-DETAIL.                                        IX2124.2
055500 REWRITE-TEST-GF-01.                                              IX2124.2
055600     MOVE "REWRITE...INVALID..." TO FEATURE.                      IX2124.2
055700     MOVE "REWRITE-TEST-GF-01" TO PAR-NAME.                       IX2124.2
055800     MOVE "REWRITES RECORD FOUND IN READ-TEST-F2-03" TO RE-MARK.  IX2124.2
055900     MOVE "III917ALT04" TO IX-FS1-ALT04.                          IX2124.2
056000     REWRITE IX-FS1-RECORD INVALID KEY                            IX2124.2
056100                  GO TO REWRITE-INVALID-GF-01.                    IX2124.2
056200     PERFORM PASS.                                                IX2124.2
056300     GO TO REWRITE-WRITE-GF-01.                                   IX2124.2
056400 REWRITE-DELETE-GF-01.                                            IX2124.2
056500     PERFORM DE-LETE.                                             IX2124.2
056600     GO TO REWRITE-WRITE-GF-01.                                   IX2124.2
056700 REWRITE-INVALID-GF-01.                                           IX2124.2
056800     MOVE "IX-33; 4.6.2, INVALID KEY CONDITION (IX-6) " TO RE-MARKIX2124.2
056900     PERFORM FAIL.                                                IX2124.2
057000     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
057100     MOVE "III917ALT04" TO CORRECT-A.                             IX2124.2
057200 REWRITE-WRITE-GF-01.                                             IX2124.2
057300     PERFORM PRINT-DETAIL.                                        IX2124.2
057400 READ-TEST-F2-04.                                                 IX2124.2
057500     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
057600     MOVE "READ-TEST-F2-04" TO PAR-NAME.                          IX2124.2
057700     MOVE "INVALID KEY PATH SHOULD BE TAKEN" TO RE-MARK.          IX2124.2
057800     MOVE "ZZZ999ALT09" TO IX-FS1-ALT09.                          IX2124.2
057900     READ IX-FS1 RECORD                                           IX2124.2
058000                      KEY IS IX-FS1-ALT09                         IX2124.2
058100      INVALID KEY   PERFORM PASS                                  IX2124.2
058200        GO TO READ-WRITE-F2-04.                                   IX2124.2
058300     GO TO READ-FAIL-F2-04.                                       IX2124.2
058400 READ-DELETE-F2-04.                                               IX2124.2
058500     PERFORM DE-LETE.                                             IX2124.2
058600     GO TO READ-WRITE-F2-04.                                      IX2124.2
058700 READ-FAIL-F2-04.                                                 IX2124.2
058800     MOVE "IX-28; 4.5.2 F2, INVALID KEY NOT TAKEN (IX-6)"         IX2124.2
058900          TO RE-MARK.                                             IX2124.2
059000     PERFORM FAIL.                                                IX2124.2
059100     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
059200     MOVE "INVALID KEY" TO CORRECT-A.                             IX2124.2
059300 READ-WRITE-F2-04.                                                IX2124.2
059400     PERFORM PRINT-DETAIL.                                        IX2124.2
059500 START-TEST-GF-01.                                                IX2124.2
059600     MOVE "START...KEY IS EQUAL"    TO FEATURE.                   IX2124.2
059700     MOVE "START-TEST-GF-01" TO PAR-NAME.                         IX2124.2
059800     MOVE "RETRIEVED BY ALTERNATE KEY 3" TO RE-MARK.              IX2124.2
059900     MOVE "GGG058ALT03" TO IX-FS1-ALT03.                          IX2124.2
060000     START IX-FS1                                                 IX2124.2
060100                KEY IS EQUAL TO IX-FS1-ALT03                      IX2124.2
060200        INVALID KEY  GO TO START-INVALID-GF-01.                   IX2124.2
060300     READ IX-FS1 NEXT RECORD AT END GO TO START-END-GF-01.        IX2124.2
060400     IF IX-FS1-KEY = "AAA058"                                     IX2124.2
060500        PERFORM PASS                                              IX2124.2
060600        ELSE GO TO START-FAIL-GF-01.                              IX2124.2
060700     GO TO START-WRITE-GF-01.                                     IX2124.2
060800 START-DELETE-GF-01.                                              IX2124.2
060900     PERFORM DE-LETE.                                             IX2124.2
061000     GO TO START-WRITE-GF-01.                                     IX2124.2
061100 START-INVALID-GF-01.                                             IX2124.2
061200     MOVE "IX-36; 4.7.2, INVALID KEY CONDITION (IX-6)" TO RE-MARK.IX2124.2
061300     PERFORM FAIL.                                                IX2124.2
061400     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
061500     MOVE "GGG058ALT03" TO CORRECT-A.                             IX2124.2
061600     GO TO START-WRITE-GF-01.                                     IX2124.2
061700 START-END-GF-01.                                                 IX2124.2
061800     MOVE "IX-28; 4.5.2 F1, AT END CONDITION     " TO RE-MARK.    IX2124.2
061900     PERFORM FAIL.                                                IX2124.2
062000     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2124.2
062100     MOVE "AAA058" TO CORRECT-A.                                  IX2124.2
062200     GO TO START-WRITE-GF-01.                                     IX2124.2
062300 START-FAIL-GF-01.                                                IX2124.2
062400     MOVE "IX-28; 4.5.2 F1, WRONG KEY OR IX-36 WRONG START  "     IX2124.2
062500           TO RE-MARK.                                            IX2124.2
062600     PERFORM FAIL.                                                IX2124.2
062700     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
062800     MOVE "AAA058" TO CORRECT-A.                                  IX2124.2
062900 START-WRITE-GF-01.                                               IX2124.2
063000     PERFORM PRINT-DETAIL.                                        IX2124.2
063100 START-TEST-GF-02.                                                IX2124.2
063200     MOVE "START...KEY >" TO FEATURE.                             IX2124.2
063300     MOVE "START-TEST-GF-02" TO PAR-NAME.                         IX2124.2
063400     MOVE "RETRIEVED BY ALTERNATE KEY 6" TO RE-MARK.              IX2124.2
063500     MOVE "MMM089ALT06" TO IX-FS1-ALT06.                          IX2124.2
063600     START IX-FS1                                                 IX2124.2
063700                KEY > IX-FS1-ALT06                                IX2124.2
063800        INVALID KEY GO TO START-INVALID-GF-02.                    IX2124.2
063900     READ IX-FS1 NEXT RECORD AT END GO TO START-END-GF-02.        IX2124.2
064000     IF IX-FS1-KEY = "AAA090"                                     IX2124.2
064100        PERFORM PASS                                              IX2124.2
064200        ELSE GO TO START-FAIL-GF-02.                              IX2124.2
064300     GO TO START-WRITE-GF-02.                                     IX2124.2
064400 START-DELETE-GF-02.                                              IX2124.2
064500     PERFORM DE-LETE.                                             IX2124.2
064600     GO TO START-WRITE-GF-02.                                     IX2124.2
064700 START-INVALID-GF-02.                                             IX2124.2
064800     MOVE "IX-36; 4.7.2, INVALID KEY CONDITION (IX-6)" TO RE-MARK.IX2124.2
064900     PERFORM FAIL.                                                IX2124.2
065000     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
065100     MOVE "MMM089ALT09" TO CORRECT-A.                             IX2124.2
065200     GO TO START-WRITE-GF-02.                                     IX2124.2
065300 START-END-GF-02.                                                 IX2124.2
065400     MOVE "IX-28; 4.5.2 F1, AT END CONDITION (IX-6)" TO RE-MARK.  IX2124.2
065500     PERFORM FAIL.                                                IX2124.2
065600     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2124.2
065700     MOVE "AAA090" TO CORRECT-A.                                  IX2124.2
065800     GO TO START-WRITE-GF-02.                                     IX2124.2
065900 START-FAIL-GF-02.                                                IX2124.2
066000     MOVE "IX-28; 4.5.2 F1, AT END OR IX-36 START WRONG KEY"      IX2124.2
066100           TO RE-MARK.                                            IX2124.2
066200     PERFORM FAIL.                                                IX2124.2
066300     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
066400     MOVE "AAA090" TO CORRECT-A.                                  IX2124.2
066500 START-WRITE-GF-02.                                               IX2124.2
066600     PERFORM PRINT-DETAIL.                                        IX2124.2
066700 START-TEST-GF-03.                                                IX2124.2
066800     MOVE "START...KEY NOT <" TO FEATURE.                         IX2124.2
066900     MOVE "START-TEST-GF-03" TO PAR-NAME.                         IX2124.2
067000     MOVE "RETRIEVED BY ALTERNATE KEY 10" TO RE-MARK.             IX2124.2
067100     MOVE "UUU002ALT10" TO IX-FS1-ALT10.                          IX2124.2
067200     START IX-FS1                                                 IX2124.2
067300                KEY NOT < IX-FS1-ALT10                            IX2124.2
067400      INVALID KEY   GO TO START-INVALID-GF-03.                    IX2124.2
067500     READ IX-FS1 NEXT RECORD                                      IX2124.2
067600      AT END GO TO START-END-GF-03.                               IX2124.2
067700     IF IX-FS1-KEY = "AAA002"                                     IX2124.2
067800        PERFORM PASS                                              IX2124.2
067900        ELSE GO TO START-FAIL-GF-03.                              IX2124.2
068000     GO TO START-WRITE-GF-03.                                     IX2124.2
068100 START-DELETE-GF-03.                                              IX2124.2
068200     PERFORM DE-LETE.                                             IX2124.2
068300     GO TO START-WRITE-GF-03.                                     IX2124.2
068400 START-INVALID-GF-03.                                             IX2124.2
068500     MOVE "IX-36; 4.7.2 F2, INVALID KEY CONDITION (IX-6)"         IX2124.2
068600          TO RE-MARK.                                             IX2124.2
068700     PERFORM FAIL.                                                IX2124.2
068800     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
068900     MOVE "UUU002ALT10" TO CORRECT-A.                             IX2124.2
069000     GO TO START-WRITE-GF-03.                                     IX2124.2
069100 START-END-GF-03.                                                 IX2124.2
069200     MOVE "IX-28; 4.5.2 F1, AT END CONDITION (IX-6)" TO RE-MARK.  IX2124.2
069300     PERFORM FAIL.                                                IX2124.2
069400     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2124.2
069500     MOVE "AAA002" TO CORRECT-A.                                  IX2124.2
069600     GO TO START-WRITE-GF-03.                                     IX2124.2
069700 START-FAIL-GF-03.                                                IX2124.2
069800     MOVE "IX-28; 4.5.2 F1, AT END OR IX-36 START WRONG KEY"      IX2124.2
069900           TO RE-MARK.                                            IX2124.2
070000     PERFORM FAIL.                                                IX2124.2
070100     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
070200     MOVE "AAA002" TO CORRECT-A.                                  IX2124.2
070300 START-WRITE-GF-03.                                               IX2124.2
070400     PERFORM PRINT-DETAIL.                                        IX2124.2
070500 START-TEST-GF-04.                                                IX2124.2
070600     MOVE "START...KEY >=   " TO FEATURE.                         IX2124.2
070700     MOVE "START-TEST-GF-04" TO PAR-NAME.                         IX2124.2
070800     MOVE "RETRIEVED BY ALTERNATE KEY 10" TO RE-MARK.             IX2124.2
070900     MOVE "UUU002ALT10" TO IX-FS1-ALT10.                          IX2124.2
071000     START IX-FS1                                                 IX2124.2
071100                KEY >=    IX-FS1-ALT10                            IX2124.2
071200      INVALID KEY   GO TO START-INVALID-GF-04.                    IX2124.2
071300     READ IX-FS1 NEXT RECORD                                      IX2124.2
071400      AT END GO TO START-END-GF-04.                               IX2124.2
071500     IF IX-FS1-KEY = "AAA002"                                     IX2124.2
071600        PERFORM PASS                                              IX2124.2
071700        ELSE GO TO START-FAIL-GF-04.                              IX2124.2
071800     GO TO START-WRITE-GF-04.                                     IX2124.2
071900 START-DELETE-GF-04.                                              IX2124.2
072000     PERFORM DE-LETE.                                             IX2124.2
072100     GO TO START-WRITE-GF-04.                                     IX2124.2
072200 START-INVALID-GF-04.                                             IX2124.2
072300     MOVE "IX-36; 4.7.2 F2, INVALID KEY CONDITION (IX-6)"         IX2124.2
072400          TO RE-MARK.                                             IX2124.2
072500     PERFORM FAIL.                                                IX2124.2
072600     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
072700     MOVE "UUU002ALT10" TO CORRECT-A.                             IX2124.2
072800     GO TO START-WRITE-GF-04.                                     IX2124.2
072900 START-END-GF-04.                                                 IX2124.2
073000     MOVE "IX-28; 4.5.2 F1, AT END CONDITION (IX-6)" TO RE-MARK.  IX2124.2
073100     PERFORM FAIL.                                                IX2124.2
073200     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2124.2
073300     MOVE "AAA002" TO CORRECT-A.                                  IX2124.2
073400     GO TO START-WRITE-GF-04.                                     IX2124.2
073500 START-FAIL-GF-04.                                                IX2124.2
073600     MOVE "IX-28; 4.5.2 F1, AT END OR IX-36 START WRONG KEY"      IX2124.2
073700           TO RE-MARK.                                            IX2124.2
073800     PERFORM FAIL.                                                IX2124.2
073900     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
074000     MOVE "AAA002" TO CORRECT-A.                                  IX2124.2
074100 START-WRITE-GF-04.                                               IX2124.2
074200     PERFORM PRINT-DETAIL.                                        IX2124.2
074300 READ-TEST-F2-05.                                                 IX2124.2
074400     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
074500     MOVE "READ-TEST-F2-05"  TO PAR-NAME.                         IX2124.2
074600     MOVE "INVALID KEY PATH SHOULD BE TAKEN" TO RE-MARK.          IX2124.2
074700     MOVE "QQQ043ALT08" TO IX-FS1-ALT08.                          IX2124.2
074800     READ IX-FS1 RECORD                                           IX2124.2
074900                      KEY IS IX-FS1-ALT08                         IX2124.2
075000       INVALID KEY  PERFORM PASS                                  IX2124.2
075100        GO TO READ-WRITE-F2-05.                                   IX2124.2
075200     GO TO READ-FAIL-F2-05.                                       IX2124.2
075300 READ-DELETE-F2-05.                                               IX2124.2
075400     PERFORM DE-LETE.                                             IX2124.2
075500     GO TO READ-WRITE-F2-05.                                      IX2124.2
075600 READ-FAIL-F2-05.                                                 IX2124.2
075700     MOVE "IX-28; 4.5.2 F2, INVALID KEY PATH NOT TAKEN (IX-6) "   IX2124.2
075800             TO RE-MARK.                                          IX2124.2
075900     PERFORM FAIL.                                                IX2124.2
076000     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
076100     MOVE "INVALID KEY" TO CORRECT-A.                             IX2124.2
076200 READ-WRITE-F2-05.                                                IX2124.2
076300     PERFORM PRINT-DETAIL.                                        IX2124.2
076400 READ-TEST-F2-06.                                                 IX2124.2
076500     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
076600     MOVE "READ-TEST-F2-06" TO PAR-NAME.                          IX2124.2
076700     MOVE "RETRIEVED BY ALTERNATE KEY 4" TO RE-MARK.              IX2124.2
076800     MOVE "III917ALT04" TO IX-FS1-ALT04.                          IX2124.2
076900     READ IX-FS1 RECORD                                           IX2124.2
077000                      KEY IS IX-FS1-ALT04                         IX2124.2
077100      INVALID KEY   GO TO READ-INVALID-F2-06.                     IX2124.2
077200     IF IX-FS1-KEY = "AAA017"                                     IX2124.2
077300        PERFORM PASS                                              IX2124.2
077400        ELSE GO TO READ-FAIL-F2-06.                               IX2124.2
077500     GO TO READ-WRITE-F2-06.                                      IX2124.2
077600 READ-DELETE-F2-06.                                               IX2124.2
077700     PERFORM DE-LETE.                                             IX2124.2
077800     GO TO READ-WRITE-F2-06.                                      IX2124.2
077900 READ-INVALID-F2-06.                                              IX2124.2
078000     MOVE "IX-28; 4.5.2 F2, INVALID KEY CONDITION (IX-6) "        IX2124.2
078100           TO RE-MARK.                                            IX2124.2
078200     PERFORM FAIL.                                                IX2124.2
078300     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
078400     MOVE "III917ALT04" TO CORRECT-A.                             IX2124.2
078500     GO TO READ-WRITE-F2-06.                                      IX2124.2
078600 READ-FAIL-F2-06.                                                 IX2124.2
078700     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2124.2
078800     PERFORM FAIL.                                                IX2124.2
078900     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
079000     MOVE "AAA017" TO CORRECT-A.                                  IX2124.2
079100 READ-WRITE-F2-06.                                                IX2124.2
079200     PERFORM PRINT-DETAIL.                                        IX2124.2
079300 READ-TEST-F2-07.                                                 IX2124.2
079400     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
079500     MOVE "READ-TEST-F2-07"  TO PAR-NAME.                         IX2124.2
079600     MOVE "RETRIEVED BY PRIMARY KEY" TO RE-MARK.                  IX2124.2
079700     MOVE "AAA018" TO IX-FS1-KEY.                                 IX2124.2
079800     READ IX-FS1 RECORD                                           IX2124.2
079900                      KEY IS IX-FS1-KEY                           IX2124.2
080000       INVALID KEY  GO TO READ-INVALID-F2-07.                     IX2124.2
080100     IF IX-FS1-KEY = "AAA018"                                     IX2124.2
080200        PERFORM PASS                                              IX2124.2
080300        ELSE GO TO READ-FAIL-F2-07.                               IX2124.2
080400     GO TO READ-WRITE-F2-07.                                      IX2124.2
080500 READ-DELETE-F2-07.                                               IX2124.2
080600     PERFORM DE-LETE.                                             IX2124.2
080700     GO TO READ-WRITE-F2-07.                                      IX2124.2
080800 READ-INVALID-F2-07.                                              IX2124.2
080900     MOVE "IX-28; 4.5.2 F2, INVALID KEY CONDITION (IX-6) "        IX2124.2
081000          TO RE-MARK.                                             IX2124.2
081100     PERFORM FAIL.                                                IX2124.2
081200     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
081300     MOVE "AAA018" TO CORRECT-A.                                  IX2124.2
081400     GO TO READ-WRITE-F2-07.                                      IX2124.2
081500 READ-FAIL-F2-07.                                                 IX2124.2
081600     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2124.2
081700     PERFORM FAIL.                                                IX2124.2
081800     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
081900     MOVE "AAA018" TO CORRECT-A.                                  IX2124.2
082000 READ-WRITE-F2-07.                                                IX2124.2
082100     PERFORM PRINT-DETAIL.                                        IX2124.2
082200 DELETE-TEST-GF-02.                                               IX2124.2
082300     MOVE "DELETE...RECORD" TO FEATURE.                           IX2124.2
082400     MOVE "DELETE-TEST-GF-02" TO PAR-NAME.                        IX2124.2
082500     MOVE "DELETES RECORD FOUND IN READ-TEST-F2-07" TO RE-MARK.   IX2124.2
082600     DELETE IX-FS1 RECORD INVALID KEY GO TO DELETE-FAIL-GF-02.    IX2124.2
082700     PERFORM PASS.                                                IX2124.2
082800     GO TO DELETE-WRITE-GF-02.                                    IX2124.2
082900 DELETE-DELETE-GF-02.                                             IX2124.2
083000     PERFORM DE-LETE.                                             IX2124.2
083100     GO TO DELETE-WRITE-GF-02.                                    IX2124.2
083200 DELETE-FAIL-GF-02.                                               IX2124.2
083300     MOVE "IX-21; 4.3.2 INVALID KEY PATH TAKEN (IX-6)" TO RE-MARK.IX2124.2
083400     PERFORM FAIL.                                                IX2124.2
083500     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
083600     MOVE "AAA018" TO CORRECT-A.                                  IX2124.2
083700 DELETE-WRITE-GF-02.                                              IX2124.2
083800     PERFORM PRINT-DETAIL.                                        IX2124.2
083900 READ-TEST-F1-08.                                                 IX2124.2
084000     MOVE "READ...NEXT RECORD" TO FEATURE.                        IX2124.2
084100     MOVE "READ-TEST-F1-08"  TO PAR-NAME.                         IX2124.2
084200     MOVE "READS NEXT RECORD" TO RE-MARK.                         IX2124.2
084300     READ IX-FS1 NEXT RECORD AT END GO TO READ-END-F1-08.         IX2124.2
084400     IF IX-FS1-KEY = "AAA019"                                     IX2124.2
084500        PERFORM PASS                                              IX2124.2
084600        ELSE GO TO READ-FAIL-F1-08.                               IX2124.2
084700     GO TO READ-WRITE-F1-08.                                      IX2124.2
084800 READ-DELETE-F1-08.                                               IX2124.2
084900     PERFORM DE-LETE.                                             IX2124.2
085000     GO TO READ-WRITE-F1-08.                                      IX2124.2
085100 READ-END-F1-08.                                                  IX2124.2
085200     MOVE "IX-28; 4.5.2 F1, AT END CONDITION TAKEN" TO RE-MARK.   IX2124.2
085300     PERFORM FAIL.                                                IX2124.2
085400     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2124.2
085500     MOVE "AAA019" TO CORRECT-A.                                  IX2124.2
085600     GO TO READ-WRITE-F1-08.                                      IX2124.2
085700 READ-FAIL-F1-08.                                                 IX2124.2
085800     MOVE "IX-28; 4.5.2 F1, WRONG KEY            " TO RE-MARK.    IX2124.2
085900     PERFORM FAIL.                                                IX2124.2
086000     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
086100     MOVE "AAA019" TO CORRECT-A.                                  IX2124.2
086200 READ-WRITE-F1-08.                                                IX2124.2
086300     PERFORM PRINT-DETAIL.                                        IX2124.2
086400 REWRITE-TEST-GF-02.                                              IX2124.2
086500     MOVE "REWRITE...INVALID..." TO FEATURE.                      IX2124.2
086600     MOVE "REWRITE-TEST-GF-02" TO PAR-NAME.                       IX2124.2
086700     MOVE "REWRITES RECORD FOUND IN READ-TEST-F1-08" TO RE-MARK.  IX2124.2
086800     MOVE "SSSSSSALT09" TO IX-FS1-ALT09.                          IX2124.2
086900     REWRITE IX-FS1-RECORD INVALID KEY GO TO REWRITE-FAIL-GF-02.  IX2124.2
087000     PERFORM PASS.                                                IX2124.2
087100     GO TO REWRITE-WRITE-GF-02.                                   IX2124.2
087200 REWRITE-DELETE-GF-02.                                            IX2124.2
087300     PERFORM DE-LETE.                                             IX2124.2
087400     GO TO REWRITE-WRITE-GF-02.                                   IX2124.2
087500 REWRITE-FAIL-GF-02.                                              IX2124.2
087600     MOVE "IX-33; 4.6.2 F2, INVALID KEY CONDITION (IX-6) "        IX2124.2
087700           TO RE-MARK.                                            IX2124.2
087800     PERFORM FAIL.                                                IX2124.2
087900     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
088000     MOVE "SSSSSSALT09" TO CORRECT-A.                             IX2124.2
088100 REWRITE-WRITE-GF-02.                                             IX2124.2
088200     PERFORM PRINT-DETAIL.                                        IX2124.2
088300 READ-TEST-F2-09.                                                 IX2124.2
088400     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
088500     MOVE "READ-TEST-F2-09" TO PAR-NAME.                          IX2124.2
088600     MOVE "RETRIEVED BY ALTERNATE KEY 2" TO RE-MARK.              IX2124.2
088700     MOVE "EEE075ALT02" TO IX-FS1-ALT02.                          IX2124.2
088800     READ IX-FS1 RECORD                                           IX2124.2
088900                      KEY IS IX-FS1-ALT02                         IX2124.2
089000       INVALID KEY  GO TO READ-INVALID-F2-09.                     IX2124.2
089100     IF IX-FS1-KEY = "AAA075"                                     IX2124.2
089200        PERFORM PASS                                              IX2124.2
089300        ELSE GO TO READ-FAIL-F2-09.                               IX2124.2
089400     GO TO READ-WRITE-F2-09.                                      IX2124.2
089500 READ-DELETE-F2-09.                                               IX2124.2
089600     PERFORM DE-LETE.                                             IX2124.2
089700     GO TO READ-WRITE-F2-09.                                      IX2124.2
089800 READ-INVALID-F2-09.                                              IX2124.2
089900     MOVE "IX-28; 4.5.2 F2, INVALID KEY CONDITION (IX-6) "        IX2124.2
090000           TO RE-MARK.                                            IX2124.2
090100     PERFORM FAIL.                                                IX2124.2
090200     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
090300     MOVE "EEE075ALT02" TO CORRECT-A.                             IX2124.2
090400     GO TO READ-WRITE-F2-09.                                      IX2124.2
090500 READ-FAIL-F2-09.                                                 IX2124.2
090600     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2124.2
090700     PERFORM FAIL.                                                IX2124.2
090800     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
090900     MOVE "AAA075" TO CORRECT-A.                                  IX2124.2
091000 READ-WRITE-F2-09.                                                IX2124.2
091100     PERFORM PRINT-DETAIL.                                        IX2124.2
091200 REWRITE-TEST-GF-03.                                              IX2124.2
091300     MOVE "REWRITE...INVALID..." TO FEATURE.                      IX2124.2
091400     MOVE "REWRITE-TEST-GF-03" TO PAR-NAME.                       IX2124.2
091500     MOVE "INVALID KEY PATH SHOULD BE TAKEN" TO RE-MARK.          IX2124.2
091600     MOVE "EEE076ALT02" TO IX-FS1-ALT02.                          IX2124.2
091700     REWRITE IX-FS1-RECORD INVALID KEY                            IX2124.2
091800        PERFORM PASS                                              IX2124.2
091900        GO TO REWRITE-WRITE-GF-03.                                IX2124.2
092000     GO TO REWRITE-FAIL-GF-03.                                    IX2124.2
092100 REWRITE-DELETE-GF-03.                                            IX2124.2
092200     PERFORM DE-LETE.                                             IX2124.2
092300     GO TO REWRITE-WRITE-GF-03.                                   IX2124.2
092400 REWRITE-FAIL-GF-03.                                              IX2124.2
092500     MOVE "IX-33; 4.6.2 & INVALID KEY CONDITION (IX-6) PATH NOT BEIX2124.2
092600-      " TAKEN"   TO RE-MARK.                                     IX2124.2
092700     PERFORM FAIL.                                                IX2124.2
092800     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
092900     MOVE "INVALID KEY" TO CORRECT-A.                             IX2124.2
093000 REWRITE-WRITE-GF-03.                                             IX2124.2
093100     PERFORM PRINT-DETAIL.                                        IX2124.2
093200 READ-TEST-F2-10.                                                 IX2124.2
093300     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
093400     MOVE "READ-TEST-F2-10" TO PAR-NAME.                          IX2124.2
093500     MOVE "INVALID KEY PATH SHOULD BE TAKEN" TO RE-MARK.          IX2124.2
093600     MOVE "KKK018ALT05" TO IX-FS1-ALT05.                          IX2124.2
093700     READ IX-FS1 RECORD                                           IX2124.2
093800                      KEY IS IX-FS1-ALT05                         IX2124.2
093900       INVALID KEY  PERFORM PASS                                  IX2124.2
094000        GO TO READ-WRITE-F2-10.                                   IX2124.2
094100     GO TO READ-FAIL-F2-10.                                       IX2124.2
094200 READ-DELETE-F2-10.                                               IX2124.2
094300     PERFORM DE-LETE.                                             IX2124.2
094400     GO TO READ-WRITE-F2-10.                                      IX2124.2
094500 READ-FAIL-F2-10.                                                 IX2124.2
094600     MOVE "IX-28; 4.5.2 & INVALID KEY CONDITION (IX-6) PATH NOT BEIX2124.2
094700-      " TAKEN"   TO RE-MARK.                                     IX2124.2
094800     PERFORM FAIL.                                                IX2124.2
094900     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
095000     MOVE "INVALID KEY" TO CORRECT-A.                             IX2124.2
095100 READ-WRITE-F2-10.                                                IX2124.2
095200     PERFORM PRINT-DETAIL.                                        IX2124.2
095300 READ-TEST-F2-11.                                                 IX2124.2
095400     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
095500     MOVE "READ-TEST-F2-11"  TO PAR-NAME.                         IX2124.2
095600     MOVE "RETRIEVED BY ALTERNATE KEY 9" TO RE-MARK.              IX2124.2
095700     MOVE "SSSSSSALT09" TO IX-FS1-ALT09.                          IX2124.2
095800     READ IX-FS1 RECORD                                           IX2124.2
095900                      KEY IS IX-FS1-ALT09                         IX2124.2
096000        INVALID KEY GO TO READ-INVALID-F2-11.                     IX2124.2
096100     IF IX-FS1-KEY = "AAA019"                                     IX2124.2
096200        PERFORM PASS                                              IX2124.2
096300        ELSE GO TO READ-FAIL-F2-11.                               IX2124.2
096400     GO TO READ-WRITE-F2-11.                                      IX2124.2
096500 READ-DELETE-F2-11.                                               IX2124.2
096600     PERFORM DE-LETE.                                             IX2124.2
096700     GO TO READ-WRITE-F2-11.                                      IX2124.2
096800 READ-INVALID-F2-11.                                              IX2124.2
096900     MOVE "IX-28; 4.5.2 & INVALID KEY CONDITION (IX-6) PATH     BEIX2124.2
097000-      " TAKEN"   TO RE-MARK.                                     IX2124.2
097100     PERFORM FAIL.                                                IX2124.2
097200     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
097300     MOVE "SSSSSSALT09" TO CORRECT-A.                             IX2124.2
097400     GO TO READ-WRITE-F2-11.                                      IX2124.2
097500 READ-FAIL-F2-11.                                                 IX2124.2
097600     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2124.2
097700     PERFORM FAIL.                                                IX2124.2
097800     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
097900     MOVE "AAA019" TO CORRECT-A.                                  IX2124.2
098000 READ-WRITE-F2-11.                                                IX2124.2
098100     PERFORM PRINT-DETAIL.                                        IX2124.2
098200 READ-TEST-F2-12.                                                 IX2124.2
098300     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
098400     MOVE "READ-TEST-F2-12" TO PAR-NAME.                          IX2124.2
098500     MOVE "RETRIEVED BY ALTERNATE KEY 7" TO RE-MARK.              IX2124.2
098600     MOVE "OOO026ALT07"  TO IX-FS1-ALT07.                         IX2124.2
098700     READ IX-FS1 RECORD                                           IX2124.2
098800                      KEY IS IX-FS1-ALT07                         IX2124.2
098900        INVALID KEY GO TO READ-INVALID-F2-12.                     IX2124.2
099000     IF IX-FS1-KEY = "AAA026"                                     IX2124.2
099100        PERFORM PASS                                              IX2124.2
099200        ELSE GO TO READ-FAIL-F2-12.                               IX2124.2
099300     GO TO READ-WRITE-F2-12.                                      IX2124.2
099400 READ-DELETE-F2-12.                                               IX2124.2
099500     PERFORM DE-LETE.                                             IX2124.2
099600     GO TO READ-WRITE-F2-12.                                      IX2124.2
099700 READ-INVALID-F2-12.                                              IX2124.2
099800     MOVE "IX-28; 4.5.2 & INVALID KEY CONDITION (IX-6) PATH     BEIX2124.2
099900-      " TAKEN"   TO RE-MARK.                                     IX2124.2
100000     PERFORM FAIL.                                                IX2124.2
100100     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
100200     MOVE "OOO026ALT07" TO CORRECT-A.                             IX2124.2
100300     GO TO READ-WRITE-F2-12.                                      IX2124.2
100400 READ-FAIL-F2-12.                                                 IX2124.2
100500     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2124.2
100600     PERFORM FAIL.                                                IX2124.2
100700     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
100800     MOVE "AAA026" TO CORRECT-A.                                  IX2124.2
100900 READ-WRITE-F2-12.                                                IX2124.2
101000     PERFORM PRINT-DETAIL.                                        IX2124.2
101100 DELETE-TEST-GF-03.                                               IX2124.2
101200     MOVE "DELETE...RECORD" TO FEATURE.                           IX2124.2
101300     MOVE "DELETE-TEST-GF-03" TO PAR-NAME.                        IX2124.2
101400     MOVE "DELETES RECORD FOUND IN READ-TEST-F2-12" TO RE-MARK.   IX2124.2
101500     DELETE IX-FS1 RECORD INVALID KEY GO TO DELETE-FAIL-GF-03.    IX2124.2
101600     PERFORM PASS.                                                IX2124.2
101700     GO TO DELETE-WRITE-GF-03.                                    IX2124.2
101800 DELETE-DELETE-GF-03.                                             IX2124.2
101900     PERFORM DE-LETE.                                             IX2124.2
102000     GO TO DELETE-WRITE-GF-03.                                    IX2124.2
102100 DELETE-FAIL-GF-03.                                               IX2124.2
102200     MOVE "IX-21; 4.3.2 & INVALID KEY CONDITION (IX-6) PATH     BEIX2124.2
102300-      " TAKEN"   TO RE-MARK.                                     IX2124.2
102400     PERFORM FAIL.                                                IX2124.2
102500     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2124.2
102600     MOVE "AAA026" TO CORRECT-A.                                  IX2124.2
102700 DELETE-WRITE-GF-03.                                              IX2124.2
102800     PERFORM PRINT-DETAIL.                                        IX2124.2
102900 READ-TEST-F2-13.                                                 IX2124.2
103000     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2124.2
103100     MOVE "READ-TEST-F2-13" TO PAR-NAME.                          IX2124.2
103200     MOVE "INVALID KEY PATH SHOULD BE TAKEN" TO RE-MARK.          IX2124.2
103300     MOVE "KKK026ALT05" TO IX-FS1-ALT05.                          IX2124.2
103400     READ IX-FS1 RECORD                                           IX2124.2
103500                      KEY IS IX-FS1-ALT05                         IX2124.2
103600       INVALID KEY  PERFORM PASS                                  IX2124.2
103700        GO TO READ-WRITE-F2-13.                                   IX2124.2
103800     GO TO READ-FAIL-F2-13.                                       IX2124.2
103900 READ-DELETE-F2-13.                                               IX2124.2
104000     PERFORM DE-LETE.                                             IX2124.2
104100     GO TO READ-WRITE-F2-13.                                      IX2124.2
104200 READ-FAIL-F2-13.                                                 IX2124.2
104300     MOVE "IX-28; 4.5.2 & INVALID KEY CONDITION (IX-6) PATH NOT BEIX2124.2
104400-      " TAKEN"   TO RE-MARK.                                     IX2124.2
104500     PERFORM FAIL.                                                IX2124.2
104600     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2124.2
104700     MOVE "INVALID KEY" TO CORRECT-A.                             IX2124.2
104800 READ-WRITE-F2-13.                                                IX2124.2
104900     PERFORM PRINT-DETAIL.                                        IX2124.2
105000     CLOSE IX-FS1.                                                IX2124.2
105100 CCVS-EXIT SECTION.                                               IX2124.2
105200 CCVS-999999.                                                     IX2124.2
105300     GO TO CLOSE-FILES.                                           IX2124.2
      *END-OF,IX212A                                                                  
