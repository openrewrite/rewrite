      *HEADER,COBOL,DB204A                                                            
000100 IDENTIFICATION DIVISION.                                         DB2044.2
000200 PROGRAM-ID.                                                      DB2044.2
000300     DB204A.                                                      DB2044.2
000400 AUTHOR.                                                          DB2044.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB2044.2
000600 INSTALLATION.                                                    DB2044.2
000700     GENERAL SERVICES ADMINISTRATION                              DB2044.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB2044.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB2044.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB2044.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB2044.2
001200                                                                  DB2044.2
001300     PHONE   (703) 756-6153                                       DB2044.2
001400                                                                  DB2044.2
001500     " HIGH       ".                                              DB2044.2
001600 DATE-WRITTEN.                                                    DB2044.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB2044.2
001800     CREATION DATE     /    VALIDATION DATE                       DB2044.2
001900     "4.2 ".                                                      DB2044.2
002000 SECURITY.                                                        DB2044.2
002100     NONE.                                                        DB2044.2
002200*                                                                 DB2044.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB2044.2
002400*                                                                 DB2044.2
002500*                       PROGRAM ABSTRACT                          DB2044.2
002600*                                                                 DB2044.2
002700*    DB204A TESTS THE CAPABILITY OF THE DEBUG MODULE TO HANDLE A  DB2044.2
002800*    DEBUGGING PROCEDURE WHICH IS MONITORING A "MERGE OUTPUT"     DB2044.2
002900*    PROCEDURE.  THIS PROGRAM IS TO BE COMPILED AND EXECUTED      DB2044.2
003000*    WITH BOTH COMPILE AND OBJECT TIME DEBUGGING SWITCHES ON.     DB2044.2
003100*    THE DEBUGGING PROCEDURE SHOULD BE INCLUDED IN COMPILATION    DB2044.2
003200*    AND GENERATE CODE.  DURING EXECUTION, TWO SEQUENTIAL FILES   DB2044.2
003300*    ARE CREATED WITH EACH CONTAINING 80-CHARACTER RECORDS IN     DB2044.2
003400*    SORTED ORDER.  THE TWO FILES ARE THEN MERGED.  EXECUTION     DB2044.2
003500*    OF THE MERGE OPERATION SHOULD TRIGGER THE DEBUGGING PRO-     DB2044.2
003600*    CEDURE LINKED TO THE MERGE OUTPUT PROCEDURE-NAME.            DB2044.2
003700*                                                                 DB2044.2
003800*                                                                 DB2044.2
003900*                                                                 DB2044.2
004000 ENVIRONMENT DIVISION.                                            DB2044.2
004100 CONFIGURATION SECTION.                                           DB2044.2
004200 SOURCE-COMPUTER.                                                 DB2044.2
004300     XXXXX082                                                     DB2044.2
004400         WITH DEBUGGING MODE.                                     DB2044.2
004500 OBJECT-COMPUTER.                                                 DB2044.2
004600     XXXXX083.                                                    DB2044.2
004700 INPUT-OUTPUT SECTION.                                            DB2044.2
004800 FILE-CONTROL.                                                    DB2044.2
004900     SELECT PRINT-FILE ASSIGN TO                                  DB2044.2
005000     XXXXX055.                                                    DB2044.2
005100     SELECT SQ-FS1 ASSIGN TO                                      DB2044.2
005200     XXXXX014.                                                    DB2044.2
005300     SELECT SQ-FS2  ASSIGN TO                                     DB2044.2
005400     XXXXX015.                                                    DB2044.2
005500     SELECT SQ-FS3  ASSIGN TO                                     DB2044.2
005600     XXXXX016.                                                    DB2044.2
005700     SELECT ST-FS4  ASSIGN TO                                     DB2044.2
005800     XXXXX027.                                                    DB2044.2
005900 DATA DIVISION.                                                   DB2044.2
006000 FILE SECTION.                                                    DB2044.2
006100 FD  PRINT-FILE                                                   DB2044.2
006200     LABEL RECORDS                                                DB2044.2
006300     XXXXX084                                                     DB2044.2
006400     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB2044.2
006500 01  PRINT-REC PICTURE X(120).                                    DB2044.2
006600 01  DUMMY-RECORD PICTURE X(120).                                 DB2044.2
006700 FD  SQ-FS1                                                       DB2044.2
006800C    VALUE OF                                                     DB2044.2
006900C    XXXXX074                                                     DB2044.2
007000*      XXXXX074  REPLACE WITH IMPLEMENTOR NAME (*OPT C ONLY)      DB2044.2
007100C    IS                                                           DB2044.2
007200C    XXXXX075                                                     DB2044.2
007300*      XXXXX075  REPLACE WITH VALUE CLAUSE OBJECT (*OPT C ONLY)   DB2044.2
007400*    XXXXX069                                                     DB2044.2
007500*      XXXXX069  REPLACE WITH ADDITIONAL INFO (*OPT G ONLY)       DB2044.2
007600     LABEL RECORDS ARE STANDARD.                                  DB2044.2
007700 01  REC-1 PIC X(120).                                            DB2044.2
007800 FD  SQ-FS2                                                       DB2044.2
007900C    VALUE OF                                                     DB2044.2
008000C    XXXXX074                                                     DB2044.2
008100*      XXXXX074  REPLACE WITH IMPLEMENTOR NAME (*OPT C ONLY)      DB2044.2
008200C    IS                                                           DB2044.2
008300C    XXXXX076                                                     DB2044.2
008400*      XXXXX076  REPLACE WITH VALUE CLAUSE OBJECT (*OPT C ONLY)   DB2044.2
008500*    XXXXX069                                                     DB2044.2
008600*      XXXXX069  REPLACE WITH ADDITIONAL INFO (*OPT G ONLY)       DB2044.2
008700     LABEL RECORDS ARE STANDARD.                                  DB2044.2
008800 01  REC-2 PIC X(120).                                            DB2044.2
008900 FD  SQ-FS3                                                       DB2044.2
009000C    VALUE OF                                                     DB2044.2
009100C    XXXXX074                                                     DB2044.2
009200*      XXXXX074  REPLACE WITH IMPLEMENTOR NAME (*OPT C ONLY)      DB2044.2
009300C    IS                                                           DB2044.2
009400C    XXXXX077                                                     DB2044.2
009500*      XXXXX077  REPLACE WITH VALUE CLAUSE OBJECT (*OPT C ONLY)   DB2044.2
009600*    XXXXX069                                                     DB2044.2
009700*      XXXXX069  REPLACE WITH ADDITIONAL INFO (*OPT G ONLY)       DB2044.2
009800     LABEL RECORDS ARE STANDARD.                                  DB2044.2
009900 01  REC-3 PIC X(120).                                            DB2044.2
010000 SD  ST-FS4.                                                      DB2044.2
010100 01  REC-4.                                                       DB2044.2
010200     02  FILLER PIC X(34).                                        DB2044.2
010300     02  SORT-KEY PIC X(6).                                       DB2044.2
010400     02  FILLER PIC X(80).                                        DB2044.2
010500 WORKING-STORAGE SECTION.                                         DB2044.2
010600 01  ITEM-1.                                                      DB2044.2
010700     02  KEY-1 PIC 99.                                            DB2044.2
010800     02  LINE-1 PIC X(6).                                         DB2044.2
010900     02  NAME-1 PIC X(30).                                        DB2044.2
011000     02  UNQUAL-NAME-1 PIC X(30).                                 DB2044.2
011100     02  CONTENTS-1 PIC X(30).                                    DB2044.2
011200 01  FILE-RECORD-INFORMATION-REC.                                 DB2044.2
011300     03 FILE-RECORD-INFO-SKELETON.                                DB2044.2
011400        05 FILLER                 PICTURE X(48)       VALUE       DB2044.2
011500             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  DB2044.2
011600        05 FILLER                 PICTURE X(46)       VALUE       DB2044.2
011700             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    DB2044.2
011800        05 FILLER                 PICTURE X(26)       VALUE       DB2044.2
011900             ",LFIL=000000,ORG=  ,LBLR= ".                        DB2044.2
012000        05 FILLER                 PICTURE X(37)       VALUE       DB2044.2
012100             ",RECKEY=                             ".             DB2044.2
012200        05 FILLER                 PICTURE X(38)       VALUE       DB2044.2
012300             ",ALTKEY1=                             ".            DB2044.2
012400        05 FILLER                 PICTURE X(38)       VALUE       DB2044.2
012500             ",ALTKEY2=                             ".            DB2044.2
012600        05 FILLER                 PICTURE X(7)        VALUE SPACE.DB2044.2
012700     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              DB2044.2
012800        05 FILE-RECORD-INFO-P1-120.                               DB2044.2
012900           07 FILLER              PIC X(5).                       DB2044.2
013000           07 XFILE-NAME           PIC X(6).                      DB2044.2
013100           07 FILLER              PIC X(8).                       DB2044.2
013200           07 XRECORD-NAME         PIC X(6).                      DB2044.2
013300           07 FILLER              PIC X(1).                       DB2044.2
013400           07 REELUNIT-NUMBER     PIC 9(1).                       DB2044.2
013500           07 FILLER              PIC X(7).                       DB2044.2
013600           07 XRECORD-NUMBER       PIC 9(6).                      DB2044.2
013700           07 FILLER              PIC X(6).                       DB2044.2
013800           07 UPDATE-NUMBER       PIC 9(2).                       DB2044.2
013900           07 FILLER              PIC X(5).                       DB2044.2
014000           07 ODO-NUMBER          PIC 9(4).                       DB2044.2
014100           07 FILLER              PIC X(5).                       DB2044.2
014200           07 XPROGRAM-NAME        PIC X(5).                      DB2044.2
014300           07 FILLER              PIC X(7).                       DB2044.2
014400           07 XRECORD-LENGTH       PIC 9(6).                      DB2044.2
014500           07 FILLER              PIC X(7).                       DB2044.2
014600           07 CHARS-OR-RECORDS    PIC X(2).                       DB2044.2
014700           07 FILLER              PIC X(1).                       DB2044.2
014800           07 XBLOCK-SIZE          PIC 9(4).                      DB2044.2
014900           07 FILLER              PIC X(6).                       DB2044.2
015000           07 RECORDS-IN-FILE     PIC 9(6).                       DB2044.2
015100           07 FILLER              PIC X(5).                       DB2044.2
015200           07 XFILE-ORGANIZATION   PIC X(2).                      DB2044.2
015300           07 FILLER              PIC X(6).                       DB2044.2
015400           07 XLABEL-TYPE          PIC X(1).                      DB2044.2
015500        05 FILE-RECORD-INFO-P121-240.                             DB2044.2
015600           07 FILLER              PIC X(8).                       DB2044.2
015700           07 XRECORD-KEY          PIC X(29).                     DB2044.2
015800           07 FILLER              PIC X(9).                       DB2044.2
015900           07 ALTERNATE-KEY1      PIC X(29).                      DB2044.2
016000           07 FILLER              PIC X(9).                       DB2044.2
016100           07 ALTERNATE-KEY2      PIC X(29).                      DB2044.2
016200           07 FILLER              PIC X(7).                       DB2044.2
016300 01  TEST-RESULTS.                                                DB2044.2
016400     02 FILLER                    PICTURE X VALUE SPACE.          DB2044.2
016500     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB2044.2
016600     02 FILLER                    PICTURE X VALUE SPACE.          DB2044.2
016700     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB2044.2
016800     02 FILLER                    PICTURE X  VALUE SPACE.         DB2044.2
016900     02  PAR-NAME.                                                DB2044.2
017000       03 FILLER PICTURE X(12) VALUE SPACE.                       DB2044.2
017100       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB2044.2
017200       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB2044.2
017300       03 FILLER PIC X(5) VALUE SPACE.                            DB2044.2
017400     02 FILLER PIC X(10) VALUE SPACE.                             DB2044.2
017500     02 RE-MARK PIC X(61).                                        DB2044.2
017600 01  TEST-COMPUTED.                                               DB2044.2
017700     02 FILLER PIC X(30) VALUE SPACE.                             DB2044.2
017800     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB2044.2
017900     02 COMPUTED-X.                                               DB2044.2
018000     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB2044.2
018100     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB2044.2
018200     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB2044.2
018300     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB2044.2
018400     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB2044.2
018500     03       CM-18V0 REDEFINES COMPUTED-A.                       DB2044.2
018600         04 COMPUTED-18V0                   PICTURE -9(18).       DB2044.2
018700         04 FILLER                          PICTURE X.            DB2044.2
018800     03 FILLER PIC X(50) VALUE SPACE.                             DB2044.2
018900 01  TEST-CORRECT.                                                DB2044.2
019000     02 FILLER PIC X(30) VALUE SPACE.                             DB2044.2
019100     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB2044.2
019200     02 CORRECT-X.                                                DB2044.2
019300     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB2044.2
019400     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB2044.2
019500     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB2044.2
019600     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB2044.2
019700     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB2044.2
019800     03      CR-18V0 REDEFINES CORRECT-A.                         DB2044.2
019900         04 CORRECT-18V0                    PICTURE -9(18).       DB2044.2
020000         04 FILLER                          PICTURE X.            DB2044.2
020100     03 FILLER PIC X(50) VALUE SPACE.                             DB2044.2
020200 01  CCVS-C-1.                                                    DB2044.2
020300     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB2044.2
020400-    "SS  PARAGRAPH-NAME                                          DB2044.2
020500-    "        REMARKS".                                           DB2044.2
020600     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB2044.2
020700 01  CCVS-C-2.                                                    DB2044.2
020800     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2044.2
020900     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB2044.2
021000     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB2044.2
021100     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB2044.2
021200     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB2044.2
021300 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB2044.2
021400 01  REC-CT PICTURE 99 VALUE ZERO.                                DB2044.2
021500 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB2044.2
021600 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB2044.2
021700 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB2044.2
021800 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB2044.2
021900 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB2044.2
022000 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB2044.2
022100 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB2044.2
022200 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB2044.2
022300 01  CCVS-H-1.                                                    DB2044.2
022400     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB2044.2
022500     02 FILLER PICTURE X(67) VALUE                                DB2044.2
022600     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB2044.2
022700-    " SYSTEM".                                                   DB2044.2
022800     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB2044.2
022900 01  CCVS-H-2.                                                    DB2044.2
023000     02 FILLER PICTURE X(52) VALUE IS                             DB2044.2
023100     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB2044.2
023200     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB2044.2
023300     02 TEST-ID PICTURE IS X(9).                                  DB2044.2
023400     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB2044.2
023500 01  CCVS-H-3.                                                    DB2044.2
023600     02  FILLER PICTURE X(34) VALUE                               DB2044.2
023700     " FOR OFFICIAL USE ONLY    ".                                DB2044.2
023800     02  FILLER PICTURE X(58) VALUE                               DB2044.2
023900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB2044.2
024000     02  FILLER PICTURE X(28) VALUE                               DB2044.2
024100     "  COPYRIGHT   1974 ".                                       DB2044.2
024200 01  CCVS-E-1.                                                    DB2044.2
024300     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB2044.2
024400     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB2044.2
024500     02 ID-AGAIN PICTURE IS X(9).                                 DB2044.2
024600     02 FILLER PICTURE X(45) VALUE IS                             DB2044.2
024700     " NTIS DISTRIBUTION COBOL 74".                               DB2044.2
024800 01  CCVS-E-2.                                                    DB2044.2
024900     02  FILLER                   PICTURE X(31)  VALUE            DB2044.2
025000     SPACE.                                                       DB2044.2
025100     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB2044.2
025200     02 CCVS-E-2-2.                                               DB2044.2
025300         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB2044.2
025400         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB2044.2
025500         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB2044.2
025600 01  CCVS-E-3.                                                    DB2044.2
025700     02  FILLER PICTURE X(22) VALUE                               DB2044.2
025800     " FOR OFFICIAL USE ONLY".                                    DB2044.2
025900     02  FILLER PICTURE X(12) VALUE SPACE.                        DB2044.2
026000     02  FILLER PICTURE X(58) VALUE                               DB2044.2
026100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB2044.2
026200     02  FILLER PICTURE X(13) VALUE SPACE.                        DB2044.2
026300     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB2044.2
026400 01  CCVS-E-4.                                                    DB2044.2
026500     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB2044.2
026600     02 FILLER PIC XXXX VALUE " OF ".                             DB2044.2
026700     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB2044.2
026800     02 FILLER PIC X(40) VALUE                                    DB2044.2
026900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB2044.2
027000 01  XXINFO.                                                      DB2044.2
027100     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB2044.2
027200     02 INFO-TEXT.                                                DB2044.2
027300     04 FILLER PIC X(20) VALUE SPACE.                             DB2044.2
027400     04 XXCOMPUTED PIC X(20).                                     DB2044.2
027500     04 FILLER PIC X(5) VALUE SPACE.                              DB2044.2
027600     04 XXCORRECT PIC X(20).                                      DB2044.2
027700 01  HYPHEN-LINE.                                                 DB2044.2
027800     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2044.2
027900     02 FILLER PICTURE IS X(65) VALUE IS "************************DB2044.2
028000-    "*****************************************".                 DB2044.2
028100     02 FILLER PICTURE IS X(54) VALUE IS "************************DB2044.2
028200-    "******************************".                            DB2044.2
028300 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB2044.2
028400     "DB204A".                                                    DB2044.2
028500 PROCEDURE DIVISION.                                              DB2044.2
028600 DECLARATIVES.                                                    DB2044.2
028700 MERGE-PROC SECTION.                                              DB2044.2
028800     USE FOR DEBUGGING ON MERGE-OUTPUT-PROC.                      DB2044.2
028900 MERGE-1.                                                         DB2044.2
029000     ADD 1 TO KEY-1.                                              DB2044.2
029100     MOVE DEBUG-LINE TO LINE-1.                                   DB2044.2
029200     MOVE DEBUG-NAME TO NAME-1  UNQUAL-NAME-1.                    DB2044.2
029300     MOVE DEBUG-CONTENTS TO CONTENTS-1.                           DB2044.2
029400     INSPECT UNQUAL-NAME-1 REPLACING CHARACTERS BY SPACES         DB2044.2
029500         AFTER INITIAL SPACE.                                     DB2044.2
029600 END DECLARATIVES.                                                DB2044.2
029700 CCVS1 SECTION.                                                   DB2044.2
029800 OPEN-FILES.                                                      DB2044.2
029900     OPEN     OUTPUT PRINT-FILE.                                  DB2044.2
030000     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB2044.2
030100     MOVE    SPACE TO TEST-RESULTS.                               DB2044.2
030200     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB2044.2
030300     GO TO CCVS1-EXIT.                                            DB2044.2
030400 CLOSE-FILES.                                                     DB2044.2
030500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB2044.2
030600 TERMINATE-CCVS.                                                  DB2044.2
030700*    EXIT PROGRAM.                                                DB2044.2
030800*TERMINATE-CALL.                                                  DB2044.2
030900     STOP     RUN.                                                DB2044.2
031000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB2044.2
031100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB2044.2
031200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB2044.2
031300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB2044.2
031400     MOVE "****TEST DELETED****" TO RE-MARK.                      DB2044.2
031500 PRINT-DETAIL.                                                    DB2044.2
031600     IF REC-CT NOT EQUAL TO ZERO                                  DB2044.2
031700             MOVE "." TO PARDOT-X                                 DB2044.2
031800             MOVE REC-CT TO DOTVALUE.                             DB2044.2
031900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB2044.2
032000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB2044.2
032100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB2044.2
032200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB2044.2
032300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB2044.2
032400     MOVE SPACE TO CORRECT-X.                                     DB2044.2
032500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB2044.2
032600     MOVE     SPACE TO RE-MARK.                                   DB2044.2
032700 HEAD-ROUTINE.                                                    DB2044.2
032800     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2044.2
032900     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB2044.2
033000     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB2044.2
033100 COLUMN-NAMES-ROUTINE.                                            DB2044.2
033200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2044.2
033300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2044.2
033400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB2044.2
033500 END-ROUTINE.                                                     DB2044.2
033600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB2044.2
033700 END-RTN-EXIT.                                                    DB2044.2
033800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2044.2
033900 END-ROUTINE-1.                                                   DB2044.2
034000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB2044.2
034100      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB2044.2
034200      ADD PASS-COUNTER TO ERROR-HOLD.                             DB2044.2
034300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB2044.2
034400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB2044.2
034500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB2044.2
034600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB2044.2
034700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB2044.2
034800  END-ROUTINE-12.                                                 DB2044.2
034900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB2044.2
035000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB2044.2
035100         MOVE "NO " TO ERROR-TOTAL                                DB2044.2
035200         ELSE                                                     DB2044.2
035300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB2044.2
035400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB2044.2
035500     PERFORM WRITE-LINE.                                          DB2044.2
035600 END-ROUTINE-13.                                                  DB2044.2
035700     IF DELETE-CNT IS EQUAL TO ZERO                               DB2044.2
035800         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB2044.2
035900         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB2044.2
036000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB2044.2
036100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2044.2
036200      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB2044.2
036300          MOVE "NO " TO ERROR-TOTAL                               DB2044.2
036400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB2044.2
036500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB2044.2
036600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB2044.2
036700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2044.2
036800 WRITE-LINE.                                                      DB2044.2
036900     ADD 1 TO RECORD-COUNT.                                       DB2044.2
037000*    IF RECORD-COUNT GREATER 50                                   DB2044.2
037100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB2044.2
037200*        MOVE SPACE TO DUMMY-RECORD                               DB2044.2
037300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB2044.2
037400*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB2044.2
037500*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB2044.2
037600*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB2044.2
037700*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB2044.2
037800*        MOVE ZERO TO RECORD-COUNT.                               DB2044.2
037900     PERFORM WRT-LN.                                              DB2044.2
038000 WRT-LN.                                                          DB2044.2
038100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB2044.2
038200     MOVE SPACE TO DUMMY-RECORD.                                  DB2044.2
038300 BLANK-LINE-PRINT.                                                DB2044.2
038400     PERFORM WRT-LN.                                              DB2044.2
038500 FAIL-ROUTINE.                                                    DB2044.2
038600     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB2044.2
038700     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB2044.2
038800     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB2044.2
038900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2044.2
039000     GO TO FAIL-ROUTINE-EX.                                       DB2044.2
039100 FAIL-ROUTINE-WRITE.                                              DB2044.2
039200     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB2044.2
039300     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB2044.2
039400 FAIL-ROUTINE-EX. EXIT.                                           DB2044.2
039500 BAIL-OUT.                                                        DB2044.2
039600     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB2044.2
039700     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB2044.2
039800 BAIL-OUT-WRITE.                                                  DB2044.2
039900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB2044.2
040000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2044.2
040100 BAIL-OUT-EX. EXIT.                                               DB2044.2
040200 CCVS1-EXIT.                                                      DB2044.2
040300     EXIT.                                                        DB2044.2
040400 CREATE-INPUT-FILES SECTION.                                      DB2044.2
040500 SET-UP-REC-AREAS.                                                DB2044.2
040600     MOVE FILE-RECORD-INFO-SKELETON TO FILE-RECORD-INFO (1)       DB2044.2
040700                                       FILE-RECORD-INFO (2).      DB2044.2
040800     MOVE "SQ-FS1" TO XFILE-NAME (1).                             DB2044.2
040900     MOVE "SQ-FS2" TO XFILE-NAME (2).                             DB2044.2
041000     MOVE "REC-1" TO XRECORD-NAME (1).                            DB2044.2
041100     MOVE "REC-2" TO XRECORD-NAME (2).                            DB2044.2
041200     MOVE ".XXX." TO XPROGRAM-NAME (1)                            DB2044.2
041300                     XPROGRAM-NAME (2).                           DB2044.2
041400     MOVE 120 TO XRECORD-LENGTH (1)                               DB2044.2
041500                 XRECORD-LENGTH (2).                              DB2044.2
041600     MOVE "RC" TO CHARS-OR-RECORDS (1)                            DB2044.2
041700                  CHARS-OR-RECORDS (2).                           DB2044.2
041800     MOVE 1 TO XBLOCK-SIZE (1)                                    DB2044.2
041900               XBLOCK-SIZE (2).                                   DB2044.2
042000     MOVE 10 TO RECORDS-IN-FILE (1)                               DB2044.2
042100                RECORDS-IN-FILE (2).                              DB2044.2
042200     MOVE "SQ" TO XFILE-ORGANIZATION (1)                          DB2044.2
042300                  XFILE-ORGANIZATION (2).                         DB2044.2
042400     MOVE "S" TO XLABEL-TYPE (1)                                  DB2044.2
042500                 XLABEL-TYPE (2).                                 DB2044.2
042600     OPEN OUTPUT SQ-FS1  SQ-FS2.                                  DB2044.2
042700 WRITE-FILES.                                                     DB2044.2
042800     MOVE 1 TO XRECORD-NUMBER (1).                                DB2044.2
042900     MOVE 2 TO XRECORD-NUMBER (2).                                DB2044.2
043000     PERFORM WRITE-FILES-SUBROUTINE 10 TIMES.                     DB2044.2
043100     CLOSE SQ-FS1  SQ-FS2.                                        DB2044.2
043200      GO TO BEGIN-DB204A-TESTS.                                   DB2044.2
043300 WRITE-FILES-SUBROUTINE.                                          DB2044.2
043400     MOVE FILE-RECORD-INFO (1) TO REC-1.                          DB2044.2
043500     WRITE REC-1.                                                 DB2044.2
043600     ADD 2 TO XRECORD-NUMBER (1).                                 DB2044.2
043700     MOVE FILE-RECORD-INFO (2) TO REC-2.                          DB2044.2
043800     WRITE REC-2.                                                 DB2044.2
043900     ADD 2 TO XRECORD-NUMBER (2).                                 DB2044.2
044000 BEGIN-DB204A-TESTS SECTION.                                      DB2044.2
044100 MERGE-TEST-INIT.                                                 DB2044.2
044200     MOVE "MERGE-TEST" TO PAR-NAME.                               DB2044.2
044300     MOVE "MERGE OUTPUT PROC" TO FEATURE.                         DB2044.2
044400     MOVE SPACES TO ITEM-1.                                       DB2044.2
044500     MOVE 0 TO KEY-1.                                             DB2044.2
044600******************************************************************DB2044.2
044700*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2044.2
044800*    "MERGE-TEST-A" SHOULD POINT TO THE EXECUTABLE STATEMENT     *DB2044.2
044900*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS, "MERGE      *DB2044.2
045000*    ST-FS4  ON ASCENDING KEY SORT-KEY  USING SQ-FS1 SQ-FS2      *DB2044.2
045100*    OUTPUT PROCEDURE IS MERGE-OUTPUT-PROC.".                    *DB2044.2
045200******************************************************************DB2044.2
045300 MERGE-TEST.                                                      DB2044.2
045400     MERGE ST-FS4  ON ASCENDING KEY SORT-KEY                      DB2044.2
045500         USING  SQ-FS1  SQ-FS2                                    DB2044.2
045600         OUTPUT PROCEDURE IS MERGE-OUTPUT-PROC.                   DB2044.2
045700     IF KEY-1 IS EQUAL TO 1                                       DB2044.2
045800         PERFORM PASS                                             DB2044.2
045900         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2044.2
046000     ELSE  PERFORM FAIL                                           DB2044.2
046100         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK          DB2044.2
046200         MOVE 1 TO CORRECT-18V0                                   DB2044.2
046300         MOVE KEY-1 TO COMPUTED-18V0.                             DB2044.2
046400     PERFORM PRINT-DETAIL                                         DB2044.2
046500     IF KEY-1 IS EQUAL TO 0                                       DB2044.2
046600         PERFORM DELETE-MERGE-TEST-SUBTESTS                       DB2044.2
046700         GO TO END-OF-DB204A                                      DB2044.2
046800     ELSE GO TO MERGE-TEST-A.                                     DB2044.2
046900 MERGE-TEST-DELETE.                                               DB2044.2
047000     PERFORM DE-LETE.                                             DB2044.2
047100     PERFORM PRINT-DETAIL.                                        DB2044.2
047200     PERFORM DELETE-MERGE-TEST-SUBTESTS.                          DB2044.2
047300     GO TO END-OF-DB204A.                                         DB2044.2
047400 DELETE-MERGE-TEST-SUBTESTS.                                      DB2044.2
047500     MOVE "MERGE-TEST-A" TO PAR-NAME.                             DB2044.2
047600     PERFORM DE-LETE.                                             DB2044.2
047700     PERFORM PRINT-DETAIL.                                        DB2044.2
047800     MOVE "MERGE-TEST-B" TO PAR-NAME.                             DB2044.2
047900     PERFORM DE-LETE.                                             DB2044.2
048000     PERFORM PRINT-DETAIL.                                        DB2044.2
048100     MOVE "MERGE-TEST-C" TO PAR-NAME.                             DB2044.2
048200     PERFORM DE-LETE.                                             DB2044.2
048300     PERFORM PRINT-DETAIL.                                        DB2044.2
048400 MERGE-TEST-A.                                                    DB2044.2
048500     MOVE "MERGE-TEST-A" TO PAR-NAME.                             DB2044.2
048600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2044.2
048700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2044.2
048800     MOVE LINE-1 TO COMPUTED-A.                                   DB2044.2
048900     PERFORM INSPT.                                               DB2044.2
049000     PERFORM PRINT-DETAIL.                                        DB2044.2
049100 MERGE-TEST-B.                                                    DB2044.2
049200     MOVE "MERGE-TEST-B" TO PAR-NAME.                             DB2044.2
049300     IF UNQUAL-NAME-1 IS EQUAL TO "MERGE-OUTPUT-PROC"             DB2044.2
049400         PERFORM PASS                                             DB2044.2
049500     ELSE  PERFORM FAIL                                           DB2044.2
049600         MOVE "MERGE-OUTPUT-PROC" TO CORRECT-A                    DB2044.2
049700         MOVE NAME-1 TO COMPUTED-A.                               DB2044.2
049800     MOVE "DEBUG-NAME" TO RE-MARK                                 DB2044.2
049900     PERFORM PRINT-DETAIL.                                        DB2044.2
050000 MERGE-TEST-C.                                                    DB2044.2
050100     MOVE "MERGE-TEST-C" TO PAR-NAME.                             DB2044.2
050200     IF CONTENTS-1 IS EQUAL TO "MERGE OUTPUT"                     DB2044.2
050300         PERFORM PASS                                             DB2044.2
050400     ELSE  PERFORM FAIL                                           DB2044.2
050500         MOVE "MERGE OUTPUT" TO CORRECT-A                         DB2044.2
050600         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2044.2
050700     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2044.2
050800     PERFORM PRINT-DETAIL.                                        DB2044.2
050900     GO TO END-OF-DB204A.                                         DB2044.2
051000 MERGE-OUTPUT-PROC SECTION.                                       DB2044.2
051100 OPEN-OUTPUT-FILE.                                                DB2044.2
051200     OPEN OUTPUT SQ-FS3.                                          DB2044.2
051300 RETURN-RECORDS.                                                  DB2044.2
051400     RETURN ST-FS4 RECORD INTO REC-3                              DB2044.2
051500         AT END GO TO CLOSE-OUTPUT-FILE.                          DB2044.2
051600     WRITE REC-3.                                                 DB2044.2
051700     GO TO RETURN-RECORDS.                                        DB2044.2
051800 CLOSE-OUTPUT-FILE.                                               DB2044.2
051900     CLOSE SQ-FS3.                                                DB2044.2
052000 END-OF-DB204A SECTION.                                           DB2044.2
052100*DUMP-SQ-FS1.                                                     DB2044.2
052200*    OPEN INPUT SQ-FS1.                                           DB2044.2
052300*    MOVE "DUMP OF SQ-FS1 FOLLOWS:" TO PRINT-REC.                 DB2044.2
052400*    PERFORM WRITE-LINE.                                          DB2044.2
052500*READ-SQ-FS1.                                                     DB2044.2
052600*    READ SQ-FS1  AT END GO TO DUMP-SQ-FS2.                       DB2044.2
052700*    MOVE REC-1 TO PRINT-REC.                                     DB2044.2
052800*    PERFORM WRITE-LINE.                                          DB2044.2
052900*    GO TO READ-SQ-FS1.                                           DB2044.2
053000*DUMP-SQ-FS2.                                                     DB2044.2
053100*    CLOSE SQ-FS1.                                                DB2044.2
053200*    OPEN INPUT SQ-FS2.                                           DB2044.2
053300*    MOVE "DUMP OF SQ-FS2 FOLLOWS:" TO PRINT-REC.                 DB2044.2
053400*    PERFORM WRITE-LINE.                                          DB2044.2
053500*READ-SQ-FS2.                                                     DB2044.2
053600*    READ SQ-FS2 AT END GO TO DUMP-SQ-FS3.                        DB2044.2
053700*    MOVE REC-2 TO PRINT-REC.                                     DB2044.2
053800*    PERFORM WRITE-LINE.                                          DB2044.2
053900*    GO TO READ-SQ-FS2.                                           DB2044.2
054000*DUMP-SQ-FS3.                                                     DB2044.2
054100*    CLOSE SQ-FS2.                                                DB2044.2
054200*    OPEN INPUT SQ-FS3.                                           DB2044.2
054300*    MOVE "DUMP OF SQ-FS3 FOLLOWS:" TO PRINT-REC.                 DB2044.2
054400*    PERFORM WRITE-LINE.                                          DB2044.2
054500*READ-SQ-FS3.                                                     DB2044.2
054600*    READ SQ-FS3 AT END GO TO END-OF-TESTS.                       DB2044.2
054700*    MOVE REC-3 TO PRINT-REC.                                     DB2044.2
054800*    PERFORM WRITE-LINE.                                          DB2044.2
054900*    GO TO READ-SQ-FS3.                                           DB2044.2
055000 END-OF-TESTS.                                                    DB2044.2
055100     EXIT.                                                        DB2044.2
055200 CCVS-EXIT SECTION.                                               DB2044.2
055300 CCVS-999999.                                                     DB2044.2
055400     GO TO CLOSE-FILES.                                           DB2044.2
      *END-OF,DB204A                                                                  
