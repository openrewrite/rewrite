      *HEADER,COBOL,IX213A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2134.2
000200 PROGRAM-ID.                                                      IX2134.2
000300     IX213A.                                                      IX2134.2
000400****************************************************************  IX2134.2
000500*                                                              *  IX2134.2
000600*    VALIDATION FOR:-                                          *  IX2134.2
000700*                                                              *  IX2134.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2134.2
000900*                                                              *  IX2134.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2134.2
001100*                                                              *  IX2134.2
001200****************************************************************  IX2134.2
001300*                                                              *  IX2134.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IX2134.2
001500*                                                              *  IX2134.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IX2134.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IX2134.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IX2134.2
001900*                                                              *  IX2134.2
002000****************************************************************  IX2134.2
002100*    THIS PROGRAM CREATES A 100 RECORD FIXED LENGTH INDEXED FILE  IX2134.2
002200*    WHOSE  ACCESS  MODES  IS DYNAMIC AND CONTAINS 100 DUPLICATE  IX2134.2
002300*    ALTERNATE  KEYS.  THE  INDEXED  FILE  IS MANIPULATED BY THE  IX2134.2
002400*    DUPLICATE ALTERNATE KEYS USING THE FOLLOWING VERBS:          IX2134.2
002500*                                                                 IX2134.2
002600*            . DELETE                                             IX2134.2
002700*            . READ ... NEXT RECORD                               IX2134.2
002800*            . READ ... RECORD KEY                                IX2134.2
002900*            . REWRITE                                            IX2134.2
003000*            . START                                              IX2134.2
003100*                                                                 IX2134.2
003200 ENVIRONMENT DIVISION.                                            IX2134.2
003300 CONFIGURATION SECTION.                                           IX2134.2
003400 SOURCE-COMPUTER.                                                 IX2134.2
003500     XXXXX082.                                                    IX2134.2
003600 OBJECT-COMPUTER.                                                 IX2134.2
003700     XXXXX083.                                                    IX2134.2
003800 INPUT-OUTPUT SECTION.                                            IX2134.2
003900 FILE-CONTROL.                                                    IX2134.2
004000*    SELECT RAW-DATA   ASSIGN TO                                  IX2134.2
004100*    XXXXX062                                                     IX2134.2
004200*           ORGANIZATION IS INDEXED                               IX2134.2
004300*           ACCESS MODE IS RANDOM                                 IX2134.2
004400*           RECORD KEY IS RAW-DATA-KEY.                           IX2134.2
004500     SELECT PRINT-FILE ASSIGN TO                                  IX2134.2
004600     XXXXX055.                                                    IX2134.2
004700     SELECT IX-FS1                                                IX2134.2
004800     ASSIGN TO                                                    IX2134.2
004900     XXXXX024                                                     IX2134.2
005000*    XXXXX044                                                     IX2134.2
005100     ACCESS MODE IS DYNAMIC                                       IX2134.2
005200     RECORD KEY IS IX-FS1-KEY                                     IX2134.2
005300     ALTERNATE RECORD KEY IS IX-FS1-ALT01 WITH DUPLICATES         IX2134.2
005400     ALTERNATE RECORD KEY IS IX-FS1-ALT02 WITH DUPLICATES         IX2134.2
005500     ALTERNATE RECORD KEY IS IX-FS1-ALT03 WITH DUPLICATES         IX2134.2
005600     ALTERNATE RECORD KEY IS IX-FS1-ALT04 WITH DUPLICATES         IX2134.2
005700     ALTERNATE RECORD KEY IS IX-FS1-ALT05 WITH DUPLICATES         IX2134.2
005800     ALTERNATE RECORD KEY IS IX-FS1-ALT06 WITH DUPLICATES         IX2134.2
005900     ALTERNATE RECORD KEY IS IX-FS1-ALT07 WITH DUPLICATES         IX2134.2
006000     ALTERNATE RECORD KEY IS IX-FS1-ALT08 WITH DUPLICATES         IX2134.2
006100     ALTERNATE RECORD KEY IS IX-FS1-ALT09 WITH DUPLICATES         IX2134.2
006200     ALTERNATE RECORD KEY IS IX-FS1-ALT10 WITH DUPLICATES         IX2134.2
006300     ORGANIZATION IS INDEXED.                                     IX2134.2
006400 DATA DIVISION.                                                   IX2134.2
006500 FILE SECTION.                                                    IX2134.2
006600*                                                                 IX2134.2
006700*FD  RAW-DATA.                                                    IX2134.2
006800*                                                                 IX2134.2
006900*01  RAW-DATA-SATZ.                                               IX2134.2
007000*    05  RAW-DATA-KEY        PIC X(6).                            IX2134.2
007100*    05  C-DATE              PIC 9(6).                            IX2134.2
007200*    05  C-TIME              PIC 9(8).                            IX2134.2
007300*    05  C-NO-OF-TESTS       PIC 99.                              IX2134.2
007400*    05  C-OK                PIC 999.                             IX2134.2
007500*    05  C-ALL               PIC 999.                             IX2134.2
007600*    05  C-FAIL              PIC 999.                             IX2134.2
007700*    05  C-DELETED           PIC 999.                             IX2134.2
007800*    05  C-INSPECT           PIC 999.                             IX2134.2
007900*    05  C-NOTE              PIC X(13).                           IX2134.2
008000*    05  C-INDENT            PIC X.                               IX2134.2
008100*    05  C-ABORT             PIC X(8).                            IX2134.2
008200 FD  PRINT-FILE.                                                  IX2134.2
008300 01  PRINT-REC PICTURE X(120).                                    IX2134.2
008400 01  DUMMY-RECORD PICTURE X(120).                                 IX2134.2
008500 FD  IX-FS1                                                       IX2134.2
008600C    LABEL RECORDS ARE STANDARD                                   IX2134.2
008700C    DATA RECORD IS IX-FS1-RECORD                                 IX2134.2
008800     RECORD CONTAINS 116 CHARACTERS.                              IX2134.2
008900 01  IX-FS1-RECORD.                                               IX2134.2
009000     02 IX-FS1-KEY                 PIC X(6).                      IX2134.2
009100     02 IX-FS1-ALT01               PIC X(11).                     IX2134.2
009200     02 IX-FS1-ALT02               PIC X(11).                     IX2134.2
009300     02 IX-FS1-ALT03               PIC X(11).                     IX2134.2
009400     02 IX-FS1-ALT04               PIC X(11).                     IX2134.2
009500     02 IX-FS1-ALT05               PIC X(11).                     IX2134.2
009600     02 IX-FS1-ALT06               PIC X(11).                     IX2134.2
009700     02 IX-FS1-ALT07               PIC X(11).                     IX2134.2
009800     02 IX-FS1-ALT08               PIC X(11).                     IX2134.2
009900     02 IX-FS1-ALT09               PIC X(11).                     IX2134.2
010000     02 IX-FS1-ALT10               PIC X(11).                     IX2134.2
010100 WORKING-STORAGE SECTION.                                         IX2134.2
010200 01  RECORD-COUNTER              PIC 999    VALUE ZEROS.          IX2134.2
010300 01  INVKEY-COUNTER              PIC 999    VALUE ZEROS.          IX2134.2
010400 01  WORK-RECORD.                                                 IX2134.2
010500     02 FILLER                   PIC XXX    VALUE "AAA".          IX2134.2
010600     02 COUNTER00                PIC 999.                         IX2134.2
010700     02 FILLER                   PIC XXX    VALUE "CCC".          IX2134.2
010800     02 COUNTER01                PIC 999.                         IX2134.2
010900     02 FILLER                   PIC X(5)   VALUE "ALT01".        IX2134.2
011000     02 FILLER                   PIC XXX    VALUE "EEE".          IX2134.2
011100     02 COUNTER02                PIC 999.                         IX2134.2
011200     02 FILLER                   PIC X(5)   VALUE "ALT02".        IX2134.2
011300     02 FILLER                   PIC XXX    VALUE "GGG".          IX2134.2
011400     02 COUNTER03                PIC 999.                         IX2134.2
011500     02 FILLER                   PIC X(5)   VALUE "ALT03".        IX2134.2
011600     02 FILLER                   PIC XXX    VALUE "III".          IX2134.2
011700     02 COUNTER04                PIC 999.                         IX2134.2
011800     02 FILLER                   PIC X(5)   VALUE "ALT04".        IX2134.2
011900     02 FILLER                   PIC XXX    VALUE "KKK".          IX2134.2
012000     02 COUNTER05                PIC 999.                         IX2134.2
012100     02 FILLER                   PIC X(5)   VALUE "ALT05".        IX2134.2
012200     02 FILLER                   PIC XXX    VALUE "MMM".          IX2134.2
012300     02 COUNTER06                PIC 999.                         IX2134.2
012400     02 FILLER                   PIC X(5)   VALUE "ALT06".        IX2134.2
012500     02 FILLER                   PIC XXX    VALUE "OOO".          IX2134.2
012600     02 COUNTER07                PIC 999.                         IX2134.2
012700     02 FILLER                   PIC X(5)   VALUE "ALT07".        IX2134.2
012800     02 FILLER                   PIC XXX    VALUE "QQQ".          IX2134.2
012900     02 COUNTER08                PIC 999.                         IX2134.2
013000     02 FILLER                   PIC X(5)   VALUE "ALT08".        IX2134.2
013100     02 FILLER                   PIC XXX    VALUE "SSS".          IX2134.2
013200     02 COUNTER09                PIC 999.                         IX2134.2
013300     02 FILLER                   PIC X(5)   VALUE "ALT09".        IX2134.2
013400     02 FILLER                   PIC XXX    VALUE "UUU".          IX2134.2
013500     02 COUNTER10                PIC 999.                         IX2134.2
013600     02 FILLER                   PIC X(5)   VALUE "ALT10".        IX2134.2
013700 01  TEST-RESULTS.                                                IX2134.2
013800     02 FILLER                   PIC X      VALUE SPACE.          IX2134.2
013900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2134.2
014000     02 FILLER                   PIC X      VALUE SPACE.          IX2134.2
014100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2134.2
014200     02 FILLER                   PIC X      VALUE SPACE.          IX2134.2
014300     02  PAR-NAME.                                                IX2134.2
014400       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2134.2
014500       03  PARDOT-X              PIC X      VALUE SPACE.          IX2134.2
014600       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2134.2
014700     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2134.2
014800     02 RE-MARK                  PIC X(61).                       IX2134.2
014900 01  TEST-COMPUTED.                                               IX2134.2
015000     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2134.2
015100     02 FILLER                   PIC X(17)  VALUE                 IX2134.2
015200            "       COMPUTED=".                                   IX2134.2
015300     02 COMPUTED-X.                                               IX2134.2
015400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2134.2
015500     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2134.2
015600                                 PIC -9(9).9(9).                  IX2134.2
015700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2134.2
015800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2134.2
015900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2134.2
016000     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2134.2
016100         04 COMPUTED-18V0                    PIC -9(18).          IX2134.2
016200         04 FILLER                           PIC X.               IX2134.2
016300     03 FILLER PIC X(50) VALUE SPACE.                             IX2134.2
016400 01  TEST-CORRECT.                                                IX2134.2
016500     02 FILLER PIC X(30) VALUE SPACE.                             IX2134.2
016600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2134.2
016700     02 CORRECT-X.                                                IX2134.2
016800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2134.2
016900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2134.2
017000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2134.2
017100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2134.2
017200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2134.2
017300     03      CR-18V0 REDEFINES CORRECT-A.                         IX2134.2
017400         04 CORRECT-18V0                     PIC -9(18).          IX2134.2
017500         04 FILLER                           PIC X.               IX2134.2
017600     03 FILLER PIC X(2) VALUE SPACE.                              IX2134.2
017700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2134.2
017800 01  CCVS-C-1.                                                    IX2134.2
017900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2134.2
018000-    "SS  PARAGRAPH-NAME                                          IX2134.2
018100-    "       REMARKS".                                            IX2134.2
018200     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2134.2
018300 01  CCVS-C-2.                                                    IX2134.2
018400     02 FILLER                     PIC X        VALUE SPACE.      IX2134.2
018500     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2134.2
018600     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2134.2
018700     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2134.2
018800     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2134.2
018900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2134.2
019000 01  REC-CT                        PIC 99       VALUE ZERO.       IX2134.2
019100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2134.2
019200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2134.2
019300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2134.2
019400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2134.2
019500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2134.2
019600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2134.2
019700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2134.2
019800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2134.2
019900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2134.2
020000 01  CCVS-H-1.                                                    IX2134.2
020100     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2134.2
020200     02  FILLER                    PIC X(42)    VALUE             IX2134.2
020300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2134.2
020400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2134.2
020500 01  CCVS-H-2A.                                                   IX2134.2
020600   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2134.2
020700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2134.2
020800   02  FILLER                        PIC XXXX   VALUE             IX2134.2
020900     "4.2 ".                                                      IX2134.2
021000   02  FILLER                        PIC X(28)  VALUE             IX2134.2
021100            " COPY - NOT FOR DISTRIBUTION".                       IX2134.2
021200   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2134.2
021300                                                                  IX2134.2
021400 01  CCVS-H-2B.                                                   IX2134.2
021500   02  FILLER                        PIC X(15)  VALUE             IX2134.2
021600            "TEST RESULT OF ".                                    IX2134.2
021700   02  TEST-ID                       PIC X(9).                    IX2134.2
021800   02  FILLER                        PIC X(4)   VALUE             IX2134.2
021900            " IN ".                                               IX2134.2
022000   02  FILLER                        PIC X(12)  VALUE             IX2134.2
022100     " HIGH       ".                                              IX2134.2
022200   02  FILLER                        PIC X(22)  VALUE             IX2134.2
022300            " LEVEL VALIDATION FOR ".                             IX2134.2
022400   02  FILLER                        PIC X(58)  VALUE             IX2134.2
022500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2134.2
022600 01  CCVS-H-3.                                                    IX2134.2
022700     02  FILLER                      PIC X(34)  VALUE             IX2134.2
022800            " FOR OFFICIAL USE ONLY    ".                         IX2134.2
022900     02  FILLER                      PIC X(58)  VALUE             IX2134.2
023000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2134.2
023100     02  FILLER                      PIC X(28)  VALUE             IX2134.2
023200            "  COPYRIGHT   1985 ".                                IX2134.2
023300 01  CCVS-E-1.                                                    IX2134.2
023400     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2134.2
023500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2134.2
023600     02 ID-AGAIN                     PIC X(9).                    IX2134.2
023700     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2134.2
023800 01  CCVS-E-2.                                                    IX2134.2
023900     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2134.2
024000     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2134.2
024100     02 CCVS-E-2-2.                                               IX2134.2
024200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2134.2
024300         03 FILLER                   PIC X      VALUE SPACE.      IX2134.2
024400         03 ENDER-DESC               PIC X(44)  VALUE             IX2134.2
024500            "ERRORS ENCOUNTERED".                                 IX2134.2
024600 01  CCVS-E-3.                                                    IX2134.2
024700     02  FILLER                      PIC X(22)  VALUE             IX2134.2
024800            " FOR OFFICIAL USE ONLY".                             IX2134.2
024900     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2134.2
025000     02  FILLER                      PIC X(58)  VALUE             IX2134.2
025100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2134.2
025200     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2134.2
025300     02 FILLER                       PIC X(15)  VALUE             IX2134.2
025400             " COPYRIGHT 1985".                                   IX2134.2
025500 01  CCVS-E-4.                                                    IX2134.2
025600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2134.2
025700     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2134.2
025800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2134.2
025900     02 FILLER                       PIC X(40)  VALUE             IX2134.2
026000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2134.2
026100 01  XXINFO.                                                      IX2134.2
026200     02 FILLER                       PIC X(19)  VALUE             IX2134.2
026300            "*** INFORMATION ***".                                IX2134.2
026400     02 INFO-TEXT.                                                IX2134.2
026500       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2134.2
026600       04 XXCOMPUTED                 PIC X(20).                   IX2134.2
026700       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2134.2
026800       04 XXCORRECT                  PIC X(20).                   IX2134.2
026900     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2134.2
027000 01  HYPHEN-LINE.                                                 IX2134.2
027100     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2134.2
027200     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2134.2
027300-    "*****************************************".                 IX2134.2
027400     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2134.2
027500-    "******************************".                            IX2134.2
027600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2134.2
027700     "IX213A".                                                    IX2134.2
027800 PROCEDURE DIVISION.                                              IX2134.2
027900 CCVS1 SECTION.                                                   IX2134.2
028000 OPEN-FILES.                                                      IX2134.2
028100*    OPEN I-O RAW-DATA.                                           IX2134.2
028200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2134.2
028300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2134.2
028400*    MOVE "ABORTED " TO C-ABORT.                                  IX2134.2
028500*    ADD 1 TO C-NO-OF-TESTS.                                      IX2134.2
028600*    ACCEPT C-DATE  FROM DATE.                                    IX2134.2
028700*    ACCEPT C-TIME  FROM TIME.                                    IX2134.2
028800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2134.2
028900*END-E-1.                                                         IX2134.2
029000*    CLOSE RAW-DATA.                                              IX2134.2
029100     OPEN    OUTPUT PRINT-FILE.                                   IX2134.2
029200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2134.2
029300     MOVE    SPACE TO TEST-RESULTS.                               IX2134.2
029400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2134.2
029500     GO TO CCVS1-EXIT.                                            IX2134.2
029600 CLOSE-FILES.                                                     IX2134.2
029700*    OPEN I-O RAW-DATA.                                           IX2134.2
029800*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2134.2
029900*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2134.2
030000*    MOVE "OK.     " TO C-ABORT.                                  IX2134.2
030100*    MOVE PASS-COUNTER TO C-OK.                                   IX2134.2
030200*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2134.2
030300*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2134.2
030400*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2134.2
030500*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2134.2
030600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2134.2
030700*END-E-2.                                                         IX2134.2
030800*    CLOSE RAW-DATA.                                              IX2134.2
030900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2134.2
031000 TERMINATE-CCVS.                                                  IX2134.2
031100*    EXIT PROGRAM.                                                IX2134.2
031200*TERMINATE-CALL.                                                  IX2134.2
031300     STOP     RUN.                                                IX2134.2
031400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2134.2
031500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2134.2
031600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2134.2
031700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2134.2
031800     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2134.2
031900 PRINT-DETAIL.                                                    IX2134.2
032000     IF REC-CT NOT EQUAL TO ZERO                                  IX2134.2
032100             MOVE "." TO PARDOT-X                                 IX2134.2
032200             MOVE REC-CT TO DOTVALUE.                             IX2134.2
032300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2134.2
032400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2134.2
032500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2134.2
032600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2134.2
032700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2134.2
032800     MOVE SPACE TO CORRECT-X.                                     IX2134.2
032900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2134.2
033000     MOVE     SPACE TO RE-MARK.                                   IX2134.2
033100 HEAD-ROUTINE.                                                    IX2134.2
033200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2134.2
033300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2134.2
033400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2134.2
033500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2134.2
033600 COLUMN-NAMES-ROUTINE.                                            IX2134.2
033700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2134.2
033800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2134.2
033900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2134.2
034000 END-ROUTINE.                                                     IX2134.2
034100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2134.2
034200 END-RTN-EXIT.                                                    IX2134.2
034300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2134.2
034400 END-ROUTINE-1.                                                   IX2134.2
034500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2134.2
034600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2134.2
034700      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2134.2
034800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2134.2
034900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2134.2
035000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2134.2
035100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2134.2
035200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2134.2
035300  END-ROUTINE-12.                                                 IX2134.2
035400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2134.2
035500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2134.2
035600         MOVE "NO " TO ERROR-TOTAL                                IX2134.2
035700         ELSE                                                     IX2134.2
035800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2134.2
035900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2134.2
036000     PERFORM WRITE-LINE.                                          IX2134.2
036100 END-ROUTINE-13.                                                  IX2134.2
036200     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2134.2
036300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2134.2
036400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2134.2
036500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2134.2
036600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2134.2
036700      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2134.2
036800          MOVE "NO " TO ERROR-TOTAL                               IX2134.2
036900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2134.2
037000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2134.2
037100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2134.2
037200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2134.2
037300 WRITE-LINE.                                                      IX2134.2
037400     ADD 1 TO RECORD-COUNT.                                       IX2134.2
037500*    IF RECORD-COUNT GREATER 42                                   IX2134.2
037600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2134.2
037700*        MOVE SPACE TO DUMMY-RECORD                               IX2134.2
037800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2134.2
037900*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2134.2
038000*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2134.2
038100*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2134.2
038200*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2134.2
038300*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2134.2
038400*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2134.2
038500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2134.2
038600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2134.2
038700*        MOVE ZERO TO RECORD-COUNT.                               IX2134.2
038800     PERFORM WRT-LN.                                              IX2134.2
038900 WRT-LN.                                                          IX2134.2
039000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2134.2
039100     MOVE SPACE TO DUMMY-RECORD.                                  IX2134.2
039200 BLANK-LINE-PRINT.                                                IX2134.2
039300     PERFORM WRT-LN.                                              IX2134.2
039400 FAIL-ROUTINE.                                                    IX2134.2
039500     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2134.2
039600            GO TO   FAIL-ROUTINE-WRITE.                           IX2134.2
039700     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2134.2
039800     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2134.2
039900     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2134.2
040000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2134.2
040100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2134.2
040200     GO TO  FAIL-ROUTINE-EX.                                      IX2134.2
040300 FAIL-ROUTINE-WRITE.                                              IX2134.2
040400     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2134.2
040500     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2134.2
040600     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2134.2
040700     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2134.2
040800 FAIL-ROUTINE-EX. EXIT.                                           IX2134.2
040900 BAIL-OUT.                                                        IX2134.2
041000     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2134.2
041100     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2134.2
041200 BAIL-OUT-WRITE.                                                  IX2134.2
041300     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2134.2
041400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2134.2
041500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2134.2
041600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2134.2
041700 BAIL-OUT-EX. EXIT.                                               IX2134.2
041800 CCVS1-EXIT.                                                      IX2134.2
041900     EXIT.                                                        IX2134.2
042000 WRITE-INIT-GF-01.                                                IX2134.2
042100     OPEN OUTPUT IX-FS1.                                          IX2134.2
042200     PERFORM CREATE-IX-FS1 VARYING RECORD-COUNTER FROM 1 BY 1     IX2134.2
042300        UNTIL RECORD-COUNTER IS GREATER THAN 100.                 IX2134.2
042400 CREATE-IX-FS1.                                                   IX2134.2
042500     MOVE RECORD-COUNTER TO COUNTER00, COUNTER01, COUNTER02,      IX2134.2
042600        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
042700        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
042800     IF RECORD-COUNTER = 011 MOVE 010 TO COUNTER01, COUNTER02,    IX2134.2
042900        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
043000        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
043100     IF RECORD-COUNTER = 021 MOVE 020 TO COUNTER01, COUNTER02,    IX2134.2
043200        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
043300        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
043400     IF RECORD-COUNTER = 031 MOVE 030 TO COUNTER01, COUNTER02,    IX2134.2
043500        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
043600        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
043700     IF RECORD-COUNTER = 041 MOVE 040 TO COUNTER01, COUNTER02,    IX2134.2
043800        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
043900        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
044000     IF RECORD-COUNTER = 051 MOVE 050 TO COUNTER01, COUNTER02,    IX2134.2
044100        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
044200        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
044300     IF RECORD-COUNTER = 061 MOVE 060 TO COUNTER01, COUNTER02,    IX2134.2
044400        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
044500        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
044600     IF RECORD-COUNTER = 071 MOVE 070 TO COUNTER01, COUNTER02,    IX2134.2
044700        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
044800        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
044900     IF RECORD-COUNTER = 081 MOVE 080 TO COUNTER01, COUNTER02,    IX2134.2
045000        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
045100        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
045200     IF RECORD-COUNTER = 091 MOVE 090 TO COUNTER01, COUNTER02,    IX2134.2
045300        COUNTER03, COUNTER04, COUNTER05, COUNTER06, COUNTER07,    IX2134.2
045400        COUNTER08, COUNTER09, COUNTER10.                          IX2134.2
045500     WRITE IX-FS1-RECORD FROM WORK-RECORD INVALID KEY             IX2134.2
045600        ADD 1 TO INVKEY-COUNTER.                                  IX2134.2
045700 WRITE-TEST-GD-01.                                                IX2134.2
045800     MOVE "WRITE INVALID KEY" TO FEATURE.                         IX2134.2
045900     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         IX2134.2
046000     MOVE "CREATE IX-FS1" TO RE-MARK.                             IX2134.2
046100     IF INVKEY-COUNTER = 0                                        IX2134.2
046200        PERFORM PASS                                              IX2134.2
046300        ELSE GO TO WRITE-FAIL-GF-01.                              IX2134.2
046400     GO TO WRITE-WRITE-GF-01.                                     IX2134.2
046500 INDEX-DELETE-1.                                                  IX2134.2
046600     PERFORM DE-LETE.                                             IX2134.2
046700     GO TO WRITE-WRITE-GF-01.                                     IX2134.2
046800 WRITE-FAIL-GF-01.                                                IX2134.2
046900     MOVE "IX-41; 4.9.2, FILE NOT CREATED CORRECTLY" TO RE-MARK.  IX2134.2
047000     PERFORM FAIL.                                                IX2134.2
047100     MOVE INVKEY-COUNTER TO COMPUTED-18V0.                        IX2134.2
047200     MOVE 0 TO CORRECT-18V0.                                      IX2134.2
047300 WRITE-WRITE-GF-01.                                               IX2134.2
047400     PERFORM PRINT-DETAIL.                                        IX2134.2
047500 READ-TEST-F2-01.                                                 IX2134.2
047600     CLOSE IX-FS1.                                                IX2134.2
047700     OPEN I-O IX-FS1.                                             IX2134.2
047800     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
047900     MOVE "READ-TEST-F2-01" TO PAR-NAME.                          IX2134.2
048000     MOVE "RETRIEVED BY ALTERNATE KEY 1" TO RE-MARK.              IX2134.2
048100     MOVE "CCC050ALT01" TO IX-FS1-ALT01.                          IX2134.2
048200     READ IX-FS1 RECORD                                           IX2134.2
048300                        KEY IS IX-FS1-ALT01                       IX2134.2
048400       INVALID KEY  GO TO READ-INVALID-F2-01.                     IX2134.2
048500     IF IX-FS1-KEY = "AAA050"                                     IX2134.2
048600        PERFORM PASS                                              IX2134.2
048700        ELSE GO TO READ-FAIL-F2-01.                               IX2134.2
048800     GO TO READ-WRITE-F2-01.                                      IX2134.2
048900 READ-DELETE-F2-01.                                               IX2134.2
049000     PERFORM DE-LETE.                                             IX2134.2
049100     GO TO READ-WRITE-F2-01.                                      IX2134.2
049200 READ-INVALID-F2-01.                                              IX2134.2
049300     MOVE "IX-28; 4.5.2 AND IX-6 1.3.5 INVALID KEY PATH TAKEN"    IX2134.2
049400            TO RE-MARK.                                           IX2134.2
049500     PERFORM FAIL.                                                IX2134.2
049600     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
049700     MOVE "CCC050ALT01" TO CORRECT-A.                             IX2134.2
049800     GO TO READ-WRITE-F2-01.                                      IX2134.2
049900 READ-FAIL-F2-01.                                                 IX2134.2
050000     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
050100     PERFORM FAIL.                                                IX2134.2
050200     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
050300     MOVE "AAA050" TO CORRECT-A.                                  IX2134.2
050400 READ-WRITE-F2-01.                                                IX2134.2
050500     PERFORM PRINT-DETAIL.                                        IX2134.2
050600 READ-TEST-F2-02.                                                 IX2134.2
050700     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
050800     MOVE "READ-TEST-F2-02"  TO PAR-NAME.                         IX2134.2
050900     MOVE "RETRIEVED BY ALTERNATE KEY 8" TO RE-MARK.              IX2134.2
051000     MOVE "QQQ040ALT08" TO IX-FS1-ALT08.                          IX2134.2
051100     READ IX-FS1 RECORD                                           IX2134.2
051200                        KEY IS IX-FS1-ALT08                       IX2134.2
051300       INVALID KEY  GO TO READ-INVALID-F2-02.                     IX2134.2
051400     IF IX-FS1-KEY = "AAA040"                                     IX2134.2
051500        PERFORM PASS                                              IX2134.2
051600        ELSE GO TO READ-FAIL-F2-02.                               IX2134.2
051700     GO TO READ-WRITE-F2-02.                                      IX2134.2
051800 READ-DELETE-F2-02.                                               IX2134.2
051900     PERFORM DE-LETE.                                             IX2134.2
052000     GO TO READ-WRITE-F2-02.                                      IX2134.2
052100 READ-INVALID-F2-02.                                              IX2134.2
052200     MOVE "IX-28; 4.5.2 AND IX-6 1.3.5 INVALID KEY PATH TAKEN"    IX2134.2
052300            TO RE-MARK.                                           IX2134.2
052400     PERFORM FAIL.                                                IX2134.2
052500     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
052600     MOVE "QQQ040ALT08" TO CORRECT-A.                             IX2134.2
052700     GO TO READ-WRITE-F2-02.                                      IX2134.2
052800 READ-FAIL-F2-02.                                                 IX2134.2
052900     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
053000     PERFORM FAIL.                                                IX2134.2
053100     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
053200     MOVE "AAA040" TO CORRECT-A.                                  IX2134.2
053300 READ-WRITE-F2-02.                                                IX2134.2
053400     PERFORM PRINT-DETAIL.                                        IX2134.2
053500 DELETE-TEST-GF-01.                                               IX2134.2
053600     MOVE "DELETE...RECORD" TO FEATURE.                           IX2134.2
053700     MOVE "DELETE-TEST-GF-01" TO PAR-NAME.                        IX2134.2
053800     MOVE "DELETES RECORD FOUND IN READ-TEST-GF-02" TO RE-MARK.   IX2134.2
053900     DELETE IX-FS1 RECORD INVALID KEY GO TO DELETE-INVALID-GF-01. IX2134.2
054000     PERFORM PASS.                                                IX2134.2
054100     GO TO DELETE-WRITE-GF-01.                                    IX2134.2
054200 DELETE-DELETE-GF-01.                                             IX2134.2
054300     PERFORM DE-LETE.                                             IX2134.2
054400     GO TO DELETE-WRITE-GF-01.                                    IX2134.2
054500 DELETE-INVALID-GF-01.                                            IX2134.2
054600     MOVE "IX-21; 4.3.2 AND IX-6 1.3.5 INVALID KEY PATH TAKEN"    IX2134.2
054700            TO RE-MARK.                                           IX2134.2
054800     PERFORM FAIL.                                                IX2134.2
054900     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
055000     MOVE "AAA040" TO CORRECT-A.                                  IX2134.2
055100 DELETE-WRITE-GF-01.                                              IX2134.2
055200     PERFORM PRINT-DETAIL.                                        IX2134.2
055300 READ-TEST-F2-03.                                                 IX2134.2
055400     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
055500     MOVE "READ-TEST-F2-03"  TO PAR-NAME.                         IX2134.2
055600     MOVE "RETRIEVED BY ALTERNATE KEY 4" TO RE-MARK.              IX2134.2
055700     MOVE "III030ALT04" TO IX-FS1-ALT04.                          IX2134.2
055800     READ IX-FS1 RECORD                                           IX2134.2
055900                        KEY IS IX-FS1-ALT04                       IX2134.2
056000       INVALID KEY  GO TO READ-INVALID-F2-03.                     IX2134.2
056100     IF IX-FS1-KEY = "AAA030"                                     IX2134.2
056200        PERFORM PASS                                              IX2134.2
056300        ELSE GO TO READ-FAIL-F2-03.                               IX2134.2
056400     GO TO READ-WRITE-F2-03.                                      IX2134.2
056500 READ-DELETE-F2-03.                                               IX2134.2
056600     PERFORM DE-LETE.                                             IX2134.2
056700     GO TO READ-WRITE-F2-03.                                      IX2134.2
056800 READ-INVALID-F2-03.                                              IX2134.2
056900     MOVE "IX-28; 4.5.2 AND IX-6 1.3.5 INVALID KEY PATH TAKEN"    IX2134.2
057000            TO RE-MARK.                                           IX2134.2
057100     PERFORM FAIL.                                                IX2134.2
057200     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
057300     MOVE "III030ALT04" TO CORRECT-A.                             IX2134.2
057400     GO TO READ-WRITE-F2-03.                                      IX2134.2
057500 READ-FAIL-F2-03.                                                 IX2134.2
057600     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
057700     PERFORM FAIL.                                                IX2134.2
057800     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
057900     MOVE "AAA030" TO CORRECT-A.                                  IX2134.2
058000 READ-WRITE-F2-03.                                                IX2134.2
058100     PERFORM PRINT-DETAIL.                                        IX2134.2
058200 REWRITE-TEST-GF-01.                                              IX2134.2
058300     MOVE "REWRITE...INVALID..." TO FEATURE.                      IX2134.2
058400     MOVE "REWRITE-TEST-GF-01" TO PAR-NAME.                       IX2134.2
058500     MOVE "REWRITE-S RECORD FOUND IN READ-TEST-F2-03" TO RE-MARK. IX2134.2
058600     MOVE "IIIIIIALT04" TO IX-FS1-ALT04.                          IX2134.2
058700     REWRITE IX-FS1-RECORD INVALID KEY                            IX2134.2
058800              GO TO REWRITE-INVALID-GF-01.                        IX2134.2
058900     PERFORM PASS.                                                IX2134.2
059000     GO TO REWRITE-WRITE-GF-01.                                   IX2134.2
059100 REWRITE-DELETE-GF-01.                                            IX2134.2
059200     PERFORM DE-LETE.                                             IX2134.2
059300     GO TO REWRITE-WRITE-GF-01.                                   IX2134.2
059400 REWRITE-INVALID-GF-01.                                           IX2134.2
059500     MOVE "IX-33; 4.6.2 AND IX-6 1.3.5 INVALID KEY PATH TAKEN"    IX2134.2
059600            TO RE-MARK.                                           IX2134.2
059700     PERFORM FAIL.                                                IX2134.2
059800     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
059900     MOVE "IIIIIIALT04" TO CORRECT-A.                             IX2134.2
060000 REWRITE-WRITE-GF-01.                                             IX2134.2
060100     PERFORM PRINT-DETAIL.                                        IX2134.2
060200 READ-TEST-F2-04.                                                 IX2134.2
060300     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
060400     MOVE "READ-TEST-F2-04"  TO PAR-NAME.                         IX2134.2
060500     MOVE "INVALID KEY PATH SHOULD BE TAKEN" TO RE-MARK.          IX2134.2
060600     MOVE "ZZZ999ALT09" TO IX-FS1-ALT09.                          IX2134.2
060700     READ IX-FS1 RECORD                                           IX2134.2
060800                        KEY IS IX-FS1-ALT09                       IX2134.2
060900       INVALID KEY  PERFORM PASS                                  IX2134.2
061000        GO TO READ-WRITE-F2-04.                                   IX2134.2
061100     GO TO READ-FAIL-F2-04.                                       IX2134.2
061200 READ-DELETE-F2-04.                                               IX2134.2
061300     PERFORM DE-LETE.                                             IX2134.2
061400     GO TO READ-WRITE-F2-04.                                      IX2134.2
061500 READ-FAIL-F2-04.                                                 IX2134.2
061600     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY SHOULD BE TAKEN" IX2134.2
061700            TO RE-MARK.                                           IX2134.2
061800     PERFORM FAIL.                                                IX2134.2
061900     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
062000     MOVE "INVALID KEY" TO CORRECT-A.                             IX2134.2
062100 READ-WRITE-F2-04.                                                IX2134.2
062200     PERFORM PRINT-DETAIL.                                        IX2134.2
062300 START-TEST-GF-01.                                                IX2134.2
062400     MOVE "START...KEY IS EQUAL"    TO FEATURE.                   IX2134.2
062500     MOVE "START-TEST-GF-01" TO PAR-NAME.                         IX2134.2
062600     MOVE "RETRIEVED BY ALTERNATE KEY 3" TO RE-MARK.              IX2134.2
062700     MOVE "GGG020ALT03" TO IX-FS1-ALT03.                          IX2134.2
062800     START IX-FS1                                                 IX2134.2
062900                  KEY IS EQUAL TO IX-FS1-ALT03                    IX2134.2
063000       INVALID KEY  GO TO START-INVALID-GF-01.                    IX2134.2
063100     READ IX-FS1 NEXT RECORD AT END GO TO START-END-GF-01.        IX2134.2
063200     IF IX-FS1-KEY = "AAA020"                                     IX2134.2
063300        PERFORM PASS                                              IX2134.2
063400        ELSE GO TO START-FAIL-GF-01.                              IX2134.2
063500     GO TO START-WRITE-GF-01.                                     IX2134.2
063600 START-DELETE-GF-01.                                              IX2134.2
063700     PERFORM DE-LETE.                                             IX2134.2
063800     GO TO START-WRITE-GF-01.                                     IX2134.2
063900 START-INVALID-GF-01.                                             IX2134.2
064000     MOVE "IX-36; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
064100            TO RE-MARK.                                           IX2134.2
064200     PERFORM FAIL.                                                IX2134.2
064300     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
064400     MOVE "GGG020ALT03" TO CORRECT-A.                             IX2134.2
064500     GO TO START-WRITE-GF-01.                                     IX2134.2
064600 START-END-GF-01.                                                 IX2134.2
064700     MOVE "IX-28; 4.5.2 F1, READ AT END NOT EXPECTED" TO RE-MARK. IX2134.2
064800     PERFORM FAIL.                                                IX2134.2
064900     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2134.2
065000     MOVE "AAA020" TO CORRECT-A.                                  IX2134.2
065100     GO TO START-WRITE-GF-01.                                     IX2134.2
065200 START-FAIL-GF-01.                                                IX2134.2
065300     PERFORM FAIL.                                                IX2134.2
065400     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
065500     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
065600     MOVE "AAA020" TO CORRECT-A.                                  IX2134.2
065700 START-WRITE-GF-01.                                               IX2134.2
065800     PERFORM PRINT-DETAIL.                                        IX2134.2
065900 START-TEST-GF-02.                                                IX2134.2
066000     MOVE "START...KEY >" TO FEATURE.                             IX2134.2
066100     MOVE "START-TEST-GF-02" TO PAR-NAME.                         IX2134.2
066200     MOVE "RETRIEVED BY ALTERNATE KEY 6" TO RE-MARK.              IX2134.2
066300     MOVE "MMM090ALT06" TO IX-FS1-ALT06.                          IX2134.2
066400     START IX-FS1                                                 IX2134.2
066500                  KEY > IX-FS1-ALT06                              IX2134.2
066600       INVALID KEY  GO TO START-INVALID-GF-02.                    IX2134.2
066700     READ IX-FS1 NEXT RECORD AT END GO TO START-END-GF-02.        IX2134.2
066800     IF IX-FS1-KEY = "AAA092"                                     IX2134.2
066900        PERFORM PASS                                              IX2134.2
067000        ELSE GO TO START-INVALID-GF-02.                           IX2134.2
067100     GO TO START-WRITE-GF-02.                                     IX2134.2
067200 START-DELETE-GF-02.                                              IX2134.2
067300     PERFORM DE-LETE.                                             IX2134.2
067400     GO TO START-WRITE-GF-02.                                     IX2134.2
067500 START-INVALID-GF-02.                                             IX2134.2
067600     MOVE "IX-36; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
067700            TO RE-MARK.                                           IX2134.2
067800     PERFORM FAIL.                                                IX2134.2
067900     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
068000     MOVE "MMM090ALT09" TO CORRECT-A.                             IX2134.2
068100     GO TO START-WRITE-GF-02.                                     IX2134.2
068200 START-END-GF-02.                                                 IX2134.2
068300     MOVE "IX-28; 4.5.2 F1, READ AT END NOT EXPECTED" TO RE-MARK. IX2134.2
068400     PERFORM FAIL.                                                IX2134.2
068500     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2134.2
068600     MOVE "AAA092" TO CORRECT-A.                                  IX2134.2
068700     GO TO START-WRITE-GF-02.                                     IX2134.2
068800 START-FAIL-GF-02.                                                IX2134.2
068900     MOVE "IX-28; 4.5.2 F1, WRONG KEY            " TO RE-MARK.    IX2134.2
069000     PERFORM FAIL.                                                IX2134.2
069100     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
069200     MOVE "AAA092" TO CORRECT-A.                                  IX2134.2
069300 START-WRITE-GF-02.                                               IX2134.2
069400     PERFORM PRINT-DETAIL.                                        IX2134.2
069500 START-TEST-GF-03.                                                IX2134.2
069600     MOVE "START...KEY >" TO FEATURE.                             IX2134.2
069700     MOVE "START-TEST-GF-03" TO PAR-NAME.                         IX2134.2
069800     MOVE "RETRIEVED BY ALTERNATE KEY 10" TO RE-MARK.             IX2134.2
069900     MOVE "UUU080ALT10" TO IX-FS1-ALT10.                          IX2134.2
070000     START IX-FS1                                                 IX2134.2
070100                  KEY > IX-FS1-ALT10                              IX2134.2
070200       INVALID KEY  GO TO START-INVALID-GF-03.                    IX2134.2
070300     READ IX-FS1 NEXT RECORD AT END GO TO START-END-GF-03.        IX2134.2
070400     IF IX-FS1-KEY = "AAA082"                                     IX2134.2
070500        PERFORM PASS                                              IX2134.2
070600        ELSE GO TO START-FAIL-GF-03.                              IX2134.2
070700     GO TO START-WRITE-GF-03.                                     IX2134.2
070800 START-DELETE-GF-03.                                              IX2134.2
070900     PERFORM DE-LETE.                                             IX2134.2
071000     GO TO START-WRITE-GF-03.                                     IX2134.2
071100 START-INVALID-GF-03.                                             IX2134.2
071200     MOVE "IX-36; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
071300            TO RE-MARK.                                           IX2134.2
071400     PERFORM FAIL.                                                IX2134.2
071500     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
071600     MOVE "UUU080ALT10" TO CORRECT-A.                             IX2134.2
071700     GO TO START-WRITE-GF-03.                                     IX2134.2
071800 START-END-GF-03.                                                 IX2134.2
071900     MOVE "IX-28; 4.5.2 F1, READ AT END NOT EXPECTED" TO RE-MARK. IX2134.2
072000     PERFORM FAIL.                                                IX2134.2
072100     MOVE "FILE IS AT END" TO COMPUTED-A.                         IX2134.2
072200     MOVE "AAA082" TO CORRECT-A.                                  IX2134.2
072300     GO TO START-WRITE-GF-03.                                     IX2134.2
072400 START-FAIL-GF-03.                                                IX2134.2
072500     MOVE "IX-28; 4.5.2 F1, WRONG KEY            " TO RE-MARK.    IX2134.2
072600     PERFORM FAIL.                                                IX2134.2
072700     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
072800     MOVE "AAA082" TO CORRECT-A.                                  IX2134.2
072900 START-WRITE-GF-03.                                               IX2134.2
073000     PERFORM PRINT-DETAIL.                                        IX2134.2
073100 READ-TEST-F2-05.                                                 IX2134.2
073200     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
073300     MOVE "READ-TEST-F2-05"  TO PAR-NAME.                         IX2134.2
073400     MOVE "RETRIEVED BY ALTERNATE KEY 8" TO RE-MARK.              IX2134.2
073500     MOVE "QQQ040ALT08" TO IX-FS1-ALT08.                          IX2134.2
073600     READ IX-FS1 RECORD                                           IX2134.2
073700                        KEY IS IX-FS1-ALT08                       IX2134.2
073800       INVALID KEY  GO TO READ-INVALID-F2-05.                     IX2134.2
073900     IF IX-FS1-KEY = "AAA041"                                     IX2134.2
074000        PERFORM PASS                                              IX2134.2
074100        ELSE GO TO READ-FAIL-F2-05.                               IX2134.2
074200     GO TO READ-WRITE-F2-05.                                      IX2134.2
074300 READ-DELETE-F2-05.                                               IX2134.2
074400     PERFORM DE-LETE.                                             IX2134.2
074500     GO TO READ-WRITE-F2-05.                                      IX2134.2
074600 READ-INVALID-F2-05.                                              IX2134.2
074700     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
074800            TO RE-MARK.                                           IX2134.2
074900     PERFORM FAIL.                                                IX2134.2
075000     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
075100     MOVE "QQQ040ALT08" TO CORRECT-A.                             IX2134.2
075200     GO TO READ-WRITE-F2-05.                                      IX2134.2
075300 READ-FAIL-F2-05.                                                 IX2134.2
075400     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
075500     PERFORM FAIL.                                                IX2134.2
075600     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
075700     MOVE "AAA041" TO CORRECT-A.                                  IX2134.2
075800 READ-WRITE-F2-05.                                                IX2134.2
075900     PERFORM PRINT-DETAIL.                                        IX2134.2
076000 READ-TEST-F2-06.                                                 IX2134.2
076100     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
076200     MOVE "READ-TEST-F2-06" TO PAR-NAME.                          IX2134.2
076300     MOVE "RETRIEVED BY ALTERNATE KEY 4" TO RE-MARK.              IX2134.2
076400     MOVE "IIIIIIALT04" TO IX-FS1-ALT04.                          IX2134.2
076500     READ IX-FS1 RECORD                                           IX2134.2
076600                        KEY IS IX-FS1-ALT04                       IX2134.2
076700       INVALID KEY  GO TO READ-INVALID-F2-06.                     IX2134.2
076800     IF IX-FS1-KEY = "AAA030"                                     IX2134.2
076900        PERFORM PASS                                              IX2134.2
077000        ELSE GO TO READ-FAIL-F2-06.                               IX2134.2
077100     GO TO READ-WRITE-F2-06.                                      IX2134.2
077200 READ-DELETE-F2-06.                                               IX2134.2
077300     PERFORM DE-LETE.                                             IX2134.2
077400     GO TO READ-WRITE-F2-06.                                      IX2134.2
077500 READ-INVALID-F2-06.                                              IX2134.2
077600     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
077700            TO RE-MARK.                                           IX2134.2
077800     PERFORM FAIL.                                                IX2134.2
077900     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
078000     MOVE "IIIIIIALT04" TO CORRECT-A.                             IX2134.2
078100     GO TO READ-WRITE-F2-06.                                      IX2134.2
078200 READ-FAIL-F2-06.                                                 IX2134.2
078300     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
078400     PERFORM FAIL.                                                IX2134.2
078500     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
078600     MOVE "AAA030" TO CORRECT-A.                                  IX2134.2
078700 READ-WRITE-F2-06.                                                IX2134.2
078800     PERFORM PRINT-DETAIL.                                        IX2134.2
078900 READ-TEST-F2-07.                                                 IX2134.2
079000     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
079100     MOVE "READ-TEST-F2-07"  TO PAR-NAME.                         IX2134.2
079200     MOVE "RETRIEVED BY PRIMARY KEY" TO RE-MARK.                  IX2134.2
079300     MOVE "AAA011" TO IX-FS1-KEY.                                 IX2134.2
079400     READ IX-FS1 RECORD                                           IX2134.2
079500                        KEY IS IX-FS1-KEY                         IX2134.2
079600       INVALID KEY  GO TO READ-INVALID-F2-07.                     IX2134.2
079700     IF IX-FS1-KEY = "AAA011"                                     IX2134.2
079800        PERFORM PASS                                              IX2134.2
079900        ELSE GO TO READ-FAIL-F2-07.                               IX2134.2
080000     GO TO READ-WRITE-F2-07.                                      IX2134.2
080100 READ-DELETE-F2-07.                                               IX2134.2
080200     PERFORM DE-LETE.                                             IX2134.2
080300     GO TO READ-WRITE-F2-07.                                      IX2134.2
080400 READ-INVALID-F2-07.                                              IX2134.2
080500     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
080600            TO RE-MARK.                                           IX2134.2
080700     PERFORM FAIL.                                                IX2134.2
080800     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
080900     MOVE "AAA011" TO CORRECT-A.                                  IX2134.2
081000     GO TO READ-WRITE-F2-07.                                      IX2134.2
081100 READ-FAIL-F2-07.                                                 IX2134.2
081200     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
081300     PERFORM FAIL.                                                IX2134.2
081400     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
081500     MOVE "AAA011" TO CORRECT-A.                                  IX2134.2
081600 READ-WRITE-F2-07.                                                IX2134.2
081700     PERFORM PRINT-DETAIL.                                        IX2134.2
081800 DELETE-TEST-GF-02.                                               IX2134.2
081900     MOVE "DELETE...RECORD" TO FEATURE.                           IX2134.2
082000     MOVE "DELETE-TEST-GF-02" TO PAR-NAME.                        IX2134.2
082100     MOVE "DELETES RECORD FOUND IN READ-TEST-F2-07" TO RE-MARK.   IX2134.2
082200     DELETE IX-FS1 RECORD INVALID KEY GO TO DELETE-FAIL-GF-02.    IX2134.2
082300     PERFORM PASS.                                                IX2134.2
082400     GO TO DELETE-WRITE-GF-02.                                    IX2134.2
082500 DELETE-DELETE-GF-02.                                             IX2134.2
082600     PERFORM DE-LETE.                                             IX2134.2
082700     GO TO DELETE-WRITE-GF-02.                                    IX2134.2
082800 DELETE-FAIL-GF-02.                                               IX2134.2
082900     MOVE "IX-21; 4.3.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
083000            TO RE-MARK.                                           IX2134.2
083100     PERFORM FAIL.                                                IX2134.2
083200     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
083300     MOVE "AAA011" TO CORRECT-A.                                  IX2134.2
083400 DELETE-WRITE-GF-02.                                              IX2134.2
083500     PERFORM PRINT-DETAIL.                                        IX2134.2
083600 READ-TEST-F2-08.                                                 IX2134.2
083700     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
083800     MOVE "READ-TEST-F2-08"  TO PAR-NAME.                         IX2134.2
083900     MOVE "RETRIEVED BY ALTERNATE KEY 9" TO RE-MARK.              IX2134.2
084000     MOVE "SSS060ALT09" TO IX-FS1-ALT09.                          IX2134.2
084100     READ IX-FS1 RECORD                                           IX2134.2
084200                        KEY IS IX-FS1-ALT09                       IX2134.2
084300       INVALID KEY  GO TO READ-INVALID-F2-08.                     IX2134.2
084400     IF IX-FS1-KEY = "AAA060"                                     IX2134.2
084500        PERFORM PASS                                              IX2134.2
084600        ELSE GO TO READ-FAIL-F2-08.                               IX2134.2
084700     GO TO READ-WRITE-F2-08.                                      IX2134.2
084800 READ-DELETE-F2-08.                                               IX2134.2
084900     PERFORM DE-LETE.                                             IX2134.2
085000     GO TO READ-WRITE-F2-08.                                      IX2134.2
085100 READ-INVALID-F2-08.                                              IX2134.2
085200     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
085300            TO RE-MARK.                                           IX2134.2
085400     PERFORM FAIL.                                                IX2134.2
085500     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
085600     MOVE "AAA060" TO CORRECT-A.                                  IX2134.2
085700     GO TO READ-WRITE-F2-08.                                      IX2134.2
085800 READ-FAIL-F2-08.                                                 IX2134.2
085900     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
086000     PERFORM FAIL.                                                IX2134.2
086100     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
086200     MOVE "AAA060" TO CORRECT-A.                                  IX2134.2
086300 READ-WRITE-F2-08.                                                IX2134.2
086400     PERFORM PRINT-DETAIL.                                        IX2134.2
086500 REWRITE-TEST-GF-02.                                              IX2134.2
086600     MOVE "REWRITE...INVALID..." TO FEATURE.                      IX2134.2
086700     MOVE "REWRITE-TEST-GF-02" TO PAR-NAME.                       IX2134.2
086800     MOVE "REWRITES RECORD FOUND IN READ-TEST-GF-08" TO RE-MARK.  IX2134.2
086900     MOVE "SSSSSSALT09" TO IX-FS1-ALT09.                          IX2134.2
087000     REWRITE IX-FS1-RECORD INVALID KEY GO TO REWRITE-FAIL-GF-02.  IX2134.2
087100     PERFORM PASS.                                                IX2134.2
087200     GO TO REWRITE-WRITE-GF-02.                                   IX2134.2
087300 REWRITE-DELETE-GF-02.                                            IX2134.2
087400     PERFORM DE-LETE.                                             IX2134.2
087500     GO TO REWRITE-WRITE-GF-02.                                   IX2134.2
087600 REWRITE-FAIL-GF-02.                                              IX2134.2
087700     PERFORM FAIL.                                                IX2134.2
087800     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
087900     MOVE "SSSSSSALT09" TO CORRECT-A.                             IX2134.2
088000 REWRITE-WRITE-GF-02.                                             IX2134.2
088100     PERFORM PRINT-DETAIL.                                        IX2134.2
088200 READ-TEST-F2-09.                                                 IX2134.2
088300     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
088400     MOVE "READ-TEST-F2-09"  TO PAR-NAME.                         IX2134.2
088500     MOVE "RETRIEVED BY ALTERNATE KEY 5" TO RE-MARK.              IX2134.2
088600     MOVE "KKK010ALT05" TO IX-FS1-ALT05.                          IX2134.2
088700     READ IX-FS1 RECORD                                           IX2134.2
088800                        KEY IS IX-FS1-ALT05                       IX2134.2
088900       INVALID KEY  GO TO READ-INVALID-F2-09.                     IX2134.2
089000     IF IX-FS1-KEY = "AAA010"                                     IX2134.2
089100        PERFORM PASS                                              IX2134.2
089200        ELSE GO TO READ-FAIL-F2-09.                               IX2134.2
089300     GO TO READ-WRITE-F2-09.                                      IX2134.2
089400 READ-DELETE-F2-09.                                               IX2134.2
089500     PERFORM DE-LETE.                                             IX2134.2
089600     GO TO READ-WRITE-F2-09.                                      IX2134.2
089700 READ-INVALID-F2-09.                                              IX2134.2
089800     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
089900            TO RE-MARK.                                           IX2134.2
090000     PERFORM FAIL.                                                IX2134.2
090100     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
090200     MOVE "KKK010ALT05" TO CORRECT-A.                             IX2134.2
090300     GO TO READ-WRITE-F2-09.                                      IX2134.2
090400 READ-FAIL-F2-09.                                                 IX2134.2
090500     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
090600     PERFORM FAIL.                                                IX2134.2
090700     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
090800     MOVE "AAA010" TO CORRECT-A.                                  IX2134.2
090900 READ-WRITE-F2-09.                                                IX2134.2
091000     PERFORM PRINT-DETAIL.                                        IX2134.2
091100 READ-TEST-F2-10.                                                 IX2134.2
091200     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
091300     MOVE "READ-TEST-F2-10" TO PAR-NAME.                          IX2134.2
091400     MOVE "RETRIEVED BY ALTERNATE KEY 9" TO RE-MARK.              IX2134.2
091500     MOVE "SSS060ALT09" TO IX-FS1-ALT09.                          IX2134.2
091600     READ IX-FS1 RECORD                                           IX2134.2
091700                        KEY IS IX-FS1-ALT09                       IX2134.2
091800       INVALID KEY  GO TO READ-INVALID-F2-10.                     IX2134.2
091900     IF IX-FS1-KEY = "AAA061"                                     IX2134.2
092000        PERFORM PASS                                              IX2134.2
092100        ELSE GO TO READ-FAIL-F2-10.                               IX2134.2
092200     GO TO READ-WRITE-F2-10.                                      IX2134.2
092300 READ-DELETE-F2-10.                                               IX2134.2
092400     PERFORM DE-LETE.                                             IX2134.2
092500     GO TO READ-WRITE-F2-10.                                      IX2134.2
092600 READ-INVALID-F2-10.                                              IX2134.2
092700     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
092800            TO RE-MARK.                                           IX2134.2
092900     PERFORM FAIL.                                                IX2134.2
093000     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
093100     MOVE "SSS060ALT09" TO CORRECT-A.                             IX2134.2
093200     GO TO READ-WRITE-F2-10.                                      IX2134.2
093300 READ-FAIL-F2-10.                                                 IX2134.2
093400     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
093500     PERFORM FAIL.                                                IX2134.2
093600     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
093700     MOVE "AAA061" TO CORRECT-A.                                  IX2134.2
093800 READ-WRITE-F2-10.                                                IX2134.2
093900     PERFORM PRINT-DETAIL.                                        IX2134.2
094000 READ-TEST-F2-11.                                                 IX2134.2
094100     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
094200     MOVE "READ-TEST-F2-11"   TO PAR-NAME.                        IX2134.2
094300     MOVE "RETRIEVED BY ALTERNATE KEY 7" TO RE-MARK.              IX2134.2
094400     MOVE "OOO070ALT07"  TO IX-FS1-ALT07.                         IX2134.2
094500     READ IX-FS1 RECORD                                           IX2134.2
094600                        KEY IS IX-FS1-ALT07                       IX2134.2
094700       INVALID KEY  GO TO READ-INVALID-F2-11.                     IX2134.2
094800     IF IX-FS1-KEY = "AAA070"                                     IX2134.2
094900        PERFORM PASS                                              IX2134.2
095000        ELSE GO TO READ-FAIL-F2-11.                               IX2134.2
095100     GO TO READ-WRITE-F2-11.                                      IX2134.2
095200 READ-DELETE-F2-11.                                               IX2134.2
095300     PERFORM DE-LETE.                                             IX2134.2
095400     GO TO READ-WRITE-F2-11.                                      IX2134.2
095500 READ-INVALID-F2-11.                                              IX2134.2
095600     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
095700            TO RE-MARK.                                           IX2134.2
095800     PERFORM FAIL.                                                IX2134.2
095900     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
096000     MOVE "OOO070ALT07" TO CORRECT-A.                             IX2134.2
096100     GO TO READ-WRITE-F2-11.                                      IX2134.2
096200 READ-FAIL-F2-11.                                                 IX2134.2
096300     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
096400     PERFORM FAIL.                                                IX2134.2
096500     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
096600     MOVE "AAA070" TO CORRECT-A.                                  IX2134.2
096700 READ-WRITE-F2-11.                                                IX2134.2
096800     PERFORM PRINT-DETAIL.                                        IX2134.2
096900 DELETE-TEST-GF-03.                                               IX2134.2
097000     MOVE "DELETE...RECORD" TO FEATURE.                           IX2134.2
097100     MOVE "DELETE-TEST-GF-03" TO PAR-NAME.                        IX2134.2
097200     MOVE "DELETES RECORD FOUND IN READ-TEST-F2-11" TO RE-MARK.   IX2134.2
097300     DELETE IX-FS1 RECORD INVALID KEY GO TO DELETE-FAIL-GF-03.    IX2134.2
097400     PERFORM PASS.                                                IX2134.2
097500     GO TO DELETE-WRITE-GF-03.                                    IX2134.2
097600 DELETE-DELETE-GF-03.                                             IX2134.2
097700     PERFORM DE-LETE.                                             IX2134.2
097800     GO TO DELETE-WRITE-GF-03.                                    IX2134.2
097900 DELETE-FAIL-GF-03.                                               IX2134.2
098000     MOVE "IX-21; 4.3.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
098100            TO RE-MARK.                                           IX2134.2
098200     PERFORM FAIL.                                                IX2134.2
098300     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
098400     MOVE "AAA070" TO CORRECT-A.                                  IX2134.2
098500 DELETE-WRITE-GF-03.                                              IX2134.2
098600     PERFORM PRINT-DETAIL.                                        IX2134.2
098700 READ-TEST-F2-12.                                                 IX2134.2
098800     MOVE "READ...RECORD KEY" TO FEATURE.                         IX2134.2
098900     MOVE "READ-TEST-F2-12" TO PAR-NAME.                          IX2134.2
099000     MOVE "RETRIEVED BY ALTERNATE KEY 5" TO RE-MARK.              IX2134.2
099100     MOVE "KKK070ALT05" TO IX-FS1-ALT05.                          IX2134.2
099200     READ IX-FS1 RECORD                                           IX2134.2
099300                        KEY IS IX-FS1-ALT05                       IX2134.2
099400       INVALID KEY  GO TO READ-INVALID-F2-12.                     IX2134.2
099500     IF IX-FS1-KEY = "AAA071"                                     IX2134.2
099600        PERFORM PASS                                              IX2134.2
099700        ELSE GO TO READ-FAIL-F2-12.                               IX2134.2
099800     GO TO READ-WRITE-F2-12.                                      IX2134.2
099900 READ-DELETE-F2-12.                                               IX2134.2
100000     PERFORM DE-LETE.                                             IX2134.2
100100     GO TO READ-WRITE-F2-12.                                      IX2134.2
100200 READ-INVALID-F2-12.                                              IX2134.2
100300     MOVE "IX-28; 4.5.2 & IX-6 1.3.5 INVALID KEY PATH      TAKEN" IX2134.2
100400            TO RE-MARK.                                           IX2134.2
100500     PERFORM FAIL.                                                IX2134.2
100600     MOVE "INVALID KEY" TO COMPUTED-A.                            IX2134.2
100700     MOVE "KKK070ALT05" TO CORRECT-A.                             IX2134.2
100800     GO TO READ-WRITE-F2-12.                                      IX2134.2
100900 READ-FAIL-F2-12.                                                 IX2134.2
101000     MOVE "IX-28; 4.5.2 F2, WRONG KEY            " TO RE-MARK.    IX2134.2
101100     PERFORM FAIL.                                                IX2134.2
101200     MOVE IX-FS1-KEY TO COMPUTED-A.                               IX2134.2
101300     MOVE "AAA071" TO CORRECT-A.                                  IX2134.2
101400 READ-WRITE-F2-12.                                                IX2134.2
101500     PERFORM PRINT-DETAIL.                                        IX2134.2
101600     CLOSE IX-FS1.                                                IX2134.2
101700 CCVS-EXIT SECTION.                                               IX2134.2
101800 CCVS-999999.                                                     IX2134.2
101900     GO TO CLOSE-FILES.                                           IX2134.2
      *END-OF,IX213A                                                                  
