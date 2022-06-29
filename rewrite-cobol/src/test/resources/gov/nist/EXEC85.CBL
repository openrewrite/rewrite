CCVS85*  VERSION 4.0   01 OCT 1992 0032                                          
000100 IDENTIFICATION DIVISION.                                         EXEC84.2
000200                                                                  EXEC84.2
000400 PROGRAM-ID.                                                      EXEC84.2
000500     EXEC85.                                                      EXEC84.2
000600 INSTALLATION.                                                    EXEC84.2
000700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".EXEC84.2
000800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".EXEC84.2
000900 ENVIRONMENT DIVISION.                                            EXEC84.2
001000                                                                  EXEC84.2
001100****************************************************************  EXEC84.2
001200*                                                              *  EXEC84.2
001300*    VALIDATION FOR:-                                          *  EXEC84.2
001400*                                                              *  EXEC84.2
001500*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".EXEC84.2
001600*                                                              *  EXEC84.2
001700*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".EXEC84.2
001800*                                                              *  EXEC84.2
001900****************************************************************  EXEC84.2
002000 CONFIGURATION SECTION.                                           EXEC84.2
002100                                                                  EXEC84.2
002200 SPECIAL-NAMES.                                                   EXEC84.2
002300 INPUT-OUTPUT SECTION.                                            EXEC84.2
002400 FILE-CONTROL.                                                    EXEC84.2
002500     SELECT  OPTIONAL POPULATION-FILE                             EXEC84.2
002600     ASSIGN TO                                                    EXEC84.2
002700     XXXXX001.                                                    EXEC84.2
002800     SELECT  SOURCE-COBOL-PROGRAMS                                EXEC84.2
002900     ASSIGN TO                                                    EXEC84.2
003000     XXXXX002                                                     EXEC84.2
003100     ORGANIZATION SEQUENTIAL.                                     EXEC84.2
003200     SELECT  UPDATED-POPULATION-FILE                              EXEC84.2
003300     ASSIGN TO                                                    EXEC84.2
003400     XXXXX003.                                                    EXEC84.2
003500     SELECT  PRINT-FILE                                           EXEC84.2
003600     ASSIGN TO                                                    EXEC84.2
003700     XXXXX055.                                                    EXEC84.2
003800     SELECT  CONTROL-CARD-FILE                                    EXEC84.2
003900     ASSIGN TO                                                    EXEC84.2
004000     XXXXX058.                                                    EXEC84.2
004100 DATA DIVISION.                                                   EXEC84.2
004200 FILE SECTION.                                                    EXEC84.2
004300 FD  POPULATION-FILE.                                             EXEC84.2
004400*    RECORD CONTAINS 2400 CHARACTERS.                             EXEC84.2
004500 01  SOURCE-IN-2400.                                              EXEC84.2
004600     02 SOURCE-IN                    PIC X(80).                   EXEC84.2
004700*                                              OCCURS 30.         EXEC84.2
004800 FD  CONTROL-CARD-FILE.                                           EXEC84.2
004900 01  CONTROL-RECORD                  PIC X(80).                   EXEC84.2
005000 FD  PRINT-FILE.                                                  EXEC84.2
005100 01  PRINT-REC.                                                   EXEC84.2
005200   05        FILLER                  PIC X.                       EXEC84.2
005300   05        PRINT-DATA              PIC X(131).                  EXEC84.2
005400 FD  SOURCE-COBOL-PROGRAMS                                        EXEC84.2
005500     BLOCK CONTAINS 1 RECORDS.                                    EXEC84.2
005600 01  CT-OUT.                                                      EXEC84.2
005700     02 FILLER PIC X(72).                                         EXEC84.2
005800     02 FILLER PIC X(8).                                          EXEC84.2
005900 FD  UPDATED-POPULATION-FILE                                      EXEC84.2
006000     RECORD CONTAINS 2400 CHARACTERS.                             EXEC84.2
006100 01  UPDATED-SOURCE-OUT-2400.                                     EXEC84.2
006200     02 UD-SOURCE-OUT                PIC X(80)  OCCURS 30.        EXEC84.2
006300                                                                  EXEC84.2
006400 WORKING-STORAGE SECTION.                                         EXEC84.2
006500                                                                  EXEC84.2
006600 01  FILLER                          PIC X(40)  VALUE             EXEC84.2
006700            "NEWEXEC WORKING-STORAGE STARTS HERE ==->".           EXEC84.2
006800 01  BLOCK-TYPE                      PIC X(5).                    EXEC84.2
006900 01  SUB1                            PIC S9(3)  COMP.             EXEC84.2
007000 01  SUB2                            PIC S9(3)  COMP.             EXEC84.2
007100 01  SUB3                            PIC S9(3)  COMP.             EXEC84.2
007200 01  SUB4                            PIC S9(3)  COMP.             EXEC84.2
007300 01  SUB5                            PIC S9(3)  COMP.             EXEC84.2
007400 01  SUB6                            PIC S9(3)  COMP.             EXEC84.2
007500 01  SUB7                            PIC S9(3)  COMP.             EXEC84.2
007600 01  WA-ERR-IND                      PIC 9 VALUE ZEROES.          EXEC84.2
007700 01  WA-FIRST-IND                    PIC 9 VALUE ZEROES.          EXEC84.2
007800 01  WA-ZCARD-TABLE.                                              EXEC84.2
007900   05        WA-ZCARD                OCCURS 10                    EXEC84.2
008000                                     PIC X(60).                   EXEC84.2
008100 01  WA-TOP-OF-PAGE-LINE.                                         EXEC84.2
008200   05        FILLER                  PIC X(4)   VALUE SPACES.     EXEC84.2
008300   05        WA-VERSION.                                          EXEC84.2
008400     07      WA-VERSION-TEXT         PIC X(22)  VALUE             EXEC84.2
008500            "CCVS85 VERSION NUMBER ".                             EXEC84.2
008600     07      WA-VERSION-NUM          PIC X(3) VALUE SPACES.       EXEC84.2
008700   05        WA-RELEASE.                                          EXEC84.2
008800     07      WA-RELEASE-TEXT         PIC X(14)  VALUE             EXEC84.2
008900            ", RELEASED ON ".                                     EXEC84.2
009000     07      WA-VERSION-DATE         PIC X(11) VALUE SPACES.      EXEC84.2
009100   05        FILLER                  PIC X(4)   VALUE SPACES.     EXEC84.2
009200   05        WA-COMPANY-AND-COMPILER PIC X(30) VALUE SPACES.      EXEC84.2
009300   05        FILLER                  PIC X(5)   VALUE SPACES.     EXEC84.2
009400   05        WA-DATE                 PIC XXBXXBXX.                EXEC84.2
009500   05        FILLER                  PIC X(4)   VALUE SPACES.     EXEC84.2
009600   05        FILLER                  PIC X(5)   VALUE "PAGE ".    EXEC84.2
009700   05        WA-PAGE-CT              PIC Z(5)9.                   EXEC84.2
009800                                                                  EXEC84.2
009900 01  WA-ACCT-LINE-1.                                              EXEC84.2
010000   05        FILLER                  PIC X(19)  VALUE             EXEC84.2
010100            " ** END OF PROGRAM ".                                EXEC84.2
010200   05        WA-CURRENT-PROG         PIC X(6).                    EXEC84.2
010300   05        FILLER                  PIC X(32)  VALUE             EXEC84.2
010400            " FOUND,  COBOL LINES PROCESSED: ".                   EXEC84.2
010500   05        WA-LINES-COBOL          PIC Z(5)9.                   EXEC84.2
010600 01  WA-ACCT-LINE-2.                                              EXEC84.2
010700   05        FILLER                  PIC X(19)  VALUE             EXEC84.2
010800            " ** LINES INSERTED ".                                EXEC84.2
010900   05        WA-LINES-INSERTED       PIC Z(5)9.                   EXEC84.2
011000   05        FILLER                  PIC X(19)  VALUE             EXEC84.2
011100            " ** LINES REPLACED ".                                EXEC84.2
011200   05        WA-LINES-REPLACED       PIC Z(5)9.                   EXEC84.2
011300   05        FILLER                  PIC X(19)  VALUE             EXEC84.2
011400            " ** LINES DELETED  ".                                EXEC84.2
011500   05        WA-LINES-DELETED        PIC Z(5)9.                   EXEC84.2
011600 01  WA-ACCT-LINE-3.                                              EXEC84.2
011700   05        FILLER                  PIC X(18)  VALUE             EXEC84.2
011800            " ** OPTIONAL CODE ".                                 EXEC84.2
011900   05        WA-OPTIONAL-CODE        PIC X(8).                    EXEC84.2
012000   05        WA-CODE-REMOVED         PIC Z(5)9.                   EXEC84.2
012100   05        WA-CODE-KILLED          PIC X(21)  VALUE             EXEC84.2
012200            " ** COMMENTS DELETED ".                              EXEC84.2
012300   05        WA-COMMENTS-DEL         PIC Z(5)9.                   EXEC84.2
012400 01  WA-FINAL-LINE-1.                                             EXEC84.2
012500   05        FILLER                  PIC X(34)  VALUE             EXEC84.2
012600            " ** END OF POPULATION FILE REACHED".                 EXEC84.2
012700   05        FILLER                  PIC X(27)  VALUE             EXEC84.2
012800            " NUMBER OF PROGRAMS FOUND: ".                        EXEC84.2
012900   05        WA-PROGS-FOUND          PIC Z(5)9.                   EXEC84.2
013000 01  WA-FINAL-LINE-2.                                             EXEC84.2
013100   05        FILLER                  PIC X(47)  VALUE             EXEC84.2
013200            " ** NUMBER OF PROGRAMS WRITTEN TO SOURCE FILE: ".    EXEC84.2
013300   05        WA-SOURCE-PROGS         PIC Z(5)9.                   EXEC84.2
013400 01  WA-FINAL-LINE-3.                                             EXEC84.2
013500   05        FILLER                  PIC X(48)  VALUE             EXEC84.2
013600            " ** NUMBER OF PROGRAMS WRITTEN TO NEW POPULATION".   EXEC84.2
013700   05        FILLER                  PIC X(7)   VALUE " FILE: ".  EXEC84.2
013800   05        WA-NEWPOP-PROGS         PIC Z(5)9.                   EXEC84.2
013900 01  WB-CONTROL-DATA.                                             EXEC84.2
014000   05        WB-FILL                 PIC X(80).                   EXEC84.2
014100   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
014200     10      WB-3                    PIC X(3).                    EXEC84.2
014300     10      FILLER                  PIC X(77).                   EXEC84.2
014400   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
014500     10      WB-4                    PIC X(4).                    EXEC84.2
014600     10      WB-NN                   PIC 99.                      EXEC84.2
014700     10      FILLER                  PIC X.                       EXEC84.2
014800     10      WB-X                    PIC X.                       EXEC84.2
014900     10      FILLER                  PIC X(72).                   EXEC84.2
015000   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
015100     10      WB-6                    PIC X(6).                    EXEC84.2
015200     10      FILLER                  PIC X(74).                   EXEC84.2
015300   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
015400     10      WB-7                    PIC X(7).                    EXEC84.2
015500     10      FILLER                  PIC X(73).                   EXEC84.2
015600   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
015700     10      WB-8                    PIC X(8).                    EXEC84.2
015800     10      FILLER                  PIC X(72).                   EXEC84.2
015900   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
016000     10      WB-9                    PIC X(9).                    EXEC84.2
016100     10      FILLER                  PIC X(71).                   EXEC84.2
016200   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
016300     10      WB-10                   PIC X(10).                   EXEC84.2
016400     10      FILLER                  PIC X(70).                   EXEC84.2
016500   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
016600     10      WB-11                   PIC X(11).                   EXEC84.2
016700     10      FILLER                  PIC X(69).                   EXEC84.2
016800   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
016900     10      WB-12                   PIC X(12).                   EXEC84.2
017000     10      FILLER                  PIC X.                       EXEC84.2
017100     10      WB-PROG                 PIC X(5).                    EXEC84.2
017200     10      FILLER                  PIC X(62).                   EXEC84.2
017300   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
017400     10      WB-13                   PIC X(13).                   EXEC84.2
017500     10      FILLER                  PIC X(67).                   EXEC84.2
017600   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
017700     10      WB-14                   PIC X(14).                   EXEC84.2
017800     10      FILLER                  PIC X.                       EXEC84.2
017900     10      WB-MODULE               PIC XX.                      EXEC84.2
018000     10      FILLER                  PIC X.                       EXEC84.2
018100     10      WB-LEVEL                PIC X.                       EXEC84.2
018200     10      FILLER                  PIC X(61).                   EXEC84.2
018300   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
018400     10      WB-15                   PIC X(15).                   EXEC84.2
018500     10      FILLER                  PIC X(65).                   EXEC84.2
018600   05        FILLER                  REDEFINES  WB-FILL.          EXEC84.2
018700     10      WB-16                   PIC X(16).                   EXEC84.2
018800     10      FILLER                  PIC X(64).                   EXEC84.2
018900   05        WB-X-CARD               REDEFINES  WB-FILL.          EXEC84.2
019000     10      WB-X-HYPHEN             PIC XX.                      EXEC84.2
019100     10      WB-X-CARD-NUM           PIC 9(3).                    EXEC84.2
019200     10      WB-PROG-POS.                                         EXEC84.2
019300       15    WB-PROG-POS-NUM         PIC 99.                      EXEC84.2
019400     10      FILLER                  PIC X.                       EXEC84.2
019500     10      WB-SUBS-TEXT            PIC X(60).                   EXEC84.2
019600     10      FILLER                  PIC X(12).                   EXEC84.2
019700   05        WB-START-CARD           REDEFINES  WB-FILL.          EXEC84.2
019800     10      WB-STAR-START           PIC X(6).                    EXEC84.2
019900     10      FILLER                  PIC X.                       EXEC84.2
020000     10      WB-UPDATE-PROG          PIC X(6).                    EXEC84.2
020100     10      FILLER                  PIC X.                       EXEC84.2
020200     10      WB-RENUMBER             PIC X.                       EXEC84.2
020300     10      FILLER                  PIC X(65).                   EXEC84.2
020400   05        WB-LINE-UPDATE          REDEFINES  WB-FILL.          EXEC84.2
020500     10      WB-SEQ-1                PIC X(6).                    EXEC84.2
020600     10      WB-COBOL-LINE           PIC X(74).                   EXEC84.2
020700     10      FILLER                  REDEFINES  WB-COBOL-LINE.    EXEC84.2
020800       15    WB-COL-7                PIC X.                       EXEC84.2
020900       15    FILLER                  PIC X(73).                   EXEC84.2
021000     10      FILLER                  REDEFINES  WB-COBOL-LINE.    EXEC84.2
021100       15    WB-CHAR                 PIC X.                       EXEC84.2
021200       15    WB-SEQ-2                PIC X(6).                    EXEC84.2
021300/                                                                 EXEC84.2
021400 01  WC-CURRENT-POP-RECORD.                                       EXEC84.2
021500   05        WC-1.                                                EXEC84.2
021600     10      WC-END-OF-POPFILE       PIC X(16).                   EXEC84.2
021700     10      FILLER                  PIC X(64).                   EXEC84.2
021800   05        WC-HEADER               REDEFINES WC-1.              EXEC84.2
021900     10      WC-STAR-HEADER          PIC X(7).                    EXEC84.2
022000     10      FILLER                  PIC X.                       EXEC84.2
022100     10      WC-COBOL                PIC X(5).                    EXEC84.2
022200     10      FILLER                  PIC X.                       EXEC84.2
022300     10      WC-PROG-ID.                                          EXEC84.2
022400      12     WC-PROG-ID-1-5.                                      EXEC84.2
022500       15    WC-PROG-ID-1-4.                                      EXEC84.2
022600        18   WC-MODULE               PIC XX.                      EXEC84.2
022700        18   WC-LEVEL                PIC X.                       EXEC84.2
022800        18   FILLER                  PIC X.                       EXEC84.2
022900       15    FILLER                  PIC X.                       EXEC84.2
023000      12     WC-PROG-ID-6            PIC X.                       EXEC84.2
023100     10      FILLER                  PIC X.                       EXEC84.2
023200     10      WC-SUBPRG               PIC X(6).                    EXEC84.2
023300     10      FILLER                  PIC X.                       EXEC84.2
023400     10      WC-PROG2ID.                                          EXEC84.2
023500      12     WC-PROG2ID-1-5          PIC X(5).                    EXEC84.2
023600      12     FILLER                  PIC X.                       EXEC84.2
023700     10      FILLER                  PIC X(46).                   EXEC84.2
023800   05        FILLER                  REDEFINES WC-1.              EXEC84.2
023900     10      WC-1-72.                                             EXEC84.2
024000       15    WC-6.                                                EXEC84.2
024100         20  WC-STAR                 PIC X.                       EXEC84.2
024200         20  FILLER                  PIC X(5).                    EXEC84.2
024300       15    FILLER                  REDEFINES  WC-6.             EXEC84.2
024400         20  WC-1-5                  PIC X(5).                    EXEC84.2
024500         20  FILLER                  PIC X.                       EXEC84.2
024600       15    WC-COL-7                PIC X.                       EXEC84.2
024700       15    WC-COL-8                PIC X.                       EXEC84.2
024800       15    FILLER                  PIC X(3).                    EXEC84.2
024900       15    WC-SUB-DATA.                                         EXEC84.2
025000         20  WC-12-15                PIC X(4).                    EXEC84.2
025100         20  FILLER                  PIC X.                       EXEC84.2
025200         20  WC-17-19                PIC 9(3).                    EXEC84.2
025300         20  WC-20                   PIC X.                       EXEC84.2
025400         20  FILLER                  PIC X(52).                   EXEC84.2
025500     10      WC-73-80                PIC X(8).                    EXEC84.2
025600                                                                  EXEC84.2
025700 01  WD-SOURCE-REC.                                               EXEC84.2
025800   05        WD-1.                                                EXEC84.2
025900     10      FILLER                  PIC X(6).                    EXEC84.2
026000     10      WD-HEADER               PIC X(74).                   EXEC84.2
026100                                                                  EXEC84.2
026200 01  WE-PRINT-DATA.                                               EXEC84.2
026300   05        WE-COBOL-LINE           PIC X(80).                   EXEC84.2
026400   05        FILLER                  PIC X      VALUE SPACE.      EXEC84.2
026500   05        WE-X-CARD               PIC X(9).                    EXEC84.2
026600   05        FILLER                  PIC XX     VALUE SPACES.     EXEC84.2
026700   05        WE-CHANGE-TYPE          PIC X(12).                   EXEC84.2
026800                                                                  EXEC84.2
026900 01  WF-PROGRAM-SELECTED-TABLE.                                   EXEC84.2
027000   05        WF-PROGRAM-SELECTED     PIC X(5)   OCCURS 50.        EXEC84.2
027100                                                                  EXEC84.2
027200 01  WG-MODULE-SELECTED-TABLE.                                    EXEC84.2
027300   05        FILLER                             OCCURS 10.        EXEC84.2
027400     10      WG-MODULE-SELECTED      PIC XX.                      EXEC84.2
027500     10      WG-MODULE-LEVEL         PIC X.                       EXEC84.2
027600                                                                  EXEC84.2
027700 01  WV-PRINT-MISCELLANEOUS.                                      EXEC84.2
027800   05        WV-OPTION-HEADING       PIC X(25)  VALUE             EXEC84.2
027900            " OPTION SWITCH SETTINGS -".                          EXEC84.2
028000   05        WV-OPT-1                PIC X(40)  VALUE             EXEC84.2
028100         " 0                 1                   2".              EXEC84.2
028200   05        WV-OPT-2                PIC X(52)  VALUE             EXEC84.2
028300         " 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6".  EXEC84.2
028400   05        WV-OPT-SWITCHES.                                     EXEC84.2
028500     10      FILLER                  PIC X      VALUE SPACE.      EXEC84.2
028600     10      FILLER                  OCCURS 26.                   EXEC84.2
028700       15    WV-OPT                  PIC X.                       EXEC84.2
028800       15    FILLER                  PIC X.                       EXEC84.2
028900 01  WX-X-CARD-TABLE.                                             EXEC84.2
029000   05        WX-X-CARD               OCCURS 200.                  EXEC84.2
029100     10      WX-X-CHAR               PIC X                        EXEC84.2
029200                                     OCCURS 60.                   EXEC84.2
029300 01  WX-PROG-POS-TABLE.                                           EXEC84.2
029400   05        WX-PROG-POS             OCCURS 200                   EXEC84.2
029500                                     PIC 99.                      EXEC84.2
029600 01  WY-SWITCHES.                                                 EXEC84.2
029700   05        WY-OPTION-SWITCHES.                                  EXEC84.2
029800     10      WY-OPT-SW-1             PIC X.                       EXEC84.2
029900     10      WY-OPT-SW-2             PIC X.                       EXEC84.2
030000     10      WY-OPT-SW-3             PIC X.                       EXEC84.2
030100     10      WY-OPT-SW-4             PIC X.                       EXEC84.2
030200     10      WY-OPT-SW-5             PIC X.                       EXEC84.2
030300     10      WY-OPT-SW-6             PIC X.                       EXEC84.2
030400     10      WY-OPT-SW-7             PIC X.                       EXEC84.2
030500     10      WY-OPT-SW-8             PIC X.                       EXEC84.2
030600     10      WY-OPT-SW-9             PIC X.                       EXEC84.2
030700     10      WY-OPT-SW-10            PIC X.                       EXEC84.2
030800     10      WY-OPT-SW-11            PIC X.                       EXEC84.2
030900     10      WY-OPT-SW-12            PIC X.                       EXEC84.2
031000     10      WY-OPT-SW-13            PIC X.                       EXEC84.2
031100     10      WY-OPT-SW-14            PIC X.                       EXEC84.2
031200     10      WY-OPT-SW-15            PIC X.                       EXEC84.2
031300     10      WY-OPT-SW-16            PIC X.                       EXEC84.2
031400     10      WY-OPT-SW-17            PIC X.                       EXEC84.2
031500     10      WY-OPT-SW-18            PIC X.                       EXEC84.2
031600     10      WY-OPT-SW-19            PIC X.                       EXEC84.2
031700     10      WY-OPT-SW-20            PIC X.                       EXEC84.2
031800     10      WY-OPT-SW-21            PIC X.                       EXEC84.2
031900     10      WY-OPT-SW-22            PIC X.                       EXEC84.2
032000     10      WY-OPT-SW-23            PIC X.                       EXEC84.2
032100     10      WY-OPT-SW-24            PIC X.                       EXEC84.2
032200     10      WY-OPT-SW-25            PIC X.                       EXEC84.2
032300     10      WY-OPT-SW-26            PIC X.                       EXEC84.2
032400   05        FILLER                  REDEFINES WY-OPTION-SWITCHES.EXEC84.2
032500     10      WY-OPT-SW               PIC X                        EXEC84.2
032600                                     OCCURS 26.                   EXEC84.2
032700   05        WY-PRINT-SWITCHES.                                   EXEC84.2
032800     10      WY-EXTRACT-ALL          PIC X.                       EXEC84.2
032900     10      WY-EXTRACT-AUTO         PIC X.                       EXEC84.2
033000     10      WY-EXTRACT-MAN          PIC X.                       EXEC84.2
033100     10      WY-KILL-DELETIONS       PIC X.                       EXEC84.2
033200     10      WY-LIST-NO-UPDATES      PIC X.                       EXEC84.2
033300     10      WY-LIST-X-CARDS         PIC X.                       EXEC84.2
033400     10      WY-LIST-PROGRAMS        PIC X.                       EXEC84.2
033500     10      WY-LIST-COMPACT         PIC X.                       EXEC84.2
033600     10      WY-NO-DATA              PIC X.                       EXEC84.2
033700     10      WY-NO-LIBRARY           PIC X.                       EXEC84.2
033800     10      WY-NO-SOURCE            PIC X.                       EXEC84.2
033900     10      WY-REMOVE-COMMENTS      PIC X.                       EXEC84.2
034000     10      WY-NEW-POP              PIC X.                       EXEC84.2
034100     10      WY-SELECT-PROG          PIC X.                       EXEC84.2
034200     10      WY-SELECT-MODULE        PIC X.                       EXEC84.2
034300     10      WY-SELECT-LEVEL         PIC X.                       EXEC84.2
034400                                                                  EXEC84.2
034500 01  WZ-MISCELLANEOUS.                                            EXEC84.2
034600   05        WZ-PROGRAM-SELECTED     PIC X.                       EXEC84.2
034700   05        WZ-END-OF-POPFILE       PIC X.                       EXEC84.2
034800   05        WZ-FULL-STOP            PIC X.                       EXEC84.2
034900   05        WZ-DONT-READ-POPFILE    PIC X.                       EXEC84.2
035000   05        WZ-UPDATE-THIS-PROG     PIC X.                       EXEC84.2
035100   05        WZ-REPLACE-FLAG         PIC X.                       EXEC84.2
035200   05        WZ-LINE-UPDATE          PIC X.                       EXEC84.2
035300   05        WZ-RESEQUENCE-THIS      PIC X.                       EXEC84.2
035400   05        WZ-RESEQUENCE-NEXT      PIC X.                       EXEC84.2
035500   05        WZ-END-OF-UPDATES       PIC X.                       EXEC84.2
035600   05        WZ-OPTIONAL-SELECTED    PIC X.                       EXEC84.2
035700   05        WZ-DELETE-FLAG          PIC X.                       EXEC84.2
035800   05        WZ-NOT-THIS-COMMENT     PIC X.                       EXEC84.2
035900   05        WZ-CURRENT-HEADER       PIC X(5).                    EXEC84.2
036000   05        WZ-INVALID-DATA.                                     EXEC84.2
036100     10      FILLER                  PIC X(20).                   EXEC84.2
036200     10      WZ-ERROR-MESSAGE        PIC X(60).                   EXEC84.2
036300   05        WZ-CURRENT-UPD-PROG.                                 EXEC84.2
036400     10      WZ-UPD-PROG-CHAR        PIC X.                       EXEC84.2
036500     10      FILLER                  PIC X(5).                    EXEC84.2
036600   05        WZ-CURRENT-MAIN-PROG.                                EXEC84.2
036700     10      WZ-MAIN-PROG-CHAR       PIC X      OCCURS 6.         EXEC84.2
036800   05        WZ-PROG-BREAK.                                       EXEC84.2
036900     10      WZ-1CHAR                PIC X      OCCURS 6.         EXEC84.2
037000   05        WZ-CURRENT-POP-PROG.                                 EXEC84.2
037100     10      FILLER                  PIC X(5).                    EXEC84.2
037200     10      WZ-PROG-ID-6            PIC X.                       EXEC84.2
037300   05        WZ-MAIN-PROG-FLAG       PIC X.                       EXEC84.2
037400   05        WZ-LINES-COBOL          PIC 9(6).                    EXEC84.2
037500   05        WZ-LINES-INSERTED       PIC 9(6).                    EXEC84.2
037600   05        WZ-LINES-REPLACED       PIC 9(6).                    EXEC84.2
037700   05        WZ-LINES-DELETED        PIC 9(6).                    EXEC84.2
037800   05        WZ-COMMENTS-DELETED     PIC 9(6).                    EXEC84.2
037900   05        WZ-CODE-REMOVED         PIC 9(6).                    EXEC84.2
038000   05        WZ-SOURCE-PROGS         PIC 9(6).                    EXEC84.2
038100   05        WZ-NEWPOP-PROGS         PIC 9(6).                    EXEC84.2
038200   05        WZ-PROGS-FOUND          PIC 9(6).                    EXEC84.2
038300   05        WZ-COMMENTS-DEL         PIC 9(6).                    EXEC84.2
038400   05        WZ-SEQ-NO               PIC 9(6).                    EXEC84.2
038500   05        WZ-SAVE-POP-RECORD.                                  EXEC84.2
038600     10      WZ-SAVE-SEQ             PIC X(6).                    EXEC84.2
038700     10      FILLER                  PIC X(5).                    EXEC84.2
038800     10      WZ-SAVE-12-20.                                       EXEC84.2
038900       15    WZ-SAVE-12-15           PIC X(4).                    EXEC84.2
039000       15    FILLER                  PIC X(5).                    EXEC84.2
039100     10      FILLER                  PIC X(60).                   EXEC84.2
039200   05        WZ-PAGE-CT              PIC 9(6).                    EXEC84.2
039300   05        WZ-LINE-CT              PIC 9(6).                    EXEC84.2
039400   05        WZ-MODULE               PIC XX.                      EXEC84.2
039500   05        WZ-LEVEL                PIC X.                       EXEC84.2
039600   05        WZ-PRINT-HOLD           PIC X(132).                  EXEC84.2
039700   05        WZ-X-CARD.                                           EXEC84.2
039800     10      WZ-X-CHAR               PIC X                        EXEC84.2
039900                                     OCCURS 60.                   EXEC84.2
040000   05        WZ-WITHIN-DELETE-SERIES-FLAG  PIC X.                 EXEC84.2
040100 01  WZ-VERSION-CARD.                                             EXEC84.2
040200     10  FILLER                      PIC X(55) VALUE              EXEC84.2
040300     "CCVS85  VERSION 4.2   01 OCT 1992 0032                 ".   EXEC84.2
040400 01  WZ-VERSION-CONTROL REDEFINES WZ-VERSION-CARD.                EXEC84.2
040500     10      FILLER                  PIC X(16).                   EXEC84.2
040600     10      WZ-VERSION-NUM          PIC X(3).                    EXEC84.2
040700     10      FILLER                  PIC X(3).                    EXEC84.2
040800     10      WZ-VERSION-DATE         PIC X(11).                   EXEC84.2
040900                                                                  EXEC84.2
041000/                                                                 EXEC84.2
041100 PROCEDURE DIVISION.                                              EXEC84.2
041200*==================                                               EXEC84.2
041300*                                                                 EXEC84.2
041400 A10-MAIN SECTION.                                                EXEC84.2
041500*================                                                 EXEC84.2
041600*                                                                 EXEC84.2
041700****************************************************************  EXEC84.2
041800*    THIS IS THE HIGHEST LEVEL CONTROL MODULE                  *  EXEC84.2
041900*                                                              *  EXEC84.2
042000****************************************************************  EXEC84.2
042100 A10-1-MAIN.                                                      EXEC84.2
042200     PERFORM B10-INITIALISE.                                      EXEC84.2
042300                                                                  EXEC84.2
042400     PERFORM C10-PROCESS-MONITOR.                                 EXEC84.2
042500                                                                  EXEC84.2
042600     PERFORM D10-MERGE-UPDATE-CARDS.                              EXEC84.2
042700                                                                  EXEC84.2
042800     PERFORM E10-TERMINATE.                                       EXEC84.2
042900                                                                  EXEC84.2
043000 A10-EXIT.                                                        EXEC84.2
043100     EXIT.                                                        EXEC84.2
043200                                                                  EXEC84.2
043300/                                                                 EXEC84.2
043400 B10-INITIALISE SECTION.                                          EXEC84.2
043500*======================                                           EXEC84.2
043600*                                                                 EXEC84.2
043700****************************************************************  EXEC84.2
043800* THIS SECTION INITIALIZES THE OPTION SWITCH AND X-CARD FIELDS *  EXEC84.2
043900* PRIOR TO READING IN CONTROL CARD FILE.                       *  EXEC84.2
044000*                                                              *  EXEC84.2
044100*                                                              *  EXEC84.2
044200*                                                              *  EXEC84.2
044300*                                                              *  EXEC84.2
044400****************************************************************  EXEC84.2
044500 B10-1-INIT-OPTION-SWITCHES.                                      EXEC84.2
044600     MOVE    SPACES  TO WZ-MISCELLANEOUS.                         EXEC84.2
044700     MOVE    SPACES  TO WF-PROGRAM-SELECTED-TABLE.                EXEC84.2
044800     MOVE    SPACES  TO WG-MODULE-SELECTED-TABLE.                 EXEC84.2
044900     MOVE    SPACES  TO WY-SWITCHES.                              EXEC84.2
045000     MOVE    "A"     TO WY-OPT-SW-1.                              EXEC84.2
045100     MOVE    "E"     TO WY-OPT-SW-2.                              EXEC84.2
045200     MOVE    "H"     TO WY-OPT-SW-3.                              EXEC84.2
045300     MOVE    "L"     TO WY-OPT-SW-4.                              EXEC84.2
045400     MOVE    "Y"     TO WY-OPT-SW-7.                              EXEC84.2
045500     MOVE    "T"     TO WY-OPT-SW-11.                             EXEC84.2
045600                                                                  EXEC84.2
045700 B10-2-INIT-X-CARDS.                                              EXEC84.2
045800     MOVE    ZERO TO SUB1.                                        EXEC84.2
045900     MOVE    ZERO TO SUB6.                                        EXEC84.2
046000     MOVE    ZERO TO SUB7.                                        EXEC84.2
046100     MOVE    1    TO SUB5.                                        EXEC84.2
046200     PERFORM B20-INIT-X-CARDS 200 TIMES.                          EXEC84.2
046300     MOVE   "    OMITTED" TO WX-X-CARD (84).                      EXEC84.2
046400     MOVE    ZERO TO WZ-LINES-COBOL.                              EXEC84.2
046500     MOVE    ZERO TO WZ-LINES-INSERTED.                           EXEC84.2
046600     MOVE    ZERO TO WZ-LINES-REPLACED.                           EXEC84.2
046700     MOVE    ZERO TO WZ-LINES-DELETED.                            EXEC84.2
046800     MOVE    ZERO TO WZ-COMMENTS-DELETED.                         EXEC84.2
046900     MOVE    ZERO TO WZ-CODE-REMOVED.                             EXEC84.2
047000     MOVE    ZERO TO WZ-SOURCE-PROGS.                             EXEC84.2
047100     MOVE    ZERO TO WZ-NEWPOP-PROGS.                             EXEC84.2
047200     MOVE    ZERO TO WZ-PROGS-FOUND.                              EXEC84.2
047300     MOVE    ZERO TO WZ-COMMENTS-DEL.                             EXEC84.2
047400     MOVE    ZERO TO WZ-SEQ-NO.                                   EXEC84.2
047500     MOVE    ZERO TO WZ-PAGE-CT.                                  EXEC84.2
047600     MOVE    ZERO TO WZ-LINE-CT.                                  EXEC84.2
047700     ACCEPT WA-DATE FROM DATE.                                    EXEC84.2
047800 B10-EXIT.                                                        EXEC84.2
047900     EXIT.                                                        EXEC84.2
048000                                                                  EXEC84.2
048100                                                                  EXEC84.2
048200                                                                  EXEC84.2
048300                                                                  EXEC84.2
048400 B20-INIT-X-CARDS SECTION.                                        EXEC84.2
048500*========================                                         EXEC84.2
048600 B20-1-INIT.                                                      EXEC84.2
048700     ADD     1 TO SUB1.                                           EXEC84.2
048800     MOVE   "**** X-CARD UNDEFINED ****" TO WX-X-CARD (SUB1).     EXEC84.2
048900     MOVE    ZERO TO WX-PROG-POS (SUB1).                          EXEC84.2
049000                                                                  EXEC84.2
049100 B20-EXIT.                                                        EXEC84.2
049200     EXIT.                                                        EXEC84.2
049300/                                                                 EXEC84.2
049400 C10-PROCESS-MONITOR SECTION.                                     EXEC84.2
049500*===========================                                      EXEC84.2
049600                                                                  EXEC84.2
049700****************************************************************  EXEC84.2
049800*    THIS SECTION PROCESSES THE RECORDS COMMENCING WITH "*"    *  EXEC84.2
049900*    AND "X-" (THE MONITOR PART OF THE INPUT FILE ) AND READS  *  EXEC84.2
050000*    THE FIRST "*START" UPDATE RECORD.                         *  EXEC84.2
050100*                                                              *  EXEC84.2
050200*    PERFORMED BY    A10-MAIN                                  *  EXEC84.2
050300*    PERFORMS        C20-PROCESS-STAR-CARDS                    *  EXEC84.2
050400*                    C30-CHECK-COMBINATIONS                    *  EXEC84.2
050500*                    C40-PROCESS-X-CARDS                       *  EXEC84.2
050600****************************************************************  EXEC84.2
050700 C10-1-OPEN-FILES.                                                EXEC84.2
050800     OPEN    OUTPUT  PRINT-FILE.                                  EXEC84.2
050900     MOVE    SPACES TO PRINT-REC.                                 EXEC84.2
051000     OPEN    INPUT   CONTROL-CARD-FILE.                           EXEC84.2
051100     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
051200             AT END  MOVE "CONTROL-CARD-FILE IS EMPTY"            EXEC84.2
051300                  TO PRINT-DATA                                   EXEC84.2
051400             PERFORM  X20-PRINT-DETAIL                            EXEC84.2
051500             STOP RUN.                                            EXEC84.2
051600     PERFORM C20-PROCESS-STAR-CARDS                               EXEC84.2
051700             UNTIL   WB-X-HYPHEN = "X-".                          EXEC84.2
051800     PERFORM C30-CHECK-COMBINATIONS.                              EXEC84.2
051900     PERFORM C40-PROCESS-X-CARDS                                  EXEC84.2
052000             UNTIL   WB-12 = "*END-MONITOR".                      EXEC84.2
052100                                                                  EXEC84.2
052200     PERFORM C50-PRINT-OPTIONS.                                   EXEC84.2
052300                                                                  EXEC84.2
052400 C10-10-GET-FIRST-START-CARD.                                     EXEC84.2
052500     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
052600             AT END  MOVE "NO BEGIN-UPDATE CARD FOUND"            EXEC84.2
052700                  TO PRINT-DATA                                   EXEC84.2
052800             PERFORM  X20-PRINT-DETAIL                            EXEC84.2
052900             STOP RUN.                                            EXEC84.2
053000     IF WB-13 NOT = "*BEGIN-UPDATE"                               EXEC84.2
053100             MOVE "*BEGIN-UPDATE CARD MISSING"                    EXEC84.2
053200                  TO PRINT-DATA                                   EXEC84.2
053300             PERFORM X20-PRINT-DETAIL                             EXEC84.2
053400             STOP RUN.                                            EXEC84.2
053500     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
053600             AT END  MOVE "NO END-UPDATE CARD FOUND"              EXEC84.2
053700                  TO PRINT-DATA                                   EXEC84.2
053800             PERFORM  X20-PRINT-DETAIL                            EXEC84.2
053900             STOP RUN.                                            EXEC84.2
054000     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
054100             MOVE   "Y" TO WZ-END-OF-UPDATES                      EXEC84.2
054200             GO TO   C10-EXIT.                                    EXEC84.2
054300     IF      WB-STAR-START = "*START"                             EXEC84.2
054400             PERFORM C60-START-CARD.                              EXEC84.2
054500                                                                  EXEC84.2
054600 C10-EXIT.                                                        EXEC84.2
054700     EXIT.                                                        EXEC84.2
054800/                                                                 EXEC84.2
054900 C20-PROCESS-STAR-CARDS SECTION.                                  EXEC84.2
055000*==============================                                   EXEC84.2
055100*                                                                 EXEC84.2
055200****************************************************************  EXEC84.2
055300*    THIS SECTION PROCESSES THE INPUT PARAMETER FILE RECORDS   *  EXEC84.2
055400*    WHICH START WITH AN ASTERISK IN COLUMN 1 AND SETS VARIOUS *  EXEC84.2
055500*    FLAGS WHICH CONTROL THE WAY THIS EXECUTIVE ROUTINE WORKS. *  EXEC84.2
055600*                                                              *  EXEC84.2
055700*    PERFORMED BY  C10-PROCESS-MONITOR                         *  EXEC84.2
055800*    PERFORMS      C25-SET-FLAGS                               *  EXEC84.2
055900****************************************************************  EXEC84.2
056000 C20-1-UPDATE-CHECK.                                              EXEC84.2
056100     IF      WB-13 = "*BEGIN-UPDATE"                              EXEC84.2
056200             MOVE    WB-13 TO WZ-INVALID-DATA                     EXEC84.2
056300             MOVE   "ENCOUNTERED BEFORE *END-MONITOR CARD"        EXEC84.2
056400                  TO WZ-ERROR-MESSAGE                             EXEC84.2
056500             MOVE    WZ-INVALID-DATA TO PRINT-DATA                EXEC84.2
056600             PERFORM X20-PRINT-DETAIL                             EXEC84.2
056700             STOP RUN.                                            EXEC84.2
056800                                                                  EXEC84.2
056900     IF      WB-6  = "*START"                                     EXEC84.2
057000             MOVE    WB-6  TO WZ-INVALID-DATA                     EXEC84.2
057100             MOVE   "ENCOUNTERED BEFORE *END-MONITOR CARD"        EXEC84.2
057200                  TO WZ-ERROR-MESSAGE                             EXEC84.2
057300             MOVE    WZ-INVALID-DATA TO PRINT-DATA                EXEC84.2
057400             PERFORM X20-PRINT-DETAIL                             EXEC84.2
057500             STOP RUN.                                            EXEC84.2
057600                                                                  EXEC84.2
057700     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
057800             MOVE    WB-11 TO WZ-INVALID-DATA                     EXEC84.2
057900             MOVE   "ENCOUNTERED BEFORE *END-MONITOR CARD"        EXEC84.2
058000                  TO WZ-ERROR-MESSAGE                             EXEC84.2
058100             MOVE    WZ-INVALID-DATA TO PRINT-DATA                EXEC84.2
058200             PERFORM X20-PRINT-DETAIL                             EXEC84.2
058300             STOP RUN.                                            EXEC84.2
058400                                                                  EXEC84.2
058500     PERFORM C25-SET-FLAGS.                                       EXEC84.2
058600                                                                  EXEC84.2
058700 C20-EXIT.                                                        EXEC84.2
058800     EXIT.                                                        EXEC84.2
058900                                                                  EXEC84.2
059000/                                                                 EXEC84.2
059100 C25-SET-FLAGS SECTION.                                           EXEC84.2
059200*=====================                                            EXEC84.2
059300 C25-1.                                                           EXEC84.2
059400     MOVE    WB-CONTROL-DATA TO PRINT-DATA.                       EXEC84.2
059500     PERFORM X20-PRINT-DETAIL.                                    EXEC84.2
059600     IF      WB-12 = "*EXTRACT-ALL"                               EXEC84.2
059700             MOVE   "Y" TO WY-EXTRACT-ALL.                        EXEC84.2
059800     IF      WB-13 = "*EXTRACT-AUTO"                              EXEC84.2
059900             MOVE   "Y" TO WY-EXTRACT-AUTO.                       EXEC84.2
060000     IF      WB-12 = "*EXTRACT-MAN"                               EXEC84.2
060100             MOVE   "Y" TO WY-EXTRACT-MAN.                        EXEC84.2
060200     IF      WB-15 = "*KILL-DELETIONS"                            EXEC84.2
060300             MOVE   "Y" TO WY-KILL-DELETIONS.                     EXEC84.2
060400     IF      WB-16 = "*LIST NO-UPDATES"                           EXEC84.2
060500             MOVE   "Y" TO WY-LIST-NO-UPDATES.                    EXEC84.2
060600     IF      WB-13 = "*LIST X-CARDS"                              EXEC84.2
060700             MOVE   "Y" TO WY-LIST-X-CARDS.                       EXEC84.2
060800     IF      WB-14 = "*LIST PROGRAMS"                             EXEC84.2
060900             MOVE   "Y" TO WY-LIST-PROGRAMS.                      EXEC84.2
061000     IF      WB-13 = "*LIST COMPACT"                              EXEC84.2
061100             MOVE   "Y" TO WY-LIST-COMPACT.                       EXEC84.2
061200     IF      WB-8  = "*NO-DATA"                                   EXEC84.2
061300             MOVE   "Y" TO WY-NO-DATA.                            EXEC84.2
061400     IF      WB-11 = "*NO-LIBRARY"                                EXEC84.2
061500             MOVE   "Y" TO WY-NO-LIBRARY.                         EXEC84.2
061600     IF      WB-10 = "*NO-SOURCE"                                 EXEC84.2
061700             MOVE   "Y" TO WY-NO-SOURCE.                          EXEC84.2
061800     IF      WB-16 = "*REMOVE-COMMENTS"                           EXEC84.2
061900             MOVE   "Y" TO WY-REMOVE-COMMENTS.                    EXEC84.2
062000     IF      WB-8  = "*NEW-POP"                                   EXEC84.2
062100             MOVE   "Y" TO WY-NEW-POP.                            EXEC84.2
062200     IF      WB-4  = "*OPT"                                       EXEC84.2
062300             MOVE   WB-X TO WY-OPT-SW (WB-NN).                    EXEC84.2
062400     IF      WB-14 = "*SELECT-MODULE"                             EXEC84.2
062500             IF      WB-MODULE = SPACE                            EXEC84.2
062600                     MOVE  "SELECTED MODULE NOT SPECIFIED"        EXEC84.2
062700                         TO PRINT-DATA                            EXEC84.2
062800                     PERFORM X20-PRINT-DETAIL                     EXEC84.2
062900                     STOP RUN.                                    EXEC84.2
063000                                                                  EXEC84.2
063100     IF      WB-14 = "*SELECT-MODULE"                             EXEC84.2
063200             ADD     1 TO SUB6                                    EXEC84.2
063300             IF      SUB6 > 10                                    EXEC84.2
063400                     MOVE   "MORE THAN 10 MODULES SELECTED"       EXEC84.2
063500                          TO PRINT-DATA                           EXEC84.2
063600                     PERFORM X20-PRINT-DETAIL                     EXEC84.2
063700                     STOP RUN                                     EXEC84.2
063800             ELSE                                                 EXEC84.2
063900                     MOVE   "Y" TO WY-SELECT-MODULE               EXEC84.2
064000                     MOVE    WB-MODULE                            EXEC84.2
064100                          TO WG-MODULE-SELECTED (SUB6)            EXEC84.2
064200                     MOVE    WB-LEVEL  TO WG-MODULE-LEVEL (SUB6). EXEC84.2
064300     IF      WB-12 = "*SELECT-PROG"                               EXEC84.2
064400             ADD     1 TO SUB7                                    EXEC84.2
064500             IF      SUB7 > 50                                    EXEC84.2
064600                     MOVE   "MORE THAN 50 RECORDS SELECTED"       EXEC84.2
064700                          TO PRINT-DATA                           EXEC84.2
064800                     PERFORM X20-PRINT-DETAIL                     EXEC84.2
064900                     STOP RUN                                     EXEC84.2
065000             ELSE                                                 EXEC84.2
065100                     MOVE   "Y" TO WY-SELECT-PROG                 EXEC84.2
065200                     MOVE    WB-PROG                              EXEC84.2
065300                          TO WF-PROGRAM-SELECTED (SUB7).          EXEC84.2
065400                                                                  EXEC84.2
065500 C25-10-READ-FILE.                                                EXEC84.2
065600     MOVE    SPACES TO WB-CONTROL-DATA.                           EXEC84.2
065700     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
065800             AT END  MOVE "*END-MONITOR NOT ENCOUNTERED"          EXEC84.2
065900                  TO PRINT-DATA                                   EXEC84.2
066000             PERFORM X20-PRINT-DETAIL                             EXEC84.2
066100             STOP RUN.                                            EXEC84.2
066200 C25-EXIT.                                                        EXEC84.2
066300     EXIT.                                                        EXEC84.2
066400/                                                                 EXEC84.2
066500 C30-CHECK-COMBINATIONS SECTION.                                  EXEC84.2
066600*==============================                                   EXEC84.2
066700*                                                                 EXEC84.2
066800****************************************************************  EXEC84.2
066900*    THIS SECTION CHECKS FOR ANY INVALID COMBINATIONS OF       *  EXEC84.2
067000*    CARDS INPUT WITH AN ASTERISK IN COLUMN 1.                 *  EXEC84.2
067100*                                                              *  EXEC84.2
067200*    PERFORMED BY  C10-PROCESS-MONITOR                         *  EXEC84.2
067300*    PERFORMS      NONE                                        *  EXEC84.2
067400****************************************************************  EXEC84.2
067500 C30-1-SELECTION-CHECK.                                           EXEC84.2
067600     IF      WY-EXTRACT-ALL = SPACE                               EXEC84.2
067700             GO TO   C30-10.                                      EXEC84.2
067800     IF      WY-EXTRACT-AUTO = "Y"                                EXEC84.2
067900             MOVE   "ALL AND AUTO PROGRAMS SELECTED"              EXEC84.2
068000                  TO PRINT-DATA                                   EXEC84.2
068100             PERFORM X20-PRINT-DETAIL                             EXEC84.2
068200             STOP RUN.                                            EXEC84.2
068300                                                                  EXEC84.2
068400 C30-10.                                                          EXEC84.2
068500     IF      WY-EXTRACT-ALL = SPACE                               EXEC84.2
068600             GO TO   C30-20.                                      EXEC84.2
068700     IF      WY-EXTRACT-MAN = "Y"                                 EXEC84.2
068800             MOVE   "ALL AND MANUAL PROGRAMS SELECTED"            EXEC84.2
068900                  TO PRINT-DATA                                   EXEC84.2
069000             PERFORM X20-PRINT-DETAIL                             EXEC84.2
069100             STOP RUN.                                            EXEC84.2
069200                                                                  EXEC84.2
069300 C30-20.                                                          EXEC84.2
069400     IF      WY-LIST-PROGRAMS = SPACE                             EXEC84.2
069500             GO TO   C30-30.                                      EXEC84.2
069600     IF      WY-LIST-NO-UPDATES = SPACE                           EXEC84.2
069700             MOVE   "BOTH UPDATES AND PROGRAMS SELECTED"          EXEC84.2
069800                  TO PRINT-DATA                                   EXEC84.2
069900             PERFORM X20-PRINT-DETAIL                             EXEC84.2
070000             STOP RUN.                                            EXEC84.2
070100                                                                  EXEC84.2
070200 C30-30.                                                          EXEC84.2
070300     IF      WY-EXTRACT-AUTO = SPACE                              EXEC84.2
070400             GO TO   C30-40-CHECK-FOR-NEW-FILE.                   EXEC84.2
070500     IF      WY-EXTRACT-MAN  = "Y"                                EXEC84.2
070600             MOVE   "AUTO AND MANUAL PROGRAMS SELECTED"           EXEC84.2
070700                  TO PRINT-DATA                                   EXEC84.2
070800             PERFORM X20-PRINT-DETAIL                             EXEC84.2
070900             STOP RUN.                                            EXEC84.2
071000                                                                  EXEC84.2
071100 C30-40-CHECK-FOR-NEW-FILE.                                       EXEC84.2
071200     IF      WY-NO-SOURCE = SPACE                                 EXEC84.2
071300             GO TO   C30-50.                                      EXEC84.2
071400     IF      WY-NEW-POP   = "Y"                                   EXEC84.2
071500             GO TO   C30-50.                                      EXEC84.2
071600     MOVE   "NO SOURCE OR UPDATED POPFILE SELECTED" TO PRINT-DATA.EXEC84.2
071700     PERFORM X20-PRINT-DETAIL                                     EXEC84.2
071800     STOP RUN.                                                    EXEC84.2
071900                                                                  EXEC84.2
072000 C30-50.                                                          EXEC84.2
072100     IF      WY-EXTRACT-ALL = "Y"                                 EXEC84.2
072200             GO TO   C30-55.                                      EXEC84.2
072300     IF      WY-EXTRACT-MAN = "Y"                                 EXEC84.2
072400             GO TO   C30-55.                                      EXEC84.2
072500     IF      WY-EXTRACT-AUTO = SPACE                              EXEC84.2
072600             GO TO   C30-60.                                      EXEC84.2
072700 C30-55.                                                          EXEC84.2
072800     IF      WY-SELECT-PROG = "Y"                                 EXEC84.2
072900             MOVE  "SINGLE PROGRAM SELECTED WITH ALL/AUTO/MANUAL" EXEC84.2
073000                  TO PRINT-DATA                                   EXEC84.2
073100             PERFORM X20-PRINT-DETAIL                             EXEC84.2
073200             STOP RUN.                                            EXEC84.2
073300     IF      WY-SELECT-MODULE = "Y"                               EXEC84.2
073400             MOVE  "SINGLE MODULE SELECTED WITH ALL/AUTO/MANUAL"  EXEC84.2
073500                  TO PRINT-DATA                                   EXEC84.2
073600             PERFORM X20-PRINT-DETAIL                             EXEC84.2
073700             STOP RUN.                                            EXEC84.2
073800                                                                  EXEC84.2
073900 C30-60.                                                          EXEC84.2
074000     IF      WY-SELECT-PROG = SPACE                               EXEC84.2
074100             GO TO   C30-70.                                      EXEC84.2
074200     IF      WY-SELECT-MODULE = "Y"                               EXEC84.2
074300             MOVE  "SINGLE MODULE AND SINGLE PROGRAM SELECTED"    EXEC84.2
074400                  TO PRINT-DATA                                   EXEC84.2
074500             PERFORM X20-PRINT-DETAIL                             EXEC84.2
074600             STOP RUN.                                            EXEC84.2
074700                                                                  EXEC84.2
074800                                                                  EXEC84.2
074900 C30-70.                                                          EXEC84.2
075000     IF      WY-EXTRACT-ALL = SPACE                               EXEC84.2
075100        IF      WY-EXTRACT-AUTO = SPACE                           EXEC84.2
075200             IF      WY-EXTRACT-MAN = SPACE                       EXEC84.2
075300                 IF      WY-SELECT-PROG = SPACE                   EXEC84.2
075400                     IF      WY-SELECT-MODULE = SPACE             EXEC84.2
075500                             MOVE   "NO PROGRAMS SELECTED"        EXEC84.2
075600                                  TO PRINT-DATA                   EXEC84.2
075700                             PERFORM X20-PRINT-DETAIL             EXEC84.2
075800                             STOP RUN.                            EXEC84.2
075900                                                                  EXEC84.2
076000 C30-EXIT.                                                        EXEC84.2
076100     EXIT.                                                        EXEC84.2
076200/                                                                 EXEC84.2
076300 C40-PROCESS-X-CARDS SECTION.                                     EXEC84.2
076400*===========================                                      EXEC84.2
076500*                                                                 EXEC84.2
076600****************************************************************  EXEC84.2
076700*    THIS SECTION PROCESSES THE INPUT PARAMETER FILE RECORDS   *  EXEC84.2
076800*    WHICH START WITH AN "X" IN COLUMN 1 AND SETS A TABLE WHICH*  EXEC84.2
076900*    CONTAINS TEXT TO BE SUBSTITUTED BY THIS EXECUTIVE ROUTINE.*  EXEC84.2
077000*                                                              *  EXEC84.2
077100*    PERFORMED BY  C10-PROCESS-MONITOR                         *  EXEC84.2
077200*    PERFORMS      NONE                                        *  EXEC84.2
077300****************************************************************  EXEC84.2
077400 C40-1-PROCESS-CARD.                                              EXEC84.2
077500     IF      WB-X-HYPHEN NOT = "X-"                               EXEC84.2
077600             MOVE   "INVALID X-CARD:" TO WZ-INVALID-DATA          EXEC84.2
077700             MOVE    WB-CONTROL-DATA  TO WZ-ERROR-MESSAGE         EXEC84.2
077800             MOVE    WZ-INVALID-DATA TO PRINT-DATA                EXEC84.2
077900             PERFORM X20-PRINT-DETAIL                             EXEC84.2
078000             GO TO   C40-90-READ-FILE.                            EXEC84.2
078100                                                                  EXEC84.2
078200     IF      WB-X-CARD-NUM > 200                                  EXEC84.2
078300             MOVE   "INVALID X-CARD:" TO WZ-INVALID-DATA          EXEC84.2
078400             MOVE    WB-CONTROL-DATA  TO WZ-ERROR-MESSAGE         EXEC84.2
078500             MOVE    WZ-INVALID-DATA  TO PRINT-DATA               EXEC84.2
078600             PERFORM X20-PRINT-DETAIL                             EXEC84.2
078700             GO TO   C40-90-READ-FILE.                            EXEC84.2
078800                                                                  EXEC84.2
078900 C40-20-MOVE-DATA.                                                EXEC84.2
079000     MOVE    WB-SUBS-TEXT TO WX-X-CARD (WB-X-CARD-NUM).           EXEC84.2
079100     IF      WB-PROG-POS = SPACES                                 EXEC84.2
079200             GO TO   C40-30-MOVE-DATA.                            EXEC84.2
079300     IF      WB-PROG-POS-NUM < 55                                 EXEC84.2
079400             MOVE   WB-PROG-POS-NUM                               EXEC84.2
079500                 TO WX-PROG-POS (WB-X-CARD-NUM).                  EXEC84.2
079600                                                                  EXEC84.2
079700 C40-30-MOVE-DATA.                                                EXEC84.2
079800     MOVE    WB-CONTROL-DATA TO PRINT-DATA.                       EXEC84.2
079900     PERFORM X20-PRINT-DETAIL.                                    EXEC84.2
080000                                                                  EXEC84.2
080100 C40-90-READ-FILE.                                                EXEC84.2
080200     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
080300             AT END  MOVE                                         EXEC84.2
080400     "*BEGIN-UPDATE AND *END-UPDATE CARDS NOT ENCOUNTERED"        EXEC84.2
080500                  TO PRINT-DATA                                   EXEC84.2
080600             PERFORM  X20-PRINT-DETAIL                            EXEC84.2
080700             STOP RUN.                                            EXEC84.2
080800                                                                  EXEC84.2
080900 C40-EXIT.                                                        EXEC84.2
081000     EXIT.                                                        EXEC84.2
081100                                                                  EXEC84.2
081200/                                                                 EXEC84.2
081300 C50-PRINT-OPTIONS SECTION.                                       EXEC84.2
081400*=========================                                        EXEC84.2
081500*                                                                 EXEC84.2
081600****************************************************************  EXEC84.2
081700*    THIS SECTION PRINTS DETAILS OF THE PROCESSING OPTIONS     *  EXEC84.2
081800*    SELECTED BY THE VARIOUS CONTROL CARDS INPUT               *  EXEC84.2
081900*                                                              *  EXEC84.2
082000*    PERFORMED BY  C10-PROCESS-MONITOR                         *  EXEC84.2
082100*    PERFORMS      NONE                                        *  EXEC84.2
082200****************************************************************  EXEC84.2
082300 C50-1-PRINT-OPTION-SWITCHES.                                     EXEC84.2
082400     WRITE   PRINT-REC FROM WV-OPTION-HEADING AFTER PAGE.         EXEC84.2
082500     WRITE   PRINT-REC FROM WV-OPT-1          AFTER 1.            EXEC84.2
082600     WRITE   PRINT-REC FROM WV-OPT-2          AFTER 1.            EXEC84.2
082700     MOVE    SPACES TO WV-OPT-SWITCHES.                           EXEC84.2
082800     MOVE    ZERO   TO SUB1.                                      EXEC84.2
082900     PERFORM C50-4 26 TIMES.                                      EXEC84.2
083000     GO TO   C50-5.                                               EXEC84.2
083100 C50-4.                                                           EXEC84.2
083200*    PERFORM 26 TIMES                                             EXEC84.2
083300             ADD     1 TO SUB1                                    EXEC84.2
083400             MOVE    WY-OPT-SW (SUB1) TO WV-OPT (SUB1).           EXEC84.2
083500*    END-PERFORM.                                                 EXEC84.2
083600 C50-5.                                                           EXEC84.2
083700     WRITE   PRINT-REC FROM WV-OPT-SWITCHES AFTER 1.              EXEC84.2
083800                                                                  EXEC84.2
083900 C50-20-LISTING-OPTIONS.                                          EXEC84.2
084000     IF      WY-LIST-NO-UPDATES = "Y"                             EXEC84.2
084100             MOVE   "UPDATES WILL NOT BE REPORTED" TO PRINT-DATA  EXEC84.2
084200     ELSE                                                         EXEC84.2
084300             MOVE   "UPDATES WILL BE REPORTED"     TO PRINT-DATA. EXEC84.2
084400     WRITE   PRINT-REC AFTER 3.                                   EXEC84.2
084500                                                                  EXEC84.2
084600     IF      WY-LIST-X-CARDS = "Y"                                EXEC84.2
084700             MOVE   "X-CARD SUBSTITUTIONS WILL BE SHOWN"          EXEC84.2
084800                  TO PRINT-DATA                                   EXEC84.2
084900     ELSE                                                         EXEC84.2
085000             MOVE   "X-CARD SUBSTITUTIONS WILL NOT BE SHOWN"      EXEC84.2
085100                  TO PRINT-DATA.                                  EXEC84.2
085200     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
085300                                                                  EXEC84.2
085400     IF      WY-LIST-PROGRAMS = "Y"                               EXEC84.2
085500             MOVE   "PROGRAM LISTINGS WILL BE PRINTED"            EXEC84.2
085600                  TO PRINT-DATA                                   EXEC84.2
085700     ELSE                                                         EXEC84.2
085800             MOVE   "PROGRAM LISTINGS WILL NOT BE PRINTED"        EXEC84.2
085900                  TO PRINT-DATA.                                  EXEC84.2
086000     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
086100                                                                  EXEC84.2
086200     IF      WY-LIST-COMPACT = "Y"                                EXEC84.2
086300             MOVE   "REPORT WILL BE COMPRESSED" TO PRINT-DATA     EXEC84.2
086400     ELSE                                                         EXEC84.2
086500             MOVE   "REPORT WILL BE EXPANDED"   TO PRINT-DATA.    EXEC84.2
086600     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
086700                                                                  EXEC84.2
086800     IF      WY-NO-SOURCE = "Y"                                   EXEC84.2
086900             MOVE   "SOURCE FILE WILL BE SUPPRESSED" TO PRINT-DATAEXEC84.2
087000     ELSE                                                         EXEC84.2
087100             MOVE   "SOURCE FILE WILL BE CREATED" TO PRINT-DATA.  EXEC84.2
087200     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
087300                                                                  EXEC84.2
087400     IF      WY-NEW-POP = "Y"                                     EXEC84.2
087500             MOVE   "NEW POPULATION FILE WILL BE CREATED"         EXEC84.2
087600                  TO PRINT-DATA                                   EXEC84.2
087700     ELSE                                                         EXEC84.2
087800             MOVE   "NEW POPULATION FILE WILL BE SUPPRESSED"      EXEC84.2
087900                  TO PRINT-DATA.                                  EXEC84.2
088000     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
088100                                                                  EXEC84.2
088200                                                                  EXEC84.2
088300 C50-30.                                                          EXEC84.2
088400     IF      WY-NO-LIBRARY = "Y"                                  EXEC84.2
088500             MOVE "LIBRARY NOT SELECTED"                          EXEC84.2
088600                  TO PRINT-DATA                                   EXEC84.2
088700     ELSE                                                         EXEC84.2
088800             MOVE "LIBRARY FILES WILL BE SELECTED"                EXEC84.2
088900                  TO PRINT-DATA.                                  EXEC84.2
089000     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
089100                                                                  EXEC84.2
089200 C50-35.                                                          EXEC84.2
089300     IF      WY-NO-DATA = "Y"                                     EXEC84.2
089400             MOVE   "DATA BLOCKS WILL BE IGNORED"                 EXEC84.2
089500                  TO PRINT-DATA                                   EXEC84.2
089600     ELSE                                                         EXEC84.2
089700             MOVE   "DATA BLOCKS WILL BE SENT TO SOURCE FILE"     EXEC84.2
089800                  TO PRINT-DATA.                                  EXEC84.2
089900     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
090000                                                                  EXEC84.2
090100 C50-40.                                                          EXEC84.2
090200     IF      WY-EXTRACT-ALL = "Y"                                 EXEC84.2
090300             MOVE   "ALL PROGRAMS SELECTED"     TO PRINT-DATA.    EXEC84.2
090400     IF      WY-EXTRACT-AUTO = "Y"                                EXEC84.2
090500             MOVE   "ALL AUTOMATIC PROGRAMS SELECTED"             EXEC84.2
090600                  TO PRINT-DATA.                                  EXEC84.2
090700     IF      WY-EXTRACT-MAN = "Y"                                 EXEC84.2
090800             MOVE   "ALL MANUAL PROGRAMS SELECTED"                EXEC84.2
090900                  TO PRINT-DATA.                                  EXEC84.2
091000     IF      WY-SELECT-PROG = "Y"                                 EXEC84.2
091100             MOVE   "SEPARATE PROGRAMS SELECTED"                  EXEC84.2
091200                  TO PRINT-DATA.                                  EXEC84.2
091300     IF      WY-SELECT-MODULE = "Y"                               EXEC84.2
091400             MOVE   "SEPARATE MODULES SELECTED"                   EXEC84.2
091500                  TO PRINT-DATA.                                  EXEC84.2
091600     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
091700                                                                  EXEC84.2
091800     IF      WY-REMOVE-COMMENTS = "Y"                             EXEC84.2
091900             MOVE   "COMMENT LINES WILL BE REMOVED"               EXEC84.2
092000                  TO PRINT-DATA                                   EXEC84.2
092100     ELSE                                                         EXEC84.2
092200             MOVE   "COMMENT LINES WILL BE RETAINED"              EXEC84.2
092300                  TO PRINT-DATA.                                  EXEC84.2
092400     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
092500                                                                  EXEC84.2
092600                                                                  EXEC84.2
092700     IF      WY-KILL-DELETIONS = "Y"                              EXEC84.2
092800             MOVE   "UNUSED OPTIONAL CODE WILL BE REMOVED"        EXEC84.2
092900                  TO PRINT-DATA                                   EXEC84.2
093000     ELSE                                                         EXEC84.2
093100             MOVE   "UNUSED OPTIONAL CODE WILL BE CONVERTED TO COMEXEC84.2
093200-    "MENTS AND RETAINED"                                         EXEC84.2
093300                  TO PRINT-DATA.                                  EXEC84.2
093400     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
093500                                                                  EXEC84.2
093600 C50-EXIT.                                                        EXEC84.2
093700     EXIT.                                                        EXEC84.2
093800                                                                  EXEC84.2
093900/                                                                 EXEC84.2
094000 C60-START-CARD SECTION.                                          EXEC84.2
094100*======================                                           EXEC84.2
094200*                                                                 EXEC84.2
094300****************************************************************  EXEC84.2
094400*    WHEN A "*START" CARD IS READ THIS SECTION INITIALISES     *  EXEC84.2
094500*    VARIOUS FIELDS AND SETS CERTAIN FLAGS.                    *  EXEC84.2
094600*                                                              *  EXEC84.2
094700*    PERFORMED BY    C10-PROCESS-MONITOR                       *  EXEC84.2
094800*                    D62-SERIES-UPDATE                         *  EXEC84.2
094900*                    D67-LINE-UPDATE                           *  EXEC84.2
095000*                    D68-LINE-EQUAL                            *  EXEC84.2
095100*                    D69-LINE-BLANK-OR-LESS                    *  EXEC84.2
095200*                    D40-ANY-UPDATES                              EXEC84.2
095300*    PERFORMS        NONE                                      *  EXEC84.2
095400****************************************************************  EXEC84.2
095500 C60-1.                                                           EXEC84.2
095600                                                                  EXEC84.2
095700     MOVE    WB-UPDATE-PROG TO WZ-CURRENT-UPD-PROG.               EXEC84.2
095800                                                                  EXEC84.2
095900     IF      WZ-CURRENT-UPD-PROG = WZ-CURRENT-POP-PROG            EXEC84.2
096000             MOVE   "Y"    TO WZ-UPDATE-THIS-PROG                 EXEC84.2
096100     ELSE                                                         EXEC84.2
096200             MOVE    SPACE TO WZ-UPDATE-THIS-PROG.                EXEC84.2
096300                                                                  EXEC84.2
096400 C60-EXIT.                                                        EXEC84.2
096500     EXIT.                                                        EXEC84.2
096600/                                                                 EXEC84.2
096700 D10-MERGE-UPDATE-CARDS SECTION.                                  EXEC84.2
096800*==============================                                   EXEC84.2
096900*                                                                 EXEC84.2
097000****************************************************************  EXEC84.2
097100*    THIS IS THE MAIN CONTROL SECTION FOR THE PROCESSING OF    *  EXEC84.2
097200*    THE POPULATION FILE.  IT OBTAINS THE HEADER FOR THE FIRST *  EXEC84.2
097300*    AUDIT ROUTINE IN THE POPFILE AND PROCESSES ALL RECORDS.   *  EXEC84.2
097400*                                                              *  EXEC84.2
097500*    PERFORMED BY    A10-MAIN                                  *  EXEC84.2
097600*    PERFORMS        D11-GET-NEXT-PROGRAM                      *  EXEC84.2
097700*                    D15-END-OF-POPFILE                        *  EXEC84.2
097800*                    D20-PROCESS-POPFILE                       *  EXEC84.2
097900****************************************************************  EXEC84.2
098000 D10-1-OPEN-FILES.                                                EXEC84.2
098100     IF      WY-NEW-POP = "Y"                                     EXEC84.2
098200             OPEN    OUTPUT UPDATED-POPULATION-FILE.              EXEC84.2
098300                                                                  EXEC84.2
098400     IF      WY-NO-SOURCE = SPACE                                 EXEC84.2
098500             OPEN    OUTPUT SOURCE-COBOL-PROGRAMS.                EXEC84.2
098600                                                                  EXEC84.2
098700     OPEN    INPUT   POPULATION-FILE.                             EXEC84.2
098800     PERFORM D10-20-GET-VERSION.                                  EXEC84.2
098900     MOVE WZ-VERSION-CARD TO PRINT-DATA.                          EXEC84.2
099000     PERFORM X20-PRINT-DETAIL.                                    EXEC84.2
099100                                                                  EXEC84.2
099200 D10-10-GET-HEADER.                                               EXEC84.2
099300     PERFORM D11-GET-NEXT-PROGRAM.                                EXEC84.2
099400                                                                  EXEC84.2
099500     PERFORM D20-PROCESS-POPFILE                                  EXEC84.2
099600             UNTIL   WZ-END-OF-POPFILE = "Y".                     EXEC84.2
099700                                                                  EXEC84.2
099800     PERFORM D15-END-OF-POPFILE.                                  EXEC84.2
099900                                                                  EXEC84.2
100000 D10-EXIT.                                                        EXEC84.2
100100     EXIT.                                                        EXEC84.2
100200/                                                                 EXEC84.2
100300 D10-20-GET-VERSION SECTION.                                      EXEC84.2
100400*============================                                     EXEC84.2
100500*                                                                 EXEC84.2
100600 D10-20-INIT-VER.                                                 EXEC84.2
100700     MOVE WZ-VERSION-NUM TO WA-VERSION-NUM.                       EXEC84.2
100800     MOVE WZ-VERSION-DATE TO WA-VERSION-DATE.                     EXEC84.2
100900                                                                  EXEC84.2
101000 D10-20-EXIT.                                                     EXEC84.2
101100     EXIT.                                                        EXEC84.2
101200/                                                                 EXEC84.2
101300 D11-GET-NEXT-PROGRAM SECTION.                                    EXEC84.2
101400*============================                                     EXEC84.2
101500*                                                                 EXEC84.2
101600****************************************************************  EXEC84.2
101700*    PERFORMED BY    D10-MERGE-UPDATE-CARDS                    *  EXEC84.2
101800*                    D30-PROCESS-HEADER                        *  EXEC84.2
101900****************************************************************  EXEC84.2
102000 D11-1-READ-POPFILE.                                              EXEC84.2
102100     MOVE SPACES  TO WC-STAR-HEADER.                              EXEC84.2
102200                                                                  EXEC84.2
102300     PERFORM X30-READ-POPFILE                                     EXEC84.2
102400             UNTIL   WC-STAR-HEADER = "*HEADER".                  EXEC84.2
102500 D11-TEST-END.                                                    EXEC84.2
102600     IF      WZ-END-OF-POPFILE = "Y"                              EXEC84.2
102700             GO TO   D11-EXIT.                                    EXEC84.2
102800                                                                  EXEC84.2
102900     IF      WC-COBOL NOT = "COBOL"                               EXEC84.2
103000             IF      WC-COBOL = "CLBRY"                           EXEC84.2
103100                     IF      WY-NO-LIBRARY = "Y"                  EXEC84.2
103200                             GO TO   D11-1-READ-POPFILE           EXEC84.2
103300                     ELSE                                         EXEC84.2
103400                             NEXT SENTENCE                        EXEC84.2
103500              ELSE                                                EXEC84.2
103600                     IF      WC-COBOL = "DATA*"                   EXEC84.2
103700                             IF    WY-NO-DATA = "Y"               EXEC84.2
103800                                   GO TO D11-1-READ-POPFILE       EXEC84.2
103900                             ELSE                                 EXEC84.2
104000                                   NEXT SENTENCE                  EXEC84.2
104100                     ELSE                                         EXEC84.2
104200                             GO TO   D11-1-READ-POPFILE.          EXEC84.2
104300                                                                  EXEC84.2
104400 D11-EXIT.                                                        EXEC84.2
104500     EXIT.                                                        EXEC84.2
104600/                                                                 EXEC84.2
104700 D15-END-OF-POPFILE SECTION.                                      EXEC84.2
104800*==========================                                       EXEC84.2
104900*                                                                 EXEC84.2
105000****************************************************************  EXEC84.2
105100*    THIS SECTION PRINTS END OF RUN ACCOUNTING INFORMATION AND *  EXEC84.2
105200*    FLAGS ANY OUTSTANDING *UPDATE RECORDS AS UNUSED.          *  EXEC84.2
105300*                                                              *  EXEC84.2
105400*    PERFORMED BY           D10-MERGE-UPDATE-CARD              *  EXEC84.2
105500*    PERFORMS               D16-FLAG-UNUSED-UPDATES            *  EXEC84.2
105600*                           X20-PRINT-DETAIL                   *  EXEC84.2
105700****************************************************************  EXEC84.2
105800 D15-1-SET-UP-FIELDS.                                             EXEC84.2
105900     MOVE    WZ-PROGS-FOUND  TO WA-PROGS-FOUND.                   EXEC84.2
106000     MOVE    WA-FINAL-LINE-1 TO PRINT-DATA.                       EXEC84.2
106100     PERFORM X20-PRINT-DETAIL.                                    EXEC84.2
106200                                                                  EXEC84.2
106300     IF      WY-NO-SOURCE = SPACE                                 EXEC84.2
106400             MOVE    WZ-SOURCE-PROGS TO WA-SOURCE-PROGS           EXEC84.2
106500             MOVE    WA-FINAL-LINE-2 TO PRINT-DATA                EXEC84.2
106600             PERFORM X20-PRINT-DETAIL.                            EXEC84.2
106700                                                                  EXEC84.2
106800     IF      WY-NEW-POP = "Y"                                     EXEC84.2
106900             MOVE    WZ-NEWPOP-PROGS TO WA-NEWPOP-PROGS           EXEC84.2
107000             MOVE    WA-FINAL-LINE-3 TO PRINT-DATA                EXEC84.2
107100             PERFORM X20-PRINT-DETAIL.                            EXEC84.2
107200                                                                  EXEC84.2
107300     IF      WZ-END-OF-UPDATES = SPACE                            EXEC84.2
107400             READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA      EXEC84.2
107500                     AT END  MOVE "Y" TO WZ-END-OF-UPDATES.       EXEC84.2
107600                                                                  EXEC84.2
107700     PERFORM D16-FLAG-UNUSED-UPDATES                              EXEC84.2
107800       UNTIL WZ-END-OF-UPDATES = "Y".                             EXEC84.2
107900                                                                  EXEC84.2
108000     IF WY-SELECT-PROG = "Y" AND                                  EXEC84.2
108100       WF-PROGRAM-SELECTED-TABLE NOT = SPACES                     EXEC84.2
108200       MOVE "SOME PROGRAMS NOT FOUND" TO PRINT-DATA               EXEC84.2
108300       PERFORM X20-PRINT-DETAIL                                   EXEC84.2
108400       PERFORM VARYING SUB7 FROM 1 BY 1 UNTIL SUB7 > 50           EXEC84.2
108500         IF WF-PROGRAM-SELECTED (SUB7) NOT = SPACE                EXEC84.2
108600              MOVE WF-PROGRAM-SELECTED (SUB7) TO PRINT-DATA       EXEC84.2
108700              PERFORM X20-PRINT-DETAIL                            EXEC84.2
108800              END-IF                                              EXEC84.2
108900         END-PERFORM.                                             EXEC84.2
109000                                                                  EXEC84.2
109100 D15-EXIT.                                                        EXEC84.2
109200     EXIT.                                                        EXEC84.2
109300                                                                  EXEC84.2
109400                                                                  EXEC84.2
109500                                                                  EXEC84.2
109600                                                                  EXEC84.2
109700 D16-FLAG-UNUSED-UPDATES SECTION.                                 EXEC84.2
109800*===============================                                  EXEC84.2
109900 D16-1-PRINT-UPDATES.                                             EXEC84.2
110000     MOVE    WB-CONTROL-DATA TO PRINT-DATA.                       EXEC84.2
110100     PERFORM X20-PRINT-DETAIL.                                    EXEC84.2
110200     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
110300             AT END  MOVE "Y" TO WZ-END-OF-UPDATES.               EXEC84.2
110400                                                                  EXEC84.2
110500 D16-EXIT.                                                        EXEC84.2
110600     EXIT.                                                        EXEC84.2
110700                                                                  EXEC84.2
110800                                                                  EXEC84.2
110900/                                                                 EXEC84.2
111000 D20-PROCESS-POPFILE SECTION.                                     EXEC84.2
111100*===========================                                      EXEC84.2
111200*                                                                 EXEC84.2
111300****************************************************************  EXEC84.2
111400*    THIS IS THE MAIN CONTROL SECTION FOR POPULATION FILE      *  EXEC84.2
111500*    PROCESSING.                                               *  EXEC84.2
111600*                                                              *  EXEC84.2
111700*    PERFORMED BY    D10-MERGE-UPDATE-CARDS                    *  EXEC84.2
111800*    PERFORMS        D25-PRINT-REPORT                          *  EXEC84.2
111900*                    D30-PROCESS-HEADER                        *  EXEC84.2
112000*                    D90-PROCESS-END-OF                        *  EXEC84.2
112100*                    X10-WRITE-UPDATED-POPFILE                 *  EXEC84.2
112200*                    X30-READ-POPFILE                          *  EXEC84.2
112300*                    X40-WRITE-SOURCE-FILE                     *  EXEC84.2
112400****************************************************************  EXEC84.2
112500 D20-1-CHECK-RECORD-TYPE.                                         EXEC84.2
112600     MOVE    SPACE TO WZ-NOT-THIS-COMMENT.                        EXEC84.2
112700     MOVE    SPACE TO WZ-LINE-UPDATE.                             EXEC84.2
112800     MOVE    SPACE TO WZ-REPLACE-FLAG.                            EXEC84.2
112900*    MOVE   "Y"    TO WZ-UPDATE-THIS-PROG.                        EXEC84.2
113000     MOVE    SPACE TO WZ-OPTIONAL-SELECTED.                       EXEC84.2
113100     MOVE    SPACE TO WZ-DELETE-FLAG.                             EXEC84.2
113200                                                                  EXEC84.2
113300     IF      WC-STAR-HEADER = "*HEADER"                           EXEC84.2
113400             MOVE    SPACE  TO WZ-PROGRAM-SELECTED                EXEC84.2
113500             PERFORM D30-PROCESS-HEADER                           EXEC84.2
113600                     UNTIL  WZ-PROGRAM-SELECTED = "Y"             EXEC84.2
113700     ELSE                                                         EXEC84.2
113800             IF      WC-STAR-HEADER = "*END-OF"                   EXEC84.2
113900                     PERFORM D90-PROCESS-END-OF                   EXEC84.2
114000             ELSE                                                 EXEC84.2
114100                     PERFORM D50-PROCESS-COBOL-LINE.              EXEC84.2
114200                                                                  EXEC84.2
114300     PERFORM D25-PRINT-REPORT.                                    EXEC84.2
114400                                                                  EXEC84.2
114500 D20-10-SOURCE-CHECK.                                             EXEC84.2
114600     IF      WY-NO-SOURCE        = "Y"                            EXEC84.2
114700             GO TO   D20-20-UPDATED-POP-CHECK.                    EXEC84.2
114800     IF      WZ-DELETE-FLAG      = "Y"                            EXEC84.2
114900             GO TO   D20-20-UPDATED-POP-CHECK.                    EXEC84.2
115000     IF      WZ-NOT-THIS-COMMENT = "Y"                            EXEC84.2
115100             GO TO   D20-20-UPDATED-POP-CHECK.                    EXEC84.2
115200*    IF      WZ-UPDATE-THIS-PROG =  SPACE                         EXEC84.2
115300*            GO TO   D20-30-READ-FILE.                            EXEC84.2
115400                                                                  EXEC84.2
115500     IF      WZ-END-OF-POPFILE NOT = "Y"                          EXEC84.2
115600             PERFORM X40-WRITE-SOURCE-FILE.                       EXEC84.2
115700                                                                  EXEC84.2
115800 D20-20-UPDATED-POP-CHECK.                                        EXEC84.2
115900     IF      WY-NEW-POP = "Y"                                     EXEC84.2
116000             PERFORM X10-WRITE-UPDATED-POPFILE.                   EXEC84.2
116100             MOVE WZ-SAVE-POP-RECORD TO WC-CURRENT-POP-RECORD.    EXEC84.2
116200                                                                  EXEC84.2
116300 D20-30-READ-FILE.                                                EXEC84.2
116400     IF      WZ-END-OF-POPFILE = "Y"                              EXEC84.2
116500             GO TO   D20-EXIT.                                    EXEC84.2
116600     IF      WB-SEQ-1 = SPACES                                    EXEC84.2
116700             MOVE    WZ-SAVE-SEQ TO WC-6                          EXEC84.2
116800             GO TO   D20-EXIT.                                    EXEC84.2
116900     PERFORM X30-READ-POPFILE.                                    EXEC84.2
117000                                                                  EXEC84.2
117100 D20-EXIT.                                                        EXEC84.2
117200     EXIT.                                                        EXEC84.2
117300                                                                  EXEC84.2
117400/                                                                 EXEC84.2
117500 D25-PRINT-REPORT SECTION.                                        EXEC84.2
117600*========================                                         EXEC84.2
117700*                                                                 EXEC84.2
117800****************************************************************  EXEC84.2
117900*    DEPENDING ON THE PRINT OPTIONS SELECTED, THIS SECTION     *  EXEC84.2
118000*    PRINTS EITHER THE EXTRACTED PROGRAMS OR ANY UPDATES AND   *  EXEC84.2
118100*    X-CARD SUBSTITUTIONS MADE.                                *  EXEC84.2
118200*                                                              *  EXEC84.2
118300*    PERFORMED BY    D20-PROCESS-POPFILE                       *  EXEC84.2
118400*    PERFORMS        X20-PRINT-DETAIL                          *  EXEC84.2
118500*                    D27-PRINT-UPDATES                         *  EXEC84.2
118600****************************************************************  EXEC84.2
118700 D25-1-CHECK-LIST-PROGRAMS.                                       EXEC84.2
118800     IF      WY-LIST-PROGRAMS = "Y"                               EXEC84.2
118900             MOVE    WC-CURRENT-POP-RECORD TO PRINT-DATA          EXEC84.2
119000             PERFORM X20-PRINT-DETAIL                             EXEC84.2
119100             GO TO   D25-EXIT.                                    EXEC84.2
119200                                                                  EXEC84.2
119300 D25-10-UPDATES-CHECK.                                            EXEC84.2
119400     IF      WY-LIST-NO-UPDATES = SPACE                           EXEC84.2
119500             IF      WZ-LINE-UPDATE = "Y"                         EXEC84.2
119600                     PERFORM D27-PRINT-UPDATES                    EXEC84.2
119700                     GO TO   D25-EXIT.                            EXEC84.2
119800                                                                  EXEC84.2
119900 D25-20-X-CARD-CHECK.                                             EXEC84.2
120000     IF      WY-LIST-X-CARDS = "Y"                                EXEC84.2
120100             IF      WZ-SAVE-12-15 = "XXXX"                       EXEC84.2
120200                     MOVE    WZ-SAVE-12-20 TO WE-X-CARD           EXEC84.2
120300                     MOVE   "SUBSTITUTION" TO WE-CHANGE-TYPE      EXEC84.2
120400                     MOVE    WC-CURRENT-POP-RECORD                EXEC84.2
120500                                           TO WE-COBOL-LINE       EXEC84.2
120600                     MOVE    WE-PRINT-DATA TO PRINT-DATA          EXEC84.2
120700                     PERFORM X20-PRINT-DETAIL.                    EXEC84.2
120800                                                                  EXEC84.2
120900 D25-EXIT.                                                        EXEC84.2
121000     EXIT.                                                        EXEC84.2
121100                                                                  EXEC84.2
121200                                                                  EXEC84.2
121300                                                                  EXEC84.2
121400/                                                                 EXEC84.2
121500 D27-PRINT-UPDATES SECTION.                                       EXEC84.2
121600*=========================                                        EXEC84.2
121700*                                                                 EXEC84.2
121800****************************************************************  EXEC84.2
121900*    THIS SECTION WILL PRINT THE ORIGINAL POPULATION FILE      *  EXEC84.2
122000*    RECORD (IF "*LIST NO-UPDATES" HAS NOT BEEN SPECIFIED) AND,*  EXEC84.2
122100*    IF A REPLACEMENT, THE NEW IMAGE OR, IF AN INSERTION, JUST *  EXEC84.2
122200*    THE NEW LINE OF CODE.                                     *  EXEC84.2
122300*                                                              *  EXEC84.2
122400*    PERFORMED BY    D25-PRINT-REPORT                          *  EXEC84.2
122500*    PERFORMS        X20-PRINT-DETAIL                          *  EXEC84.2
122600****************************************************************  EXEC84.2
122700 D27-1-PRINT-REPLACEMENTS.                                        EXEC84.2
122800     IF      WZ-REPLACE-FLAG = "Y"                                EXEC84.2
122900             MOVE    WZ-SAVE-POP-RECORD  TO WE-COBOL-LINE         EXEC84.2
123000             MOVE    SPACES              TO WE-X-CARD             EXEC84.2
123100             MOVE   "ORIGINAL"           TO WE-CHANGE-TYPE        EXEC84.2
123200             MOVE    WE-PRINT-DATA       TO PRINT-DATA            EXEC84.2
123300             PERFORM X20-PRINT-DETAIL                             EXEC84.2
123400             MOVE   "REPLACEMENT"        TO WE-CHANGE-TYPE        EXEC84.2
123500             MOVE    SPACES              TO WE-X-CARD             EXEC84.2
123600             MOVE    WC-CURRENT-POP-RECORD  TO WE-COBOL-LINE      EXEC84.2
123700             MOVE    WE-PRINT-DATA       TO PRINT-DATA            EXEC84.2
123800             PERFORM X20-PRINT-DETAIL                             EXEC84.2
123900             GO TO   D27-EXIT.                                    EXEC84.2
124000                                                                  EXEC84.2
124100 D27-10-PRINT-NEW-DATA.                                           EXEC84.2
124200     IF      WZ-DONT-READ-POPFILE = "Y"                           EXEC84.2
124300             MOVE    WC-CURRENT-POP-RECORD TO WE-COBOL-LINE       EXEC84.2
124400             MOVE    WE-PRINT-DATA         TO PRINT-DATA          EXEC84.2
124500             PERFORM X20-PRINT-DETAIL                             EXEC84.2
124600             GO TO   D27-EXIT.                                    EXEC84.2
124700                                                                  EXEC84.2
124800 D27-20-PRINT-DELETIONS.                                          EXEC84.2
124900     IF      WZ-DELETE-FLAG = "Y"                                 EXEC84.2
125000             MOVE    WZ-SAVE-POP-RECORD  TO WE-COBOL-LINE         EXEC84.2
125100             MOVE    WE-PRINT-DATA       TO PRINT-DATA            EXEC84.2
125200             PERFORM X20-PRINT-DETAIL.                            EXEC84.2
125300                                                                  EXEC84.2
125400 D27-EXIT.                                                        EXEC84.2
125500     EXIT.                                                        EXEC84.2
125600                                                                  EXEC84.2
125700/                                                                 EXEC84.2
125800 D30-PROCESS-HEADER SECTION.                                      EXEC84.2
125900*==========================                                       EXEC84.2
126000*                                                                 EXEC84.2
126100****************************************************************  EXEC84.2
126200*    THIS SECTION PROCESSES ANY "*HEADER" RECORDS.             *  EXEC84.2
126300*                                                              *  EXEC84.2
126400*    PERFORMED BY    D20-PROCESS-POPFILE                       *  EXEC84.2
126500*    PERFORMS        D90-PROCESS-END-OF                        *  EXEC84.2
126600*                    D11-GET-NEXT-PROGRAM                      *  EXEC84.2
126700*                    D31-PROCESS-DATA-HEADER                   *  EXEC84.2
126800*                    D32-PROCESS-COBOL-HEADER                  *  EXEC84.2
126900*                    D40-ANY-UPDATES                           *  EXEC84.2
127000*                    X10-WRITE-UPDATED-POPFILE                 *  EXEC84.2
127100*                    D43-PROGRAM-SELECTED                      *  EXEC84.2
127200****************************************************************  EXEC84.2
127300 D30-1-CHECK-HEADER-TYPE.                                         EXEC84.2
127400     MOVE    WC-COBOL TO BLOCK-TYPE                               EXEC84.2
127500     IF      WC-COBOL = "CLBRY"                                   EXEC84.2
127600     AND WY-NO-LIBRARY NOT = "Y"                                  EXEC84.2
127700             MOVE WC-HEADER TO PRINT-DATA                         EXEC84.2
127800             MOVE WC-PROG-ID TO WZ-CURRENT-MAIN-PROG              EXEC84.2
127900                                WZ-CURRENT-POP-PROG               EXEC84.2
128000***************************************************************** EXEC84.2
128100* NOTE FOLLOWING CHANGES                                      *** EXEC84.2
128200***************************************************************** EXEC84.2
128300*            PERFORM D36-INITIALISE-PROGRAM-TOTALS                EXEC84.2
128400             MOVE   "Y" TO WZ-PROGRAM-SELECTED                    EXEC84.2
128500             PERFORM D34-PROGRAM-SELECTED.                        EXEC84.2
128600                                                                  EXEC84.2
128700     IF      WC-COBOL = "DATA*"                                   EXEC84.2
128800             PERFORM D31-PROCESS-DATA-HEADER.                     EXEC84.2
128900                                                                  EXEC84.2
129000     IF      WC-COBOL = "COBOL"                                   EXEC84.2
129100             ADD     1 TO WZ-PROGS-FOUND                          EXEC84.2
129200             PERFORM D32-PROCESS-COBOL-HEADER.                    EXEC84.2
129300                                                                  EXEC84.2
129400     IF      WZ-PROGRAM-SELECTED = "Y"                            EXEC84.2
129500             GO TO   D30-20-HEADER-SELECTED.                      EXEC84.2
129600                                                                  EXEC84.2
129700     PERFORM D11-GET-NEXT-PROGRAM.                                EXEC84.2
129800     IF      WZ-END-OF-POPFILE = "Y"                              EXEC84.2
129900             MOVE    "Y" TO WZ-PROGRAM-SELECTED                   EXEC84.2
130000             GO TO    D30-EXIT                                    EXEC84.2
130100     ELSE                                                         EXEC84.2
130200             GO TO    D30-1-CHECK-HEADER-TYPE.                    EXEC84.2
130300                                                                  EXEC84.2
130400 D30-20-HEADER-SELECTED.                                          EXEC84.2
130500     MOVE    WC-COBOL TO WZ-CURRENT-HEADER.                       EXEC84.2
130600     IF      WZ-END-OF-UPDATES = SPACE                            EXEC84.2
130700             PERFORM D40-ANY-UPDATES.                             EXEC84.2
130800                                                                  EXEC84.2
130900 D30-EXIT.                                                        EXEC84.2
131000     EXIT.                                                        EXEC84.2
131100/                                                                 EXEC84.2
131200 D31-PROCESS-DATA-HEADER SECTION.                                 EXEC84.2
131300*===============================                                  EXEC84.2
131400*                                                                 EXEC84.2
131500 D31-1-CHECK-FOR-DATA-SELECTED.                                   EXEC84.2
131600     IF      WY-NO-DATA = "Y"                                     EXEC84.2
131700             GO TO   D31-EXIT.                                    EXEC84.2
131800                                                                  EXEC84.2
131900     IF      WZ-PROG-ID-6 = "M"                                   EXEC84.2
132000             GO TO   D31-30-MANUAL-CHECK.                         EXEC84.2
132100                                                                  EXEC84.2
132200 D31-20-AUTO-CHECK.                                               EXEC84.2
132300     IF      WY-EXTRACT-MAN = SPACE                               EXEC84.2
132400             MOVE   "Y"        TO WZ-PROGRAM-SELECTED             EXEC84.2
132500     ELSE                                                         EXEC84.2
132600             GO TO   D31-EXIT.                                    EXEC84.2
132700                                                                  EXEC84.2
132800 D31-30-MANUAL-CHECK.                                             EXEC84.2
132900     IF      WY-EXTRACT-AUTO = SPACE                              EXEC84.2
133000             MOVE   "Y"        TO WZ-PROGRAM-SELECTED.            EXEC84.2
133100                                                                  EXEC84.2
133200                                                                  EXEC84.2
133300 D31-EXIT.                                                        EXEC84.2
133400     EXIT.                                                        EXEC84.2
133500                                                                  EXEC84.2
133600/                                                                 EXEC84.2
133700 D32-PROCESS-COBOL-HEADER SECTION.                                EXEC84.2
133800*================================                                 EXEC84.2
133900*                                                                 EXEC84.2
134000****************************************************************  EXEC84.2
134100*    THIS SECTION CHECKS TO SEE IF THE CURRENT HEADER IS       *  EXEC84.2
134200*    IS WITHIN THE RANGE SPECIFIED BY THE "*EXTRACT" OR        *  EXEC84.2
134300*    "*SELECT" RECORDS OF THE INPUT CONTROL-FILE.              *  EXEC84.2
134400*                                                              *  EXEC84.2
134500*    PERFORMED BY    D30-PROCESS-HEADER                        *  EXEC84.2
134600*    PERFORMS        D33-MODULE-CHECK                          *  EXEC84.2
134700*                    D34-PROGRAM-SELECTED                      *  EXEC84.2
134800****************************************************************  EXEC84.2
134900*                                                              *  EXEC84.2
135000****************************************************************  EXEC84.2
135100*  IF PROGRAM DOES NOT END WITH '5','A','B' OR 'M' IT          *  EXEC84.2
135200*  SHOULD HAVE A SPACE CHARACTER FOR POSITION 6.               *  EXEC84.2
135300****************************************************************  EXEC84.2
135400*                                                              *  EXEC84.2
135500 D32-0-CHECK-A-OR-M.                                              EXEC84.2
135600     IF WC-PROG-ID-6 = "A"                                        EXEC84.2
135700        GO TO D32-1-CHECK-FOR-ALL-SELECTED.                       EXEC84.2
135800     IF WC-PROG-ID-6 = "B"                                        EXEC84.2
135900        GO TO D32-1-CHECK-FOR-ALL-SELECTED.                       EXEC84.2
136000     IF WC-PROG-ID-6 = "M"                                        EXEC84.2
136100        GO TO D32-1-CHECK-FOR-ALL-SELECTED.                       EXEC84.2
136200     IF WC-PROG-ID-6 = "5"                                        EXEC84.2
136300        GO TO D32-1-CHECK-FOR-ALL-SELECTED.                       EXEC84.2
136400     MOVE SPACE TO WC-PROG-ID-6.                                  EXEC84.2
136500 D32-1-CHECK-FOR-ALL-SELECTED.                                    EXEC84.2
136600     IF      WY-EXTRACT-ALL = "Y"                                 EXEC84.2
136700             MOVE   "Y" TO WZ-PROGRAM-SELECTED                    EXEC84.2
136800             MOVE    WC-PROG-ID TO WZ-CURRENT-POP-PROG            EXEC84.2
136900             GO TO   D32-50-CHECK-IF-SELECTED.                    EXEC84.2
137000                                                                  EXEC84.2
137100 D32-10-MANUAL-CHECK.                                             EXEC84.2
137200     IF      WY-EXTRACT-MAN = "Y"                                 EXEC84.2
137300             IF      WC-PROG-ID-6 = "M"                           EXEC84.2
137400                     MOVE   "Y" TO WZ-PROGRAM-SELECTED            EXEC84.2
137500                     MOVE    WC-PROG-ID TO WZ-CURRENT-POP-PROG    EXEC84.2
137600                     GO TO   D32-50-CHECK-IF-SELECTED.            EXEC84.2
137700                                                                  EXEC84.2
137800 D32-20-AUTO-CHECK.                                               EXEC84.2
137900     IF      WY-EXTRACT-AUTO = "Y"                                EXEC84.2
138000             IF      WC-PROG-ID-6 = "A" OR "B"                    EXEC84.2
138100                     MOVE   "Y" TO WZ-PROGRAM-SELECTED            EXEC84.2
138200                     MOVE    WC-PROG-ID TO WZ-CURRENT-POP-PROG    EXEC84.2
138300                     GO TO   D32-50-CHECK-IF-SELECTED.            EXEC84.2
138400                                                                  EXEC84.2
138500 D32-30-MODULE-CHECK.                                             EXEC84.2
138600     IF      WY-SELECT-MODULE = "Y"                               EXEC84.2
138700*THE FOLLOWING CHECK ASSUMES THAT THE FILES ON THE POPFILE        EXEC84.2
138800*ARE IN ORDER!                                                    EXEC84.2
138900             IF WG-MODULE-SELECTED-TABLE = SPACES                 EXEC84.2
139000                MOVE "Y" TO WZ-END-OF-POPFILE                     EXEC84.2
139100             ELSE                                                 EXEC84.2
139200             MOVE    1 TO SUB6                                    EXEC84.2
139300             PERFORM D33-MODULE-CHECK                             EXEC84.2
139400             GO TO   D32-50-CHECK-IF-SELECTED.                    EXEC84.2
139500                                                                  EXEC84.2
139600     MOVE    1 TO SUB7.                                           EXEC84.2
139700 D32-40-PROGRAM-CHECK.                                            EXEC84.2
139800     IF      WY-SELECT-PROG = "Y"                                 EXEC84.2
139900             IF WF-PROGRAM-SELECTED-TABLE = SPACES                EXEC84.2
140000                     MOVE "Y" TO WZ-END-OF-POPFILE                EXEC84.2
140100             ELSE                                                 EXEC84.2
140200             IF      WC-PROG-ID-1-5 = WF-PROGRAM-SELECTED (SUB7)  EXEC84.2
140300                     MOVE SPACE TO WF-PROGRAM-SELECTED (SUB7)     EXEC84.2
140400                     MOVE   "Y" TO WZ-PROGRAM-SELECTED            EXEC84.2
140500                     MOVE    WC-PROG-ID TO WZ-CURRENT-POP-PROG    EXEC84.2
140600                     GO TO   D32-50-CHECK-IF-SELECTED             EXEC84.2
140700             ELSE                                                 EXEC84.2
140800                     IF      SUB7  < 50                           EXEC84.2
140900                             ADD     1 TO SUB7                    EXEC84.2
141000                             GO TO   D32-40-PROGRAM-CHECK         EXEC84.2
141100                     ELSE                                         EXEC84.2
141200                          IF WC-SUBPRG = "SUBPRG" OR "SUBRTN"     EXEC84.2
141300                             MOVE 1 TO SUB7                       EXEC84.2
141400                             PERFORM 50 TIMES                     EXEC84.2
141500                                IF WC-PROG2ID-1-5 =               EXEC84.2
141600                                  WF-PROGRAM-SELECTED (SUB7)      EXEC84.2
141700                                  MOVE SPACE TO                   EXEC84.2
141800                                     WF-PROGRAM-SELECTED (SUB7)   EXEC84.2
141900                                  MOVE "Y" TO                     EXEC84.2
142000                                     WZ-PROGRAM-SELECTED          EXEC84.2
142100                                  MOVE WC-PROG2ID TO              EXEC84.2
142200                                     WZ-CURRENT-POP-PROG          EXEC84.2
142300                                  GO D32-50-CHECK-IF-SELECTED     EXEC84.2
142400                                  END-IF                          EXEC84.2
142500                             ADD 1 TO SUB7                        EXEC84.2
142600                             END-PERFORM.                         EXEC84.2
142700                                                                  EXEC84.2
142800 D32-50-CHECK-IF-SELECTED.                                        EXEC84.2
142900     IF      WZ-PROGRAM-SELECTED = "Y"                            EXEC84.2
143000             PERFORM D34-PROGRAM-SELECTED.                        EXEC84.2
143100                                                                  EXEC84.2
143200 D32-EXIT.                                                        EXEC84.2
143300     EXIT.                                                        EXEC84.2
143400/                                                                 EXEC84.2
143500 D33-MODULE-CHECK SECTION.                                        EXEC84.2
143600*========================                                         EXEC84.2
143700*                                                                 EXEC84.2
143800****************************************************************  EXEC84.2
143900*    IF ONE OR MORE MODULES HAVE BEEN SELECTED, THIS SECTION   *  EXEC84.2
144000*    CHECKS THAT THE CURRENT POPULATION FILE PROGRAM IS FROM   *  EXEC84.2
144100*    A SELECTED MODULE (AND LEVEL, IF LEVEL SELECTED).         *  EXEC84.2
144200*                                                              *  EXEC84.2
144300*    PERFORMED BY    D32-PROCESS-COBOL-HEADER                  *  EXEC84.2
144400*    PERFORMS        NONE                                      *  EXEC84.2
144500****************************************************************  EXEC84.2
144600 D33-1-MODULE-CHECK.                                              EXEC84.2
144700     IF      WC-MODULE = WG-MODULE-SELECTED (SUB6)                EXEC84.2
144800             GO TO D33-10-CHECK-MODULE-LEVEL.                     EXEC84.2
144900     IF      WC-MODULE > WG-MODULE-SELECTED (SUB6) AND            EXEC84.2
145000*THE FOLLOWING CHECK MAKES SURE SOME FILES GET OFF                EXEC84.2
145100             WZ-SOURCE-PROGS NOT = 0                              EXEC84.2
145200             MOVE SPACE TO WG-MODULE-SELECTED (SUB6)              EXEC84.2
145300             MOVE SPACE TO WG-MODULE-LEVEL (SUB6)                 EXEC84.2
145400             IF      SUB6  < 10                                   EXEC84.2
145500                     ADD     1 TO SUB6                            EXEC84.2
145600                     GO TO   D33-1-MODULE-CHECK                   EXEC84.2
145700             ELSE                                                 EXEC84.2
145800                     GO TO   D33-EXIT                             EXEC84.2
145900     ELSE                                                         EXEC84.2
146000             GO TO D33-EXIT.                                      EXEC84.2
146100                                                                  EXEC84.2
146200 D33-10-CHECK-MODULE-LEVEL.                                       EXEC84.2
146300     IF      WG-MODULE-LEVEL (SUB6) = SPACE                       EXEC84.2
146400             MOVE    WC-PROG-ID TO WZ-CURRENT-POP-PROG            EXEC84.2
146500             MOVE   "Y" TO WZ-PROGRAM-SELECTED                    EXEC84.2
146600             GO TO   D33-EXIT.                                    EXEC84.2
146700                                                                  EXEC84.2
146800     IF      WG-MODULE-LEVEL (SUB6) = WC-LEVEL                    EXEC84.2
146900             MOVE    WC-PROG-ID TO WZ-CURRENT-POP-PROG            EXEC84.2
147000             MOVE   "Y" TO WZ-PROGRAM-SELECTED.                   EXEC84.2
147100                                                                  EXEC84.2
147200 D33-EXIT.                                                        EXEC84.2
147300     EXIT.                                                        EXEC84.2
147400                                                                  EXEC84.2
147500/                                                                 EXEC84.2
147600 D34-PROGRAM-SELECTED SECTION.                                    EXEC84.2
147700*============================                                     EXEC84.2
147800*                                                                 EXEC84.2
147900****************************************************************  EXEC84.2
148000*    IF THE CURRENT POPULATION FILE PROGRAM HAS BEEN SELECTED  *  EXEC84.2
148100*    FOR PROCESSING, THIS SECTION UPDATES VARIOUS FIELDS.      *  EXEC84.2
148200*                                                              *  EXEC84.2
148300*    PERFORMED BY    D32-PROCESS-COBOL-HEADER                  *  EXEC84.2
148400*    PERFORMS        NONE                                      *  EXEC84.2
148500****************************************************************  EXEC84.2
148600 D34-1-SELECTION-CHECK.                                           EXEC84.2
148700     IF      WZ-PROGRAM-SELECTED = SPACE                          EXEC84.2
148800             GO TO   D34-EXIT.                                    EXEC84.2
148900                                                                  EXEC84.2
149000     IF      WY-NO-SOURCE = SPACE                                 EXEC84.2
149100             ADD     1 TO WZ-SOURCE-PROGS.                        EXEC84.2
149200                                                                  EXEC84.2
149300     IF      WY-NEW-POP = "Y"                                     EXEC84.2
149400             ADD     1 TO WZ-NEWPOP-PROGS.                        EXEC84.2
149500                                                                  EXEC84.2
149600*    MOVE    SPACE TO WZ-CURRENT-MAIN-PROG.                       EXEC84.2
149700     MOVE    WZ-CURRENT-POP-PROG TO WZ-CURRENT-MAIN-PROG.         EXEC84.2
149800                                                                  EXEC84.2
149900     PERFORM D36-INITIALISE-PROGRAM-TOTALS.                       EXEC84.2
150000                                                                  EXEC84.2
150100     MOVE    WC-HEADER TO PRINT-DATA.                             EXEC84.2
150200     IF      WC-SUBPRG = "SUBPRG"                                 EXEC84.2
150300             MOVE    WZ-CURRENT-POP-PROG TO WZ-CURRENT-MAIN-PROG  EXEC84.2
150400*            MOVE    WC-PROG2ID          TO WZ-CURRENT-UPD-PROG   EXEC84.2
150500             MOVE    WC-PROG2ID          TO WZ-CURRENT-POP-PROG   EXEC84.2
150600     END-IF.                                                      EXEC84.2
150700     IF      WC-SUBPRG = "SUBRTN"                                 EXEC84.2
150800             MOVE    WZ-CURRENT-POP-PROG TO WZ-CURRENT-MAIN-PROG  EXEC84.2
150900*            MOVE    WC-PROG2ID          TO WZ-CURRENT-UPD-PROG   EXEC84.2
151000             MOVE    WC-PROG2ID          TO WZ-CURRENT-POP-PROG   EXEC84.2
151100     END-IF.                                                      EXEC84.2
151200                                                                  EXEC84.2
151300 D34-2-OPEN-OUTPUT.                                               EXEC84.2
151400     IF      WY-LIST-COMPACT = SPACE                              EXEC84.2
151500             MOVE    51 TO WZ-LINE-CT.                            EXEC84.2
151600                                                                  EXEC84.2
151700     PERFORM X20-PRINT-DETAIL.                                    EXEC84.2
151800 D34-EXIT.                                                        EXEC84.2
151900     EXIT.                                                        EXEC84.2
152000/                                                                 EXEC84.2
152100 D36-INITIALISE-PROGRAM-TOTALS SECTION.                           EXEC84.2
152200*=====================================                            EXEC84.2
152300*                                                                 EXEC84.2
152400****************************************************************  EXEC84.2
152500*    WHEN A PROGRAM "*HEADER" IS SELECTED THIS SECTION RESETS  *  EXEC84.2
152600*    ALL THE VARIOUS TOTALS FOR THAT PROGRAM.                  *  EXEC84.2
152700*                                                              *  EXEC84.2
152800*    PERFORMED BY    D32-PROCESS-COBOL-HEADER                  *  EXEC84.2
152900*    PERFORMS        NONE                                      *  EXEC84.2
153000****************************************************************  EXEC84.2
153100 D36-1-RESET-TOTALS.                                              EXEC84.2
153200     MOVE    ZERO  TO WZ-LINES-COBOL.                             EXEC84.2
153300     MOVE    ZERO  TO WZ-LINES-INSERTED.                          EXEC84.2
153400     MOVE    ZERO  TO WZ-LINES-REPLACED.                          EXEC84.2
153500     MOVE    ZERO  TO WZ-LINES-DELETED.                           EXEC84.2
153600     MOVE    ZERO  TO WZ-CODE-REMOVED.                            EXEC84.2
153700     MOVE    ZERO  TO WZ-COMMENTS-DEL.                            EXEC84.2
153800     MOVE    ZERO  TO WZ-SEQ-NO.                                  EXEC84.2
153900                                                                  EXEC84.2
154000 D36-EXIT.                                                        EXEC84.2
154100     EXIT.                                                        EXEC84.2
154200/                                                                 EXEC84.2
154300 D40-ANY-UPDATES SECTION.                                         EXEC84.2
154400*=======================                                          EXEC84.2
154500*                                                                 EXEC84.2
154600****************************************************************  EXEC84.2
154700*    THIS SECTION FINDS THE FIRST UPDATE RECORD (IF ANY) FOR   *  EXEC84.2
154800*    THE CURRENT POPULATION FILE PROGRAM BEING PROCESSED.      *  EXEC84.2
154900*                                                              *  EXEC84.2
155000*    PERFORMED BY    D30-PROCESS-HEADER                        *  EXEC84.2
155100*    PERFORMS        D42-READ-UPDATE                           *  EXEC84.2
155200****************************************************************  EXEC84.2
155300 D40-1-CHECK-UPDATE-AND-POP.                                      EXEC84.2
155400     MOVE    SPACE TO WZ-UPDATE-THIS-PROG.                        EXEC84.2
155500     IF      WZ-CURRENT-UPD-PROG > WZ-CURRENT-POP-PROG            EXEC84.2
155600             GO TO   D40-EXIT.                                    EXEC84.2
155700                                                                  EXEC84.2
155800     IF      WZ-CURRENT-UPD-PROG = WZ-CURRENT-POP-PROG            EXEC84.2
155900             GO TO   D40-20-PROGRAMS-EQUAL.                       EXEC84.2
156000                                                                  EXEC84.2
156100*    IF      WZ-CURRENT-POP-PROG = WB-UPDATE-PROG                 EXEC84.2
156200*            GO TO   D40-20-PROGRAMS-EQUAL.                       EXEC84.2
156300                                                                  EXEC84.2
156400     IF      WZ-UPD-PROG-CHAR  = "A"                              EXEC84.2
156500             GO TO D40-05-CHECK-LIBRARY.                          EXEC84.2
156600                                                                  EXEC84.2
156700     IF      WZ-UPD-PROG-CHAR = "K"                               EXEC84.2
156800             GO TO D40-05-CHECK-LIBRARY                           EXEC84.2
156900     ELSE    GO TO D40-10-GET-NEXT-STAR-START.                    EXEC84.2
157000*                                                                 EXEC84.2
157100 D40-05-CHECK-LIBRARY.                                            EXEC84.2
157200     IF      WZ-CURRENT-POP-PROG < "SQ000"                        EXEC84.2
157300             GO TO D40-EXIT.                                      EXEC84.2
157400                                                                  EXEC84.2
157500 D40-10-GET-NEXT-STAR-START.                                      EXEC84.2
157600     MOVE    SPACES TO WB-STAR-START.                             EXEC84.2
157700     PERFORM D42-READ-UPDATE-FILE                                 EXEC84.2
157800             UNTIL   WB-STAR-START = "*START".                    EXEC84.2
157900                                                                  EXEC84.2
158000     IF      WZ-END-OF-UPDATES = "Y"                              EXEC84.2
158100             GO TO   D40-EXIT.                                    EXEC84.2
158200     PERFORM C60-START-CARD.                                      EXEC84.2
158300                                                                  EXEC84.2
158400     GO TO   D40-1-CHECK-UPDATE-AND-POP.                          EXEC84.2
158500                                                                  EXEC84.2
158600 D40-20-PROGRAMS-EQUAL.                                           EXEC84.2
158700     MOVE   "Y"   TO WZ-UPDATE-THIS-PROG.                         EXEC84.2
158800     IF      WB-RENUMBER = "R"                                    EXEC84.2
158900             MOVE    "Y"    TO WZ-RESEQUENCE-THIS                 EXEC84.2
159000     ELSE                                                         EXEC84.2
159100             MOVE     SPACE TO WZ-RESEQUENCE-THIS.                EXEC84.2
159200     READ    CONTROL-CARD-FILE INTO WB-CONTROL-DATA               EXEC84.2
159300             AT END  MOVE "Y" TO WZ-END-OF-UPDATES.               EXEC84.2
159400                                                                  EXEC84.2
159500     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
159600             MOVE "Y" TO WZ-END-OF-UPDATES.                       EXEC84.2
159700                                                                  EXEC84.2
159800 D40-EXIT.                                                        EXEC84.2
159900     EXIT.                                                        EXEC84.2
160000                                                                  EXEC84.2
160100/                                                                 EXEC84.2
160200 D42-READ-UPDATE-FILE SECTION.                                    EXEC84.2
160300*============================                                     EXEC84.2
160400 D42-1.                                                           EXEC84.2
160500     READ    CONTROL-CARD-FILE INTO WB-CONTROL-DATA               EXEC84.2
160600             AT END  MOVE   "Y"      TO WZ-END-OF-UPDATES         EXEC84.2
160700                     MOVE   "*START" TO WB-STAR-START             EXEC84.2
160800                     GO TO   D42-EXIT.                            EXEC84.2
160900                                                                  EXEC84.2
161000     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
161100             MOVE   "Y"      TO WZ-END-OF-UPDATES                 EXEC84.2
161200             MOVE   "*START" TO WB-STAR-START.                    EXEC84.2
161300                                                                  EXEC84.2
161400 D42-EXIT.                                                        EXEC84.2
161500     EXIT.                                                        EXEC84.2
161600                                                                  EXEC84.2
161700                                                                  EXEC84.2
161800                                                                  EXEC84.2
161900                                                                  EXEC84.2
162000 D50-PROCESS-COBOL-LINE SECTION.                                  EXEC84.2
162100*==============================                                   EXEC84.2
162200*                                                                 EXEC84.2
162300****************************************************************  EXEC84.2
162400*    THIS IS THE MAIN SECTION FOR PROCESSING THE COBOL SOURCE  *  EXEC84.2
162500*    CODE OF THE POPULATION FILE                               *  EXEC84.2
162600*                                                              *  EXEC84.2
162700*                                                              *  EXEC84.2
162800****************************************************************  EXEC84.2
162900 D50-1-CHECK-FOR-UPDATING.                                        EXEC84.2
163000                                                                  EXEC84.2
163100     IF      WZ-UPDATE-THIS-PROG = "Y"                            EXEC84.2
163200             PERFORM D60-PROCESS-UPDATE-CARD.                     EXEC84.2
163300                                                                  EXEC84.2
163400     IF      WZ-DELETE-FLAG = "Y"                                 EXEC84.2
163500             GO TO   D50-EXIT.                                    EXEC84.2
163600                                                                  EXEC84.2
163700     IF      WY-NO-SOURCE = SPACE                                 EXEC84.2
163800             IF    BLOCK-TYPE NOT = "DATA*"                       EXEC84.2
163900                   PERFORM D80-RAW-LINE.                          EXEC84.2
164000                                                                  EXEC84.2
164100                                                                  EXEC84.2
164200 D50-EXIT.                                                        EXEC84.2
164300     EXIT.                                                        EXEC84.2
164400/                                                                 EXEC84.2
164500 D60-PROCESS-UPDATE-CARD SECTION.                                 EXEC84.2
164600*===============================                                  EXEC84.2
164700*                                                                 EXEC84.2
164800****************************************************************  EXEC84.2
164900*    THIS IS THE MAIN SECTION FOR PROCESSING ANY UPDATE CARDS  *  EXEC84.2
165000*    WHICH EXIST FOR THE CURRENT POPULATION FILE PROGRAM       *  EXEC84.2
165100*    BEING PROCESSED.                                          *  EXEC84.2
165200*                                                              *  EXEC84.2
165300*    PERFORMED BY    D50-PROCESS-COBOL-LINE                    *  EXEC84.2
165400*    PERFORMS        D67-LINE-UPDATE                           *  EXEC84.2
165500*                    D62-SERIES-UPDATE                         *  EXEC84.2
165600****************************************************************  EXEC84.2
165700 D60-1-CHECK-BLOCK-INSERT.                                        EXEC84.2
165800     IF      WB-SEQ-1 = SPACES                                    EXEC84.2
165900             PERFORM D67-LINE-UPDATE                              EXEC84.2
166000             GO TO   D60-30-CHECK-END.                            EXEC84.2
166100                                                                  EXEC84.2
166200 D60-10-CHECK-FOR-SERIES-UPDATE.                                  EXEC84.2
166300     IF      WB-SEQ-1 NOT NUMERIC                                 EXEC84.2
166400             GO TO   D60-30-CHECK-END.                            EXEC84.2
166500                                                                  EXEC84.2
166600     IF      WB-SEQ-2 NUMERIC                                     EXEC84.2
166700             PERFORM D62-SERIES-UPDATE                            EXEC84.2
166800             GO TO   D60-30-CHECK-END                             EXEC84.2
166900     ELSE                                                         EXEC84.2
167000             PERFORM D67-LINE-UPDATE.                             EXEC84.2
167100                                                                  EXEC84.2
167200 D60-30-CHECK-END.                                                EXEC84.2
167300     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
167400             MOVE   "Y" TO WZ-END-OF-UPDATES.                     EXEC84.2
167500                                                                  EXEC84.2
167600                                                                  EXEC84.2
167700 D60-EXIT.                                                        EXEC84.2
167800     EXIT.                                                        EXEC84.2
167900/                                                                 EXEC84.2
168000 D62-SERIES-UPDATE SECTION.                                       EXEC84.2
168100*=========================                                        EXEC84.2
168200*                                                                 EXEC84.2
168300****************************************************************  EXEC84.2
168400*    THIS SECTION WILL PROCESS SERIES INSERTIONS OR DELETIONS  *  EXEC84.2
168500*                                                              *  EXEC84.2
168600*    PERFORMED BY    D60-PROCESS-UPDATE-CARD                   *  EXEC84.2
168700*    PERFORMS        C60-START-CARD                            *  EXEC84.2
168800****************************************************************  EXEC84.2
168900 D62-1-CHECK-FOR-WITHIN-RANGE.                                    EXEC84.2
169000     IF      WC-6 < WB-SEQ-1                                      EXEC84.2
169100         IF WZ-WITHIN-DELETE-SERIES-FLAG = "Y"                    EXEC84.2
169200*  ALLOWS FOR OUT-OF-SEQUENCE LINE NUMBER WITHIN DELETE RANGE     EXEC84.2
169300             GO TO D62-20-NOW-WITHIN-RANGE                        EXEC84.2
169400         END-IF                                                   EXEC84.2
169500             GO TO   D62-EXIT.                                    EXEC84.2
169600                                                                  EXEC84.2
169700     IF      WC-6 NOT > WB-SEQ-2                                  EXEC84.2
169800             GO TO   D62-20-NOW-WITHIN-RANGE.                     EXEC84.2
169900                                                                  EXEC84.2
170000 D62-10-READ-NEXT-UPDATE-CARD.                                    EXEC84.2
170100             MOVE SPACES TO WZ-WITHIN-DELETE-SERIES-FLAG.         EXEC84.2
170200             READ    CONTROL-CARD-FILE INTO WB-CONTROL-DATA       EXEC84.2
170300             AT END  MOVE "Y" TO WZ-END-OF-UPDATES                EXEC84.2
170400                     GO TO   D62-EXIT.                            EXEC84.2
170500                                                                  EXEC84.2
170600     IF      WB-STAR-START = "*START"                             EXEC84.2
170700             PERFORM C60-START-CARD                               EXEC84.2
170800             GO TO   D62-EXIT.                                    EXEC84.2
170900                                                                  EXEC84.2
171000     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
171100             MOVE   "Y" TO WZ-END-OF-UPDATES                      EXEC84.2
171200             GO TO   D62-EXIT.                                    EXEC84.2
171300                                                                  EXEC84.2
171400     IF WB-SEQ-2 NUMERIC                                          EXEC84.2
171500     GO TO   D62-1-CHECK-FOR-WITHIN-RANGE.                        EXEC84.2
171600     PERFORM D67-LINE-UPDATE.                                     EXEC84.2
171700     GO TO D62-EXIT.                                              EXEC84.2
171800                                                                  EXEC84.2
171900 D62-20-NOW-WITHIN-RANGE.                                         EXEC84.2
172000     MOVE   "Y" TO WZ-LINE-UPDATE.                                EXEC84.2
172100     IF      WB-COL-7 = ","                                       EXEC84.2
172200             MOVE "Y" TO WZ-WITHIN-DELETE-SERIES-FLAG             EXEC84.2
172300             ADD     1  TO WZ-LINES-DELETED                       EXEC84.2
172400             MOVE   "Y" TO WZ-DELETE-FLAG                         EXEC84.2
172500             MOVE    SPACES     TO WE-X-CARD                      EXEC84.2
172600             MOVE   "DELETIONS" TO WE-CHANGE-TYPE                 EXEC84.2
172700             IF WB-SEQ-1 = WB-SEQ-2                               EXEC84.2
172800                  MOVE SPACE TO WZ-WITHIN-DELETE-SERIES-FLAG      EXEC84.2
172900             END-IF                                               EXEC84.2
173000             GO TO   D62-EXIT.                                    EXEC84.2
173100                                                                  EXEC84.2
173200     MOVE   "Y"       TO WZ-REPLACE-FLAG.                         EXEC84.2
173300     MOVE    WB-COL-7 TO WC-COL-7.                                EXEC84.2
173400                                                                  EXEC84.2
173500 D62-EXIT.                                                        EXEC84.2
173600     EXIT.                                                        EXEC84.2
173700                                                                  EXEC84.2
173800/                                                                 EXEC84.2
173900 D67-LINE-UPDATE SECTION.                                         EXEC84.2
174000*=======================                                          EXEC84.2
174100*                                                                 EXEC84.2
174200****************************************************************  EXEC84.2
174300*    THIS SECTION PERFORMS SINGLE LINE AMENDMENTS OR DELETIONS *  EXEC84.2
174400*                                                              *  EXEC84.2
174500*    PERFORMED BY    D60-PROCESS-UPDATE-CARD                   *  EXEC84.2
174600*                    D62-SERIES-UPDATE                         *  EXEC84.2
174700*    PERFORMS        C60-START-CARD                            *  EXEC84.2
174800*                    D68-LINE-EQUAL                            *  EXEC84.2
174900*                    D69-LINE-BLANK-OR-LESS                    *  EXEC84.2
175000****************************************************************  EXEC84.2
175100 D67-1-CHECK-FOR-WITHIN-RANGE.                                    EXEC84.2
175200     IF      WC-6  < WB-SEQ-1                                     EXEC84.2
175300             GO TO   D67-EXIT.                                    EXEC84.2
175400                                                                  EXEC84.2
175500 D67-10-LINES-EQUAL-OR-LESS.                                      EXEC84.2
175600     MOVE   "Y" TO WZ-LINE-UPDATE.                                EXEC84.2
175700     IF      WB-COL-7 = ","                                       EXEC84.2
175800             ADD     1  TO WZ-LINES-DELETED                       EXEC84.2
175900             MOVE   "Y" TO WZ-DELETE-FLAG                         EXEC84.2
176000             MOVE    SPACES     TO WE-X-CARD                      EXEC84.2
176100             MOVE   "DELETION"  TO WE-CHANGE-TYPE                 EXEC84.2
176200             GO TO   D67-20-READ-CONTROL.                         EXEC84.2
176300                                                                  EXEC84.2
176400     IF      WC-6  = WB-SEQ-1                                     EXEC84.2
176500             PERFORM D68-LINE-EQUAL                               EXEC84.2
176600             GO TO   D67-EXIT.                                    EXEC84.2
176700                                                                  EXEC84.2
176800     IF      WB-SEQ-1 = SPACES                                    EXEC84.2
176900             PERFORM D69-LINE-BLANK-OR-LESS                       EXEC84.2
177000             GO TO   D67-EXIT.                                    EXEC84.2
177100                                                                  EXEC84.2
177200     IF      WC-6  > WB-SEQ-1                                     EXEC84.2
177300             PERFORM D69-LINE-BLANK-OR-LESS                       EXEC84.2
177400             GO TO   D67-EXIT.                                    EXEC84.2
177500                                                                  EXEC84.2
177600 D67-20-READ-CONTROL.                                             EXEC84.2
177700     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
177800             AT END  MOVE "Y" TO WZ-END-OF-UPDATES.               EXEC84.2
177900     IF WB-STAR-START = "*START"                                  EXEC84.2
178000             PERFORM C60-START-CARD.                              EXEC84.2
178100                                                                  EXEC84.2
178200     IF WB-11 = "*END-UPDATE"                                     EXEC84.2
178300             MOVE "Y" TO WZ-END-OF-UPDATES.                       EXEC84.2
178400                                                                  EXEC84.2
178500 D67-EXIT.                                                        EXEC84.2
178600     EXIT.                                                        EXEC84.2
178700/                                                                 EXEC84.2
178800 D68-LINE-EQUAL SECTION.                                          EXEC84.2
178900*======================                                           EXEC84.2
179000*                                                                 EXEC84.2
179100****************************************************************  EXEC84.2
179200*    THIS SECTION IS PERFORMED WHEN THE SEQUENCE NUMBER OF A   *  EXEC84.2
179300*    SINGLE UPDATE AMEND CARD IS THE SAME AS THE SEQUENCE      *  EXEC84.2
179400*    NUMBER OF THE CURRENT POPULATION FILE LINE BEING PROCESSED*  EXEC84.2
179500****************************************************************  EXEC84.2
179600*                                                                 EXEC84.2
179700 D68-1.                                                           EXEC84.2
179800     MOVE    WB-CONTROL-DATA TO WC-CURRENT-POP-RECORD.            EXEC84.2
179900     ADD     1    TO WZ-LINES-REPLACED.                           EXEC84.2
180000     MOVE   "Y"   TO WZ-REPLACE-FLAG.                             EXEC84.2
180100                                                                  EXEC84.2
180200     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
180300             AT END  MOVE "Y" TO WZ-END-OF-UPDATES.               EXEC84.2
180400                                                                  EXEC84.2
180500     IF      WB-STAR-START = "*START"                             EXEC84.2
180600             PERFORM C60-START-CARD.                              EXEC84.2
180700                                                                  EXEC84.2
180800     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
180900             MOVE   "Y" TO WZ-END-OF-UPDATES.                     EXEC84.2
181000                                                                  EXEC84.2
181100 D68-EXIT.                                                        EXEC84.2
181200     EXIT.                                                        EXEC84.2
181300                                                                  EXEC84.2
181400                                                                  EXEC84.2
181500                                                                  EXEC84.2
181600                                                                  EXEC84.2
181700 D69-LINE-BLANK-OR-LESS SECTION.                                  EXEC84.2
181800*==============================                                   EXEC84.2
181900 D69-1.                                                           EXEC84.2
182000     MOVE    WB-CONTROL-DATA TO WC-CURRENT-POP-RECORD.            EXEC84.2
182100     ADD     1    TO WZ-LINES-INSERTED.                           EXEC84.2
182200     MOVE   "Y"   TO WZ-DONT-READ-POPFILE.                        EXEC84.2
182300     MOVE   "INSERTIONS" TO WE-CHANGE-TYPE.                       EXEC84.2
182400     MOVE    SPACES      TO WE-X-CARD.                            EXEC84.2
182500     READ    CONTROL-CARD-FILE INTO  WB-CONTROL-DATA              EXEC84.2
182600             AT END  MOVE "Y" TO WZ-END-OF-UPDATES.               EXEC84.2
182700                                                                  EXEC84.2
182800     IF      WB-STAR-START = "*START"                             EXEC84.2
182900             PERFORM C60-START-CARD.                              EXEC84.2
183000                                                                  EXEC84.2
183100     IF      WB-11 = "*END-UPDATE"                                EXEC84.2
183200             MOVE   "Y" TO WZ-END-OF-UPDATES.                     EXEC84.2
183300                                                                  EXEC84.2
183400 D69-EXIT.                                                        EXEC84.2
183500     EXIT.                                                        EXEC84.2
183600                                                                  EXEC84.2
183700/                                                                 EXEC84.2
183800 D80-RAW-LINE SECTION.                                            EXEC84.2
183900*====================                                             EXEC84.2
184000*                                                                 EXEC84.2
184100****************************************************************  EXEC84.2
184200*    IF COLUMN 7 OF THE POPULATION FILE PROGRAM CONTAINS A     *  EXEC84.2
184300*    SPECIAL CHARACTER OR COLS. 12-16 CONTAIN X-CARD INFO.,    *  EXEC84.2
184400*    THIS SECTION WILL MAKE THE NECESSARY CHANGES.             *  EXEC84.2
184500*                                                              *  EXEC84.2
184600*    PERFORMED BY    D50-PROCESS-COBOL-LINE                    *  EXEC84.2
184700*    PERFORMS        D82-OPTIONAL-LETTER                       *  EXEC84.2
184800*                    D86-X-CARD-CHECK                          *  EXEC84.2
184900*                    X40-WRITE-SOURCE-FILE                     *  EXEC84.2
185000****************************************************************  EXEC84.2
185100 D80-1-ASTERISK-CHECK.                                            EXEC84.2
185200     IF      WC-COL-7 = "D" GO TO D80-30-PRINT-LINE.              EXEC84.2
185300     IF      WC-COL-7  NOT = "*"                                  EXEC84.2
185400             GO TO   D80-20-ALPHABETIC-CHECK.                     EXEC84.2
185500                                                                  EXEC84.2
185600     IF      WY-REMOVE-COMMENTS = "Y"                             EXEC84.2
185700             ADD     1  TO WZ-COMMENTS-DELETED                    EXEC84.2
185800             MOVE   "Y" TO WZ-NOT-THIS-COMMENT                    EXEC84.2
185900             GO TO   D80-EXIT.                                    EXEC84.2
186000                                                                  EXEC84.2
186100 D80-20-ALPHABETIC-CHECK.                                         EXEC84.2
186200     IF      WC-COL-7 NOT ALPHABETIC                              EXEC84.2
186300             GO TO   D80-30-PRINT-LINE.                           EXEC84.2
186400                                                                  EXEC84.2
186500     IF      WC-COL-7 NOT = SPACE                                 EXEC84.2
186600             PERFORM D82-OPTIONAL-LETTER.                         EXEC84.2
186700                                                                  EXEC84.2
186800 D80-30-PRINT-LINE.                                               EXEC84.2
186900     IF      WZ-DELETE-FLAG = SPACE                               EXEC84.2
187000             PERFORM D85-X-CARD-CHECK.                            EXEC84.2
187100                                                                  EXEC84.2
187200 D80-EXIT.                                                        EXEC84.2
187300     EXIT.                                                        EXEC84.2
187400                                                                  EXEC84.2
187500/                                                                 EXEC84.2
187600 D82-OPTIONAL-LETTER SECTION.                                     EXEC84.2
187700*===========================                                      EXEC84.2
187800*                                                                 EXEC84.2
187900 D82-1-SELECT-OPTIONAL.                                           EXEC84.2
188000     PERFORM D83-CHECK-OPTION-SWITCHES                            EXEC84.2
188100             VARYING SUB4 FROM 1 BY 1                             EXEC84.2
188200               UNTIL SUB4 > 26.                                   EXEC84.2
188300                                                                  EXEC84.2
188400     IF      WZ-OPTIONAL-SELECTED = "Y"                           EXEC84.2
188500             MOVE    SPACE TO WC-COL-7                            EXEC84.2
188600             GO TO   D82-EXIT.                                    EXEC84.2
188700                                                                  EXEC84.2
188800 D82-10-SAVE-OPTIONAL.                                            EXEC84.2
188900     ADD     1    TO WZ-CODE-REMOVED.                             EXEC84.2
189000     IF      WY-KILL-DELETIONS = "Y"                              EXEC84.2
189100             MOVE   "Y" TO WZ-DELETE-FLAG                         EXEC84.2
189200     ELSE                                                         EXEC84.2
189300             MOVE    WC-COL-7 TO WC-COL-8                         EXEC84.2
189400             MOVE   "*"       TO WC-COL-7.                        EXEC84.2
189500                                                                  EXEC84.2
189600 D82-EXIT.                                                        EXEC84.2
189700     EXIT.                                                        EXEC84.2
189800                                                                  EXEC84.2
189900                                                                  EXEC84.2
190000                                                                  EXEC84.2
190100                                                                  EXEC84.2
190200                                                                  EXEC84.2
190300 D83-CHECK-OPTION-SWITCHES SECTION.                               EXEC84.2
190400*=================================                                EXEC84.2
190500*                                                                 EXEC84.2
190600 D83-1-CHECK-SWITCH.                                              EXEC84.2
190700     IF      WY-OPT-SW (SUB4) = WC-COL-7                          EXEC84.2
190800             MOVE    99 TO SUB4                                   EXEC84.2
190900             MOVE   "Y" TO WZ-OPTIONAL-SELECTED.                  EXEC84.2
191000                                                                  EXEC84.2
191100 D83-EXIT.                                                        EXEC84.2
191200     EXIT.                                                        EXEC84.2
191300/                                                                 EXEC84.2
191400 D85-X-CARD-CHECK SECTION.                                        EXEC84.2
191500*========================                                         EXEC84.2
191600*                                                                 EXEC84.2
191700****************************************************************  EXEC84.2
191800*    WHEN "XXXX" IS ENCOUNTERED IN COLUMNS 12 - 15 THIS        *  EXEC84.2
191900*    SECTION EITHER SUBSTITUTES THE IMPLEMENTOR ASSIGNED TEXT  *  EXEC84.2
192000*    OR MOVES THE PROGRAM NAME TO AN IMPLEMENTOR SPECIFIED     *  EXEC84.2
192100*    POSITION IN THE PROGRAM CODE.                             *  EXEC84.2
192200*                                                              *  EXEC84.2
192300*    PERFORMED BY    D80-RAW-LINE                              *  EXEC84.2
192400*    PERFORMS        D87-PROGRAM-NAME                          *  EXEC84.2
192500*                    D86-REMOVE-FULL-STOP                      *  EXEC84.2
192600*                    D86-INSERT-FULL-STOP                      *  EXEC84.2
192700****************************************************************  EXEC84.2
192800 D85-1-CHECK-FOR-XXXXX.                                           EXEC84.2
192900     IF      WC-12-15 NOT = "XXXX"                                EXEC84.2
193000             GO TO   D85-EXIT.                                    EXEC84.2
193100     IF      WC-17-19 NOT NUMERIC                                 EXEC84.2
193200* XXXXX ENTRY IN POPULATION FILE IN ERROR.                        EXEC84.2
193300             GO TO D85-EXIT.                                      EXEC84.2
193400                                                                  EXEC84.2
193500 D85-10-SUBSTITUTION-REQUIRED.                                    EXEC84.2
193600     MOVE    WC-17-19 TO SUB1.                                    EXEC84.2
193700     MOVE    WX-X-CARD (SUB1) TO WZ-X-CARD.                       EXEC84.2
193800     IF      WX-PROG-POS (SUB1) NOT = ZERO                        EXEC84.2
193900             PERFORM D88-PROGRAM-NAME                             EXEC84.2
194000             GO TO   D85-EXIT.                                    EXEC84.2
194100                                                                  EXEC84.2
194200 D85-20-TEXT-SUBSTITUTION.                                        EXEC84.2
194300     IF      WC-20 = SPACE                                        EXEC84.2
194400             PERFORM D86-REMOVE-FULL-STOP                         EXEC84.2
194500             VARYING SUB4 FROM 60 BY -1                           EXEC84.2
194600               UNTIL SUB4 < 1                                     EXEC84.2
194700             MOVE    WZ-X-CARD TO WC-SUB-DATA                     EXEC84.2
194800             GO TO   D85-EXIT.                                    EXEC84.2
194900                                                                  EXEC84.2
195000     IF      WC-20 = "."                                          EXEC84.2
195100             PERFORM D87-INSERT-FULL-STOP                         EXEC84.2
195200             VARYING SUB4 FROM 60 BY -1                           EXEC84.2
195300               UNTIL SUB4 < 1                                     EXEC84.2
195400             MOVE    WZ-X-CARD  TO WC-SUB-DATA.                   EXEC84.2
195500 D85-EXIT.                                                        EXEC84.2
195600     EXIT.                                                        EXEC84.2
195700                                                                  EXEC84.2
195800/                                                                 EXEC84.2
195900 D86-REMOVE-FULL-STOP SECTION.                                    EXEC84.2
196000*============================                                     EXEC84.2
196100*                                                                 EXEC84.2
196200 D86-1.                                                           EXEC84.2
196300     IF      WZ-X-CHAR (SUB4) = SPACE                             EXEC84.2
196400             GO TO   D86-EXIT.                                    EXEC84.2
196500                                                                  EXEC84.2
196600     IF      WZ-X-CHAR (SUB4) = "."                               EXEC84.2
196700             MOVE    SPACE TO WZ-X-CHAR (SUB4)                    EXEC84.2
196800             MOVE    ZERO  TO SUB4                                EXEC84.2
196900     ELSE                                                         EXEC84.2
197000             MOVE    ZERO  TO SUB4.                               EXEC84.2
197100                                                                  EXEC84.2
197200 D86-EXIT.                                                        EXEC84.2
197300     EXIT.                                                        EXEC84.2
197400                                                                  EXEC84.2
197500                                                                  EXEC84.2
197600                                                                  EXEC84.2
197700                                                                  EXEC84.2
197800                                                                  EXEC84.2
197900 D87-INSERT-FULL-STOP SECTION.                                    EXEC84.2
198000*============================                                     EXEC84.2
198100*                                                                 EXEC84.2
198200 D87-1.                                                           EXEC84.2
198300     IF      WZ-X-CHAR (SUB4) = SPACE                             EXEC84.2
198400             GO TO   D87-EXIT.                                    EXEC84.2
198500                                                                  EXEC84.2
198600     IF      WZ-X-CHAR (SUB4) = "."                               EXEC84.2
198700             MOVE    ZERO  TO SUB4                                EXEC84.2
198800     ELSE                                                         EXEC84.2
198900             ADD     1     TO SUB4                                EXEC84.2
199000             MOVE   "."    TO WZ-X-CHAR (SUB4)                    EXEC84.2
199100             MOVE    ZERO  TO SUB4.                               EXEC84.2
199200                                                                  EXEC84.2
199300 D87-EXIT.                                                        EXEC84.2
199400     EXIT.                                                        EXEC84.2
199500/                                                                 EXEC84.2
199600 D88-PROGRAM-NAME SECTION.                                        EXEC84.2
199700*========================                                         EXEC84.2
199800*                                                                 EXEC84.2
199900 D88-1.                                                           EXEC84.2
200000     MOVE    WX-PROG-POS (SUB1) TO SUB2.                          EXEC84.2
200100     MOVE    SPACE TO WZ-MAIN-PROG-FLAG.                          EXEC84.2
200200     MOVE    SPACE TO WZ-FULL-STOP.                               EXEC84.2
200300     IF      WC-20 = "."                                          EXEC84.2
200400             MOVE    "Y" TO WZ-FULL-STOP.                         EXEC84.2
200500                                                                  EXEC84.2
200600     IF      WX-X-CHAR (SUB1 SUB2) = "J"                          EXEC84.2
200700             MOVE WZ-CURRENT-MAIN-PROG TO WZ-PROG-BREAK           EXEC84.2
200800             MOVE   "Y"    TO WZ-MAIN-PROG-FLAG                   EXEC84.2
200900     ELSE                                                         EXEC84.2
201000             MOVE WZ-CURRENT-POP-PROG TO WZ-PROG-BREAK.           EXEC84.2
201100                                                                  EXEC84.2
201200     IF      WZ-CURRENT-MAIN-PROG = SPACE                         EXEC84.2
201300             MOVE    SPACE TO WZ-MAIN-PROG-FLAG.                  EXEC84.2
201400                                                                  EXEC84.2
201500     MOVE    1 TO SUB4.                                           EXEC84.2
201600     PERFORM D89-MOVE-PROGRAM-NAME 6 TIMES.                       EXEC84.2
201700                                                                  EXEC84.2
201800     IF      WZ-FULL-STOP = "Y"                                   EXEC84.2
201900             PERFORM D87-INSERT-FULL-STOP                         EXEC84.2
202000             VARYING SUB4 FROM 60 BY -1                           EXEC84.2
202100               UNTIL SUB4 < 1                                     EXEC84.2
202200     ELSE                                                         EXEC84.2
202300             PERFORM D86-REMOVE-FULL-STOP                         EXEC84.2
202400             VARYING SUB4 FROM 60 BY -1                           EXEC84.2
202500               UNTIL SUB4 < 1.                                    EXEC84.2
202600                                                                  EXEC84.2
202700     MOVE    WZ-X-CARD  TO WC-SUB-DATA.                           EXEC84.2
202800                                                                  EXEC84.2
202900 D88-EXIT.                                                        EXEC84.2
203000     EXIT.                                                        EXEC84.2
203100                                                                  EXEC84.2
203200                                                                  EXEC84.2
203300                                                                  EXEC84.2
203400                                                                  EXEC84.2
203500 D89-MOVE-PROGRAM-NAME SECTION.                                   EXEC84.2
203600*=============================                                    EXEC84.2
203700*                                                                 EXEC84.2
203800 D89-1.                                                           EXEC84.2
203900*    IF      WZ-MAIN-PROG-FLAG = "Y"                              EXEC84.2
204000*            MOVE    WZ-MAIN-PROG-CHAR (SUB4)                     EXEC84.2
204100*                 TO WZ-X-CHAR (SUB2)                             EXEC84.2
204200*    ELSE                                                         EXEC84.2
204300*            MOVE    WZ-UPD-PROG-CHAR  (SUB4)                     EXEC84.2
204400*                 TO WZ-X-CHAR (SUB2).                            EXEC84.2
204500                                                                  EXEC84.2
204600     MOVE    WZ-1CHAR(SUB4) TO WZ-X-CHAR(SUB2).                   EXEC84.2
204700                                                                  EXEC84.2
204800     IF      SUB4 = 6 AND WZ-X-CHAR(SUB2) = SPACE                 EXEC84.2
204900             ADD 1 SUB2 GIVING SUB4                               EXEC84.2
205000             PERFORM D89-2-CLOSE-SPACE UNTIL WZ-X-CHAR(SUB4) =    EXEC84.2
205100                  SPACE                                           EXEC84.2
205200             MOVE SPACE TO WZ-X-CHAR(SUB2).                       EXEC84.2
205300                                                                  EXEC84.2
205400     ADD     1 TO SUB2.                                           EXEC84.2
205500     ADD     1 TO SUB4.                                           EXEC84.2
205600                                                                  EXEC84.2
205700 D89-EXIT.                                                        EXEC84.2
205800     EXIT.                                                        EXEC84.2
205900                                                                  EXEC84.2
206000 D89-2-CLOSE-SPACE SECTION.                                       EXEC84.2
206100 D89-2-1.                                                         EXEC84.2
206200                                                                  EXEC84.2
206300     MOVE WZ-X-CHAR(SUB4) TO WZ-X-CHAR(SUB2).                     EXEC84.2
206400     ADD 1 TO SUB2 SUB4.                                          EXEC84.2
206500                                                                  EXEC84.2
206600/                                                                 EXEC84.2
206700 D90-PROCESS-END-OF SECTION.                                      EXEC84.2
206800*==========================                                       EXEC84.2
206900*                                                                 EXEC84.2
207000****************************************************************  EXEC84.2
207100*    WHEN A "*END-OF"CARD IS ENCOUNTERED THIS SECTION PERFORMS *  EXEC84.2
207200*    THE END-OF-PROGRAMS ACTIONS.                              *  EXEC84.2
207300*                                                              *  EXEC84.2
207400*    PERFORMED BY    D20-PROCESS-POPFILE                       *  EXEC84.2
207500*    PERFORMS        X10-WRITE-UPDATED-POP                     *  EXEC84.2
207600****************************************************************  EXEC84.2
207700 D90-1-CHECK-HEADER-TYPE.                                         EXEC84.2
207800     IF      WY-KILL-DELETIONS = "Y"                              EXEC84.2
207900             MOVE   "DELETED"    TO WA-OPTIONAL-CODE              EXEC84.2
208000     ELSE                                                         EXEC84.2
208100             MOVE   "UNUSED "    TO WA-OPTIONAL-CODE.             EXEC84.2
208200                                                                  EXEC84.2
208300     MOVE    WZ-CURRENT-POP-PROG TO WA-CURRENT-PROG               EXEC84.2
208400     MOVE    WZ-LINES-COBOL      TO WA-LINES-COBOL                EXEC84.2
208500     MOVE    WZ-LINES-INSERTED   TO WA-LINES-INSERTED             EXEC84.2
208600     MOVE    WZ-LINES-REPLACED   TO WA-LINES-REPLACED             EXEC84.2
208700     MOVE    WZ-LINES-DELETED    TO WA-LINES-DELETED              EXEC84.2
208800     MOVE    WZ-CODE-REMOVED     TO WA-CODE-REMOVED               EXEC84.2
208900     MOVE    WZ-COMMENTS-DELETED TO WA-COMMENTS-DEL               EXEC84.2
209000     MOVE    WA-ACCT-LINE-1      TO PRINT-DATA                    EXEC84.2
209100     PERFORM X20-PRINT-DETAIL                                     EXEC84.2
209200     MOVE    WA-ACCT-LINE-2      TO PRINT-DATA                    EXEC84.2
209300     PERFORM X20-PRINT-DETAIL                                     EXEC84.2
209400     MOVE    WA-ACCT-LINE-3      TO PRINT-DATA                    EXEC84.2
209500     PERFORM X20-PRINT-DETAIL                                     EXEC84.2
209600                                                                  EXEC84.2
209700     MOVE    SPACES  TO WZ-CURRENT-POP-PROG.                      EXEC84.2
209800*    CLOSE SOURCE-COBOL-PROGRAMS.                                 EXEC84.2
209900                                                                  EXEC84.2
210000 D90-EXIT.                                                        EXEC84.2
210100     EXIT.                                                        EXEC84.2
210200                                                                  EXEC84.2
210300                                                                  EXEC84.2
210400 D100-PROCESS-DATA.                                               EXEC84.2
210500     EXIT.                                                        EXEC84.2
210600                                                                  EXEC84.2
210700/                                                                 EXEC84.2
210800 E10-TERMINATE SECTION.                                           EXEC84.2
210900*=====================                                            EXEC84.2
211000*                                                                 EXEC84.2
211100****************************************************************  EXEC84.2
211200*    THIS SECTION CLOSES ANY OPEN FILES                        *  EXEC84.2
211300*                                                              *  EXEC84.2
211400*                                                              *  EXEC84.2
211500****************************************************************  EXEC84.2
211600 E10-1.                                                           EXEC84.2
211700     CLOSE   PRINT-FILE.                                          EXEC84.2
211800                                                                  EXEC84.2
211900     CLOSE   CONTROL-CARD-FILE.                                   EXEC84.2
212000                                                                  EXEC84.2
212100     IF      WY-NEW-POP  = "Y"                                    EXEC84.2
212200             CLOSE   UPDATED-POPULATION-FILE.                     EXEC84.2
212300                                                                  EXEC84.2
212400***************************************************************** EXEC84.2
212500     IF      WY-NO-SOURCE = SPACE                                 EXEC84.2
212600             CLOSE   SOURCE-COBOL-PROGRAMS.                       EXEC84.2
212700                                                                  EXEC84.2
212800     CLOSE   POPULATION-FILE.                                     EXEC84.2
212900     STOP    RUN.                                                 EXEC84.2
213000 E20-EXIT.                                                        EXEC84.2
213100     EXIT.                                                        EXEC84.2
213200                                                                  EXEC84.2
213300/                                                                 EXEC84.2
213400 X10-WRITE-UPDATED-POPFILE SECTION.                               EXEC84.2
213500*=================================                                EXEC84.2
213600*                                                                 EXEC84.2
213700 X10-1-CHECK-FOR-NC114.                                           EXEC84.2
213800*    N.B.                                                         EXEC84.2
213900*    NC114A HAS MISCELLANEOUS CHARACTERS IN COLUMNS 1-6 AND SO    EXEC84.2
214000*    SHOULD NOT BE RESEQUENCED BY THIS EXECUTIVE.                 EXEC84.2
214100                                                                  EXEC84.2
214200     IF      WZ-CURRENT-POP-PROG = "NC114M"                       EXEC84.2
214300             GO TO   X10-30-WRITE-FILE.                           EXEC84.2
214400                                                                  EXEC84.2
214500 X10-20-RENUMBER-CHECK.                                           EXEC84.2
214600     IF      WC-STAR = "*"                                        EXEC84.2
214700             GO TO   X10-30-WRITE-FILE.                           EXEC84.2
214800     IF      WZ-RESEQUENCE-THIS = "Y"                             EXEC84.2
214900             ADD     100 TO WZ-SEQ-NO                             EXEC84.2
215000             MOVE    WZ-SEQ-NO TO WC-6.                           EXEC84.2
215100                                                                  EXEC84.2
215200 X10-30-WRITE-FILE.                                               EXEC84.2
215300     MOVE    WC-CURRENT-POP-RECORD TO UD-SOURCE-OUT (SUB5).       EXEC84.2
215400     ADD     1       TO SUB5.                                     EXEC84.2
215500     IF      SUB5    GREATER 30                                   EXEC84.2
215600             WRITE   UPDATED-SOURCE-OUT-2400                      EXEC84.2
215700             MOVE    1 TO SUB5                                    EXEC84.2
215800             MOVE   SPACE TO UPDATED-SOURCE-OUT-2400.             EXEC84.2
215900                                                                  EXEC84.2
216000                                                                  EXEC84.2
216100 X10-EXIT.                                                        EXEC84.2
216200     EXIT.                                                        EXEC84.2
216300                                                                  EXEC84.2
216400                                                                  EXEC84.2
216500/                                                                 EXEC84.2
216600 X20-PRINT-DETAIL SECTION.                                        EXEC84.2
216700*========================                                         EXEC84.2
216800*                                                                 EXEC84.2
216900 X20-1.                                                           EXEC84.2
217000                                                                  EXEC84.2
217100     IF      WZ-LINE-CT > 50                                      EXEC84.2
217200             MOVE    PRINT-REC    TO WZ-PRINT-HOLD                EXEC84.2
217300             ADD     1            TO WZ-PAGE-CT                   EXEC84.2
217400             MOVE    WZ-PAGE-CT   TO WA-PAGE-CT                   EXEC84.2
217500             MOVE    WA-TOP-OF-PAGE-LINE                          EXEC84.2
217600                                  TO PRINT-REC                    EXEC84.2
217700             WRITE   PRINT-REC AFTER PAGE                         EXEC84.2
217800             MOVE    SPACE        TO PRINT-REC                    EXEC84.2
217900             WRITE   PRINT-REC                                    EXEC84.2
218000             MOVE    WZ-PRINT-HOLD TO PRINT-REC                   EXEC84.2
218100             MOVE    ZERO TO WZ-LINE-CT.                          EXEC84.2
218200     ADD     1 TO WZ-LINE-CT.                                     EXEC84.2
218300                                                                  EXEC84.2
218400 X20-2.                                                           EXEC84.2
218500     WRITE   PRINT-REC AFTER 1.                                   EXEC84.2
218600     MOVE    SPACE TO PRINT-REC.                                  EXEC84.2
218700                                                                  EXEC84.2
218800 X20-EXIT.                                                        EXEC84.2
218900     EXIT.                                                        EXEC84.2
219000/                                                                 EXEC84.2
219100 X30-READ-POPFILE SECTION.                                        EXEC84.2
219200*========================                                         EXEC84.2
219300 X30-1.                                                           EXEC84.2
219400     IF      WZ-DONT-READ-POPFILE = "Y"                           EXEC84.2
219500             MOVE    SPACE TO WZ-DONT-READ-POPFILE                EXEC84.2
219600             GO TO   X30-EXIT.                                    EXEC84.2
219700                                                                  EXEC84.2
219800*    IF      SUB1 GREATER THAN 30                                 EXEC84.2
219900     READ    POPULATION-FILE INTO WC-CURRENT-POP-RECORD           EXEC84.2
220000             AT END  MOVE  "Y"       TO WZ-END-OF-POPFILE         EXEC84.2
220100                     MOVE  "*HEADER" TO WC-STAR-HEADER            EXEC84.2
220200                     GO TO  X30-EXIT.                             EXEC84.2
220300     MOVE    WC-CURRENT-POP-RECORD   TO WZ-SAVE-POP-RECORD.       EXEC84.2
220400                                                                  EXEC84.2
220500     IF      WC-STAR = "*"                                        EXEC84.2
220600             IF      WZ-CURRENT-HEADER = "COBOL"                  EXEC84.2
220700     IF WC-1-5 = "ABCDE"                                          EXEC84.2
220800          ADD 1 TO WZ-LINES-COBOL                                 EXEC84.2
220900     ELSE                                                         EXEC84.2
221000                     IF      WC-1-5 = "*/+(>"                     EXEC84.2
221100                             ADD     1 TO WZ-LINES-COBOL          EXEC84.2
221200                     ELSE                                         EXEC84.2
221300                     IF      WC-1-5 = "999-9"                     EXEC84.2
221400                             ADD     1 TO WZ-LINES-COBOL          EXEC84.2
221500                     ELSE                                         EXEC84.2
221600                     IF      WC-1-5 = "Z=.,;"                     EXEC84.2
221700                             ADD     1 TO WZ-LINES-COBOL          EXEC84.2
221800                     ELSE                                         EXEC84.2
221900                     IF      WC-1-5 = ")14$ "                     EXEC84.2
222000                             ADD     1 TO WZ-LINES-COBOL          EXEC84.2
222100                     ELSE                                         EXEC84.2
222200                     IF      WC-1-5 = "  23 "                     EXEC84.2
222300                             ADD     1 TO WZ-LINES-COBOL          EXEC84.2
222400                     ELSE                                         EXEC84.2
222500                             NEXT    SENTENCE                     EXEC84.2
222600             ELSE                                                 EXEC84.2
222700                     NEXT    SENTENCE                             EXEC84.2
222800     ELSE                                                         EXEC84.2
222900             IF      WZ-CURRENT-HEADER = "COBOL"                  EXEC84.2
223000                     ADD     1    TO WZ-LINES-COBOL.              EXEC84.2
223100                                                                  EXEC84.2
223200*    MOVE SOURCE-IN (SUB1) TO WC-CURRENT-POP-RECORD.              EXEC84.2
223300*    ADD     1 TO SUB1.                                           EXEC84.2
223400                                                                  EXEC84.2
223500                                                                  EXEC84.2
223600 X30-EXIT.                                                        EXEC84.2
223700     EXIT.                                                        EXEC84.2
223800                                                                  EXEC84.2
223900                                                                  EXEC84.2
224000                                                                  EXEC84.2
224100                                                                  EXEC84.2
224200                                                                  EXEC84.2
224300 X40-WRITE-SOURCE-FILE SECTION.                                   EXEC84.2
224400*= ===========================                                    EXEC84.2
224500 X40-1.                                                           EXEC84.2
224600     MOVE    SPACES  TO WD-1.                                     EXEC84.2
224700*    IF    BLOCK-TYPE NOT = "DATA*"                               EXEC84.2
224800*          MOVE SPACES TO WC-73-80.                               EXEC84.2
224900     IF      WC-STAR = "*"                                        EXEC84.2
225000             IF      WC-1-5 = "*/+(>"                             EXEC84.2
225100                     WRITE   CT-OUT FROM WC-CURRENT-POP-RECORD    EXEC84.2
225200             ELSE                                                 EXEC84.2
225300                     MOVE    WC-CURRENT-POP-RECORD TO WD-HEADER   EXEC84.2
225400                     WRITE   CT-OUT FROM WD-SOURCE-REC            EXEC84.2
225500     ELSE                                                         EXEC84.2
225600             WRITE   CT-OUT  FROM WC-CURRENT-POP-RECORD.          EXEC84.2
225700                                                                  EXEC84.2
225800 X40-EXIT.                                                        EXEC84.2
225900     EXIT.                                                        EXEC84.2
226000                                                                  EXEC84.2
      *END-OF,EXEC85                                                                  
