      *HEADER,COBOL,OBIC1A,SUBRTN,OBIC2A                                              
000100 IDENTIFICATION DIVISION.                                         OBIC24.2
000200 PROGRAM-ID.                                                      OBIC24.2
000300     OBIC2A.                                                      OBIC24.2
000400****************************************************************  OBIC24.2
000500*                                                              *  OBIC24.2
000600*    VALIDATION FOR:-                                          *  OBIC24.2
000700*                                                              *  OBIC24.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBIC24.2
000900*                                                              *  OBIC24.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBIC24.2
001100*                                                              *  OBIC24.2
001200****************************************************************  OBIC24.2
001300*                                                              *  OBIC24.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  OBIC24.2
001500*                                                              *  OBIC24.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  OBIC24.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  OBIC24.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  OBIC24.2
001900*                                                              *  OBIC24.2
002000****************************************************************  OBIC24.2
002100*                                                                 OBIC24.2
002200*        THE SUBPROGRAM IC219 TESTS THE USE OF A SORT STATEMENT   OBIC24.2
002300*    IN A SEGMENTED SUBPROGRAM.  THE FIRST NON-DECLARATIVE SECTIONOBIC24.2
002400*    OF THE SUBPROGRAM CONSISTS OF A SORT STATEMENT AND A STOP RUNOBIC24.2
002500*    STATEMENT IN A FIXED PERMANENT SEGMENT.  THE SORT INPUT      OBIC24.2
002600*    PROCEDURE AND THE SORT OUTPUT PROCEDURE ARE CONTAINED IN TWO OBIC24.2
002700*    INDEPENDENT SEGMENTS.  THE MAIN PROGRAM IC218 CALLS THIS     OBIC24.2
002800*    SUBPROGRAM AND THE SUBPROGRAM IC220 IS CALLED FROM THE       OBIC24.2
002900*    OUTPUT PROCEDURE SECTION TO PRINT THE OUTPUT REPORT.         OBIC24.2
003000*                                                                 OBIC24.2
003100******************************************************************OBIC24.2
003200 ENVIRONMENT DIVISION.                                            OBIC24.2
003300 CONFIGURATION SECTION.                                           OBIC24.2
003400 SOURCE-COMPUTER.                                                 OBIC24.2
003500     XXXXX082.                                                    OBIC24.2
003600 OBJECT-COMPUTER.                                                 OBIC24.2
003700     XXXXX083.                                                    OBIC24.2
003800 INPUT-OUTPUT SECTION.                                            OBIC24.2
003900 FILE-CONTROL.                                                    OBIC24.2
004000     SELECT ST-FS1 ASSIGN TO                                      OBIC24.2
004100     XXXXX027.                                                    OBIC24.2
004200 DATA DIVISION.                                                   OBIC24.2
004300 FILE SECTION.                                                    OBIC24.2
004400 SD  ST-FS1                                                       OBIC24.2
004500     DATA RECORD IS ST-FS1R1-F-G-126.                             OBIC24.2
004600 01  ST-FS1R1-F-G-126.                                            OBIC24.2
004700     02  ST-FS1-1-120.                                            OBIC24.2
004800         03  FILLER          PICTURE X(34).                       OBIC24.2
004900         03  ST-FS1-REC-NO   PICTURE 9(6).                        OBIC24.2
005000         03  FILLER          PICTURE X(80).                       OBIC24.2
005100     02  ST-FS1-121-124      PICTURE X(4).                        OBIC24.2
005200     02  ST-FS1-125-126      PICTURE 99.                          OBIC24.2
005300 WORKING-STORAGE SECTION.                                         OBIC24.2
005400 01  TEMP1               PICTURE X(4).                            OBIC24.2
005500 01  TEMP2               PICTURE 999.                             OBIC24.2
005600 01  TEMP3               PICTURE 999.                             OBIC24.2
005700 01  TEMP4               PICTURE 9(6).                            OBIC24.2
005800 01  FAIL-COUNT          PICTURE 999    VALUE ZERO.               OBIC24.2
005900 01  EOF-FLAG            PICTURE 9    VALUE ZERO.                 OBIC24.2
006000 01  PRINT-LINE-VALUES.                                           OBIC24.2
006100     02  PASS-OR-FAIL        PICTURE X(5).                        OBIC24.2
006200     02  R-COUNT             PICTURE 99.                          OBIC24.2
006300     02  FEATURE-TESTED      PICTURE X(20).                       OBIC24.2
006400     02  COMPUTED-SORT-KEY.                                       OBIC24.2
006500         03  COMPUTED-1-4    PICTURE X(4).                        OBIC24.2
006600         03  COMPUTED-5-6    PICTURE 99.                          OBIC24.2
006700         03  COMPUTED-7-12   PICTURE 9(6).                        OBIC24.2
006800         03  FILLER          PICTURE X(8)  VALUE SPACE.           OBIC24.2
006900     02  CORRECT-SORT-KEY.                                        OBIC24.2
007000         03  CORRECT-1-4     PICTURE X(4).                        OBIC24.2
007100         03  CORRECT-5-6     PICTURE 99.                          OBIC24.2
007200         03  CORRECT-7-12    PICTURE 9(6).                        OBIC24.2
007300         03  FILLER          PICTURE X(8)   VALUE SPACE.          OBIC24.2
007400     02  PARAGRAPH-NAME      PICTURE X(12).                       OBIC24.2
007500 01  PRINT-FLAG              PICTURE 9.                           OBIC24.2
007600 01  FILE-RECORD-INFORMATION-REC.                                 OBIC24.2
007700     03 FILE-RECORD-INFO-SKELETON.                                OBIC24.2
007800        05 FILLER                 PICTURE X(48)       VALUE       OBIC24.2
007900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  OBIC24.2
008000        05 FILLER                 PICTURE X(46)       VALUE       OBIC24.2
008100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    OBIC24.2
008200        05 FILLER                 PICTURE X(26)       VALUE       OBIC24.2
008300             ",LFIL=000000,ORG=  ,LBLR= ".                        OBIC24.2
008400        05 FILLER                 PICTURE X(37)       VALUE       OBIC24.2
008500             ",RECKEY=                             ".             OBIC24.2
008600        05 FILLER                 PICTURE X(38)       VALUE       OBIC24.2
008700             ",ALTKEY1=                             ".            OBIC24.2
008800        05 FILLER                 PICTURE X(38)       VALUE       OBIC24.2
008900             ",ALTKEY2=                             ".            OBIC24.2
009000        05 FILLER                 PICTURE X(7)        VALUE SPACE.OBIC24.2
009100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              OBIC24.2
009200        05 FILE-RECORD-INFO-P1-120.                               OBIC24.2
009300           07 FILLER              PIC X(5).                       OBIC24.2
009400           07 XFILE-NAME           PIC X(6).                      OBIC24.2
009500           07 FILLER              PIC X(8).                       OBIC24.2
009600           07 XRECORD-NAME         PIC X(6).                      OBIC24.2
009700           07 FILLER              PIC X(1).                       OBIC24.2
009800           07 REELUNIT-NUMBER     PIC 9(1).                       OBIC24.2
009900           07 FILLER              PIC X(7).                       OBIC24.2
010000           07 XRECORD-NUMBER       PIC 9(6).                      OBIC24.2
010100           07 FILLER              PIC X(6).                       OBIC24.2
010200           07 UPDATE-NUMBER       PIC 9(2).                       OBIC24.2
010300           07 FILLER              PIC X(5).                       OBIC24.2
010400           07 ODO-NUMBER          PIC 9(4).                       OBIC24.2
010500           07 FILLER              PIC X(5).                       OBIC24.2
010600           07 XPROGRAM-NAME        PIC X(5).                      OBIC24.2
010700           07 FILLER              PIC X(7).                       OBIC24.2
010800           07 XRECORD-LENGTH       PIC 9(6).                      OBIC24.2
010900           07 FILLER              PIC X(7).                       OBIC24.2
011000           07 CHARS-OR-RECORDS    PIC X(2).                       OBIC24.2
011100           07 FILLER              PIC X(1).                       OBIC24.2
011200           07 XBLOCK-SIZE          PIC 9(4).                      OBIC24.2
011300           07 FILLER              PIC X(6).                       OBIC24.2
011400           07 RECORDS-IN-FILE     PIC 9(6).                       OBIC24.2
011500           07 FILLER              PIC X(5).                       OBIC24.2
011600           07 XFILE-ORGANIZATION   PIC X(2).                      OBIC24.2
011700           07 FILLER              PIC X(6).                       OBIC24.2
011800           07 XLABEL-TYPE          PIC X(1).                      OBIC24.2
011900        05 FILE-RECORD-INFO-P121-240.                             OBIC24.2
012000           07 FILLER              PIC X(8).                       OBIC24.2
012100           07 XRECORD-KEY          PIC X(29).                     OBIC24.2
012200           07 FILLER              PIC X(9).                       OBIC24.2
012300           07 ALTERNATE-KEY1      PIC X(29).                      OBIC24.2
012400           07 FILLER              PIC X(9).                       OBIC24.2
012500           07 ALTERNATE-KEY2      PIC X(29).                      OBIC24.2
012600           07 FILLER              PIC X(7).                       OBIC24.2
012700 LINKAGE SECTION.                                                 OBIC24.2
012800 01  SORT-LINK           PICTURE 9.                               OBIC24.2
012900 PROCEDURE DIVISION USING SORT-LINK.                              OBIC24.2
013000 SECT-IC219-0001 SECTION 30.                                      OBIC24.2
013100*                                                                 OBIC24.2
013200*        THIS SECTION CONTAINS A SORT STATEMENT AND A STOP RUN    OBIC24.2
013300*    STATEMENT, THE ONLY STATEMENTS PERMITTED IN THE FIRST NON-   OBIC24.2
013400*    DECLARATIVE PORTION OF THE PROCEDURE DIVISION IN SORT LEVEL 1OBIC24.2
013500*                                                                 OBIC24.2
013600 SORT-PARAGRAPH.                                                  OBIC24.2
013700     SORT ST-FS1                                                  OBIC24.2
013800         ASCENDING KEY ST-FS1-121-124                             OBIC24.2
013900         ASCENDING KEY ST-FS1-125-126                             OBIC24.2
014000         ASCENDING KEY ST-FS1-REC-NO                              OBIC24.2
014100             INPUT PROCEDURE IS SECT-IC219-0002                   OBIC24.2
014200             OUTPUT PROCEDURE IS SECT-IC219-0003.                 OBIC24.2
014300     STOP RUN.                                                    OBIC24.2
014400 SECT-IC219-0002 SECTION 60.                                      OBIC24.2
014500*                                                                 OBIC24.2
014600*        THE SORT INPUT PROCEDURE RELEASES 500 SORT RECORDS OF    OBIC24.2
014700*    LENGTH 126 CHARACTERS.  THREE ITEMS ARE USED AS THE SORT KEY,OBIC24.2
014800*    THEY ARE CHARACTERS 121-124 PICX(4), CHARACTERS 125-126      OBIC24.2
014900*    PIC 99, AND THE RECORD NUMBER FIELD CHARACTERS 35-40 PIC 9(6)OBIC24.2
015000*    THE RECORDS ARE WRITTEN WITH THE SORT KEY ITEMS CONTAINING   OBIC24.2
015100*    THE FOLLOWING CHARACTERS                                     OBIC24.2
015200*                                                                 OBIC24.2
015300*        FIRST 100 RECORDS  ABCD  00 THRU 99  000001 THRU 000100  OBIC24.2
015400*        SECOND 100 RECORDS AAAA  00 THRU 99  000101 THRU 000200  OBIC24.2
015500*        THIRD 100 RECORDS  ABCD  00 THRU 99  000201 THRU 00300   OBIC24.2
015600*        FOURTH 100 RECORDS UVWY  00 THRU 99  000301 THRU 000400  OBIC24.2
015700*        FIFTH 100 RECORDS  UVWX  00 THRU 99  000401 THRU 000500  OBIC24.2
015800*                                                                 OBIC24.2
015900 SORT-INPUT-PROCEDURE.                                            OBIC24.2
016000     MOVE 1 TO SORT-LINK.                                         OBIC24.2
016100     MOVE FILE-RECORD-INFO-SKELETON TO FILE-RECORD-INFO (1).      OBIC24.2
016200     PERFORM RECORD-AREA-INIT.                                    OBIC24.2
016300     MOVE "ABCD" TO TEMP1.                                        OBIC24.2
016400     MOVE 0 TO TEMP2.                                             OBIC24.2
016500     PERFORM RELEASE-RECORD 100 TIMES.                            OBIC24.2
016600     MOVE "AAAA" TO TEMP1.                                        OBIC24.2
016700     MOVE 0 TO TEMP2.                                             OBIC24.2
016800     PERFORM RELEASE-RECORD 100 TIMES.                            OBIC24.2
016900     MOVE "ABCD" TO TEMP1.                                        OBIC24.2
017000     MOVE 0 TO TEMP2.                                             OBIC24.2
017100     PERFORM RELEASE-RECORD 100 TIMES.                            OBIC24.2
017200     MOVE "UVWY" TO TEMP1.                                        OBIC24.2
017300     MOVE 0 TO TEMP2.                                             OBIC24.2
017400     PERFORM RELEASE-RECORD 100 TIMES.                            OBIC24.2
017500     MOVE "UVWX" TO TEMP1.                                        OBIC24.2
017600     MOVE 0 TO TEMP2.                                             OBIC24.2
017700     PERFORM RELEASE-RECORD 100 TIMES.                            OBIC24.2
017800     GO TO SECT-IC219-0002-EXIT.                                  OBIC24.2
017900 RECORD-AREA-INIT.                                                OBIC24.2
018000     MOVE "ST-FS1" TO XFILE-NAME (1).                             OBIC24.2
018100     MOVE "R1-F-G" TO XRECORD-NAME (1).                           OBIC24.2
018200     MOVE "IC219" TO XPROGRAM-NAME (1).                           OBIC24.2
018300     MOVE 126 TO XRECORD-LENGTH (1).                              OBIC24.2
018400     MOVE "RC" TO CHARS-OR-RECORDS (1).                           OBIC24.2
018500     MOVE 0001 TO XBLOCK-SIZE (1).                                OBIC24.2
018600     MOVE 500 TO RECORDS-IN-FILE (1).                             OBIC24.2
018700     MOVE "NA" TO XFILE-ORGANIZATION (1).                         OBIC24.2
018800     MOVE "N" TO XLABEL-TYPE (1).                                 OBIC24.2
018900     MOVE 1 TO XRECORD-NUMBER (1).                                OBIC24.2
019000 RELEASE-RECORD.                                                  OBIC24.2
019100     MOVE FILE-RECORD-INFO-P1-120 (1) TO ST-FS1-1-120.            OBIC24.2
019200     MOVE TEMP1 TO ST-FS1-121-124.                                OBIC24.2
019300     MOVE TEMP2 TO ST-FS1-125-126.                                OBIC24.2
019400     RELEASE ST-FS1R1-F-G-126.                                    OBIC24.2
019500     ADD 1 TO XRECORD-NUMBER (1).                                 OBIC24.2
019600     ADD 1 TO TEMP2.                                              OBIC24.2
019700 SECT-IC219-0002-EXIT.                                            OBIC24.2
019800     EXIT.                                                        OBIC24.2
019900 SECT-IC219-0003 SECTION 80.                                      OBIC24.2
020000*                                                                 OBIC24.2
020100*        THE SORT OUTPUT PROCEDURE RETURNS 500 SORT RECORDS.  THE OBIC24.2
020200*    DATA ITEMS COMPRISING THE SORT KEY ARE CHECKED TO ENSURE THE OBIC24.2
020300*    RECORDS ARE RETURNED IN THE EXPECTED SORT ORDER.  THE SUBPRO-OBIC24.2
020400*    GRAM IC220 IS CALLED TO PRODUCE THE OUTPUT REPORT FOR THE    OBIC24.2
020500*    TEST RESULTS.                                                OBIC24.2
020600*                                                                 OBIC24.2
020700*        THE RECORDS SHOULD BE RETURNED WITH THE SORT KEY ITEMS   OBIC24.2
020800*    CONTAINING THE FOLLOWING CHARACTERS                          OBIC24.2
020900*                                                                 OBIC24.2
021000*        FIRST 100 RECORDS  AAAA  00 THRU 99  000101 THRU 000200  OBIC24.2
021100*                           ABCD  00          000001              OBIC24.2
021200*        NEXT               ABCD  00          000201              OBIC24.2
021300*            200            ABCD  01          000002              OBIC24.2
021400*               RECORDS     ABCD  01          000202              OBIC24.2
021500*                            . . .            . . .               OBIC24.2
021600*                           ABCD  99          000100              OBIC24.2
021700*                           ABCD  99          000300              OBIC24.2
021800*        FOURTH 100 RECORDS UVWX  00 THRU 99  000401 THRU 000500  OBIC24.2
021900*        FIFTH 100 RECORDS  UVWY  00 THRU 99  000301 THRU 000400  OBIC24.2
022000*                                                                 OBIC24.2
022100 SORT-OUTPUT-INIT.                                                OBIC24.2
022200     MOVE 2 TO SORT-LINK.                                         OBIC24.2
022300     MOVE 1 TO PRINT-FLAG.                                        OBIC24.2
022400     CALL "OBIC3A" USING PRINT-LINE-VALUES  PRINT-FLAG.           OBIC24.2
022500     MOVE "SORT IN SUBPROGRAM" TO FEATURE-TESTED.                 OBIC24.2
022600     MOVE "IC-SORT-TEST" TO PARAGRAPH-NAME.                       OBIC24.2
022700     MOVE 0 TO R-COUNT.                                           OBIC24.2
022800 CHECK-OUTPUT-FROM-SORT.                                          OBIC24.2
022900     MOVE "AAAA" TO TEMP1.                                        OBIC24.2
023000     MOVE 0 TO TEMP3.                                             OBIC24.2
023100     MOVE 100 TO TEMP4.                                           OBIC24.2
023200     PERFORM CHECK-RECORD 100 TIMES.                              OBIC24.2
023300     MOVE "ABCD" TO TEMP1.                                        OBIC24.2
023400     MOVE 0 TO TEMP3.                                             OBIC24.2
023500     MOVE 0 TO TEMP4.                                             OBIC24.2
023600     PERFORM CHECK-ABCD-RECORDS 100 TIMES.                        OBIC24.2
023700     MOVE "UVWX" TO TEMP1.                                        OBIC24.2
023800     MOVE 0 TO TEMP3.                                             OBIC24.2
023900     MOVE 400 TO TEMP4.                                           OBIC24.2
024000     PERFORM CHECK-RECORD 100 TIMES.                              OBIC24.2
024100     MOVE "UVWY" TO TEMP1.                                        OBIC24.2
024200     MOVE 0 TO TEMP3.                                             OBIC24.2
024300     MOVE 300 TO TEMP4.                                           OBIC24.2
024400     PERFORM CHECK-RECORD 100 TIMES.                              OBIC24.2
024500 CHECK-RESULTS.                                                   OBIC24.2
024600     IF EOF-FLAG EQUAL TO 1                                       OBIC24.2
024700         MOVE "PREMATURE EOF" TO COMPUTED-SORT-KEY                OBIC24.2
024800         MOVE "DATA RECORD EXPECTED" TO CORRECT-SORT-KEY          OBIC24.2
024900         GO TO FAIL-WRITE.                                        OBIC24.2
025000     RETURN ST-FS1 AT END GO TO CHECK-FAIL-COUNT.                 OBIC24.2
025100     MOVE "NO EOF - 500 READ" TO COMPUTED-SORT-KEY.               OBIC24.2
025200     MOVE "EOF EXPECTED" TO CORRECT-SORT-KEY.                     OBIC24.2
025300     GO TO FAIL-WRITE.                                            OBIC24.2
025400 CHECK-FAIL-COUNT.                                                OBIC24.2
025500     IF FAIL-COUNT EQUAL TO ZERO                                  OBIC24.2
025600         MOVE "PASS " TO PASS-OR-FAIL                             OBIC24.2
025700         GO TO WRITE-RESULTS.                                     OBIC24.2
025800     MOVE "SORT ERRORS" TO COMPUTED-SORT-KEY.                     OBIC24.2
025900     MOVE SPACE TO CORRECT-SORT-KEY.                              OBIC24.2
026000 FAIL-WRITE.                                                      OBIC24.2
026100     MOVE "FAIL " TO PASS-OR-FAIL.                                OBIC24.2
026200 WRITE-RESULTS.                                                   OBIC24.2
026300     MOVE 0 TO R-COUNT.                                           OBIC24.2
026400     MOVE 2 TO PRINT-FLAG.                                        OBIC24.2
026500     CALL "OBIC3A" USING PRINT-LINE-VALUES  PRINT-FLAG.           OBIC24.2
026600 WRAPUP-OUTPUT-PROC.                                              OBIC24.2
026700     MOVE 3 TO PRINT-FLAG.                                        OBIC24.2
026800     CALL "OBIC3A" USING PRINT-LINE-VALUES  PRINT-FLAG.           OBIC24.2
026900     GO TO SECT-IC219-0003-EXIT.                                  OBIC24.2
027000 CHECK-RECORD.                                                    OBIC24.2
027100     PERFORM RETURN-RECORD THROUGH RETURN-EXIT.                   OBIC24.2
027200     ADD 1 TO TEMP4.                                              OBIC24.2
027300     PERFORM COMPARE-VALUES THROUGH COMPARE-EXIT.                 OBIC24.2
027400     ADD 1 TO TEMP3.                                              OBIC24.2
027500 RETURN-RECORD.                                                   OBIC24.2
027600     IF EOF-FLAG EQUAL TO 1                                       OBIC24.2
027700         GO TO RETURN-EXIT.                                       OBIC24.2
027800     RETURN ST-FS1  AT END  MOVE 1 TO EOF-FLAG.                   OBIC24.2
027900 RETURN-EXIT.                                                     OBIC24.2
028000     EXIT.                                                        OBIC24.2
028100 CHECK-ABCD-RECORDS.                                              OBIC24.2
028200     PERFORM CHECK-RECORD.                                        OBIC24.2
028300     SUBTRACT 1 FROM TEMP3.                                       OBIC24.2
028400     ADD 199 TO TEMP4.                                            OBIC24.2
028500     PERFORM CHECK-RECORD.                                        OBIC24.2
028600     SUBTRACT 200 FROM TEMP4.                                     OBIC24.2
028700 COMPARE-VALUES.                                                  OBIC24.2
028800     IF TEMP1 NOT EQUAL TO ST-FS1-121-124                         OBIC24.2
028900         GO TO SORT-FAIL.                                         OBIC24.2
029000     IF TEMP3 NOT EQUAL TO ST-FS1-125-126                         OBIC24.2
029100         GO TO SORT-FAIL.                                         OBIC24.2
029200     IF TEMP4 NOT EQUAL TO ST-FS1-REC-NO                          OBIC24.2
029300         GO TO SORT-FAIL.                                         OBIC24.2
029400     GO TO COMPARE-EXIT.                                          OBIC24.2
029500 SORT-FAIL.                                                       OBIC24.2
029600     MOVE "FAIL " TO PASS-OR-FAIL.                                OBIC24.2
029700     ADD 1 TO R-COUNT.                                            OBIC24.2
029800     MOVE TEMP1 TO CORRECT-1-4.                                   OBIC24.2
029900     MOVE TEMP3 TO CORRECT-5-6.                                   OBIC24.2
030000     MOVE TEMP4 TO CORRECT-7-12.                                  OBIC24.2
030100     MOVE ST-FS1-121-124 TO COMPUTED-1-4.                         OBIC24.2
030200     MOVE ST-FS1-125-126 TO COMPUTED-5-6.                         OBIC24.2
030300     MOVE ST-FS1-REC-NO TO COMPUTED-7-12.                         OBIC24.2
030400     MOVE 2 TO PRINT-FLAG.                                        OBIC24.2
030500     CALL "OBIC3A" USING PRINT-LINE-VALUES  PRINT-FLAG.           OBIC24.2
030600     ADD 1 TO FAIL-COUNT.                                         OBIC24.2
030700 COMPARE-EXIT.                                                    OBIC24.2
030800     EXIT.                                                        OBIC24.2
030900 SECT-IC219-0003-EXIT.                                            OBIC24.2
031000     EXIT.                                                        OBIC24.2
      *END-OF,OBIC2A                                                                  
