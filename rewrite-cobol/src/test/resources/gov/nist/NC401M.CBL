      *HEADER,COBOL,NC401M                                                            
000100 IDENTIFICATION DIVISION.                                         NC4014.2
000200 PROGRAM-ID.                                                      NC4014.2
000300      NC401M.                                                     NC4014.2
000400                                                                  NC4014.2
000500                                                                  NC4014.2
000600*THIS PROGRAM TESTS THE FLAGGING OF HIGH SUBSET NUCLEUS           NC4014.2
000700*FEATURES.                                                        NC4014.2
000800 DATE-COMPILED.  22ND AUG 1988.                                   NC4014.2
000900*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
001000 ENVIRONMENT DIVISION.                                            NC4014.2
001100 CONFIGURATION SECTION.                                           NC4014.2
001200 SOURCE-COMPUTER.                                                 NC4014.2
001300     XXXXX082.                                                    NC4014.2
001400 OBJECT-COMPUTER.                                                 NC4014.2
001500     XXXXX083.                                                    NC4014.2
001600 SPECIAL-NAMES.                                                   NC4014.2
001700     XXXXX056                                                     NC4014.2
001800     IS VDUNIT                                                    NC4014.2
001900     ALPHABET                                                     NC4014.2
002000     TEST-ALPHABET IS "A" THRU "F"                                NC4014.2
002100*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
002200                                                                  NC4014.2
002300                                                                  NC4014.2
002400     SYMBOLIC CHARACTERS A IS 32.                                 NC4014.2
002500*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
002600                                                                  NC4014.2
002700                                                                  NC4014.2
002800 INPUT-OUTPUT SECTION.                                            NC4014.2
002900 FILE-CONTROL.                                                    NC4014.2
003000     SELECT TFIL ASSIGN                                           NC4014.2
003100     XXXXX001.                                                    NC4014.2
003200     SELECT TFIL-2 ASSIGN                                         NC4014.2
003300     XXXXX002.                                                    NC4014.2
003400 DATA DIVISION.                                                   NC4014.2
003500 FILE SECTION.                                                    NC4014.2
003600 FD TFIL.                                                         NC4014.2
003700 01 FREC.                                                         NC4014.2
003800     03 GUBBINS PIC X(1                                           NC4014.2
003900-                      00).                                       NC4014.2
004000*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
004100                                                                  NC4014.2
004200 FD TFIL-2.                                                       NC4014.2
004300 01 FREC-2.                                                       NC4014.2
004400     03 COLONTEST PIC X(100).                                     NC4014.2
004500     03 GUBBINS PIC X(100).                                       NC4014.2
004600                                                                  NC4014.2
004700                                                                  NC4014.2
004800 WORKING-STORAGE SECTION.                                         NC4014.2
004900                                                                  NC4014.2
005000 01 TEST-CUSTOMER-RECORD.                                         NC4014.2
005100     03 TEST-AR-CUSTOMER-ID PIC X(4).                             NC4014.2
005200     03 TEST-AR-CUSTOMER-NAME PIC X(20).                          NC4014.2
005300     03 TEST-AR-NUMBER-INVOICES PIC S9(2).                        NC4014.2
005400     03 TEST-AR-INVOICE-DATA OCCURS 1 TO 15 TIMES                 NC4014.2
005500                             DEPENDING ON                         NC4014.2
005600                                 TEST-AR-NUMBER-INVOICES          NC4014.2
005700                             INDEXED BY WS-INDEX.                 NC4014.2
005800*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
005900                                                                  NC4014.2
006000         05 TEST-INVOICE-DATE.                                    NC4014.2
006100             07 TEST-INVOICE-YY PIC 99.                           NC4014.2
006200             07 TEST-INVOICE-MM PIC 99.                           NC4014.2
006300             07 TEST-INVOICE-DD PIC 99.                           NC4014.2
006400                                                                  NC4014.2
006500 01 TEST-DESCEND-RECORD.                                          NC4014.2
006600     03 TEST-DESCEND-CUST-ID PIC X(4).                            NC4014.2
006700     03 TEST-DESCEND-CUST-NAME PIC X(20).                         NC4014.2
006800     03 TEST-DESCEND-NO-INV PIC S9(2).                            NC4014.2
006900     03 TEST-DESCEND-INVOICE OCCURS 15 TIMES                      NC4014.2
007000                             ASCENDING KEY IS                     NC4014.2
007100                                 TEST-ASCEND-TIME                 NC4014.2
007200                             DESCENDING KEY IS                    NC4014.2
007300                                 TEST-DESC-DATE.                  NC4014.2
007400*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
007500                                                                  NC4014.2
007600                                                                  NC4014.2
007700             05 TEST-DESC-DATE.                                   NC4014.2
007800                 07 TEST-DESC-YY PIC 99.                          NC4014.2
007900                 07 TEST-DESC-MM PIC 99.                          NC4014.2
008000                 07 TEST-DESC-DD PIC 99.                          NC4014.2
008100             05 TEST-ASCEND-TIME PIC 9(6).                        NC4014.2
008200                                                                  NC4014.2
008300 01 TEST-CODE-TABLE.                                              NC4014.2
008400     03 TEST-CODE PIC X(3)                                        NC4014.2
008500                     OCCURS 40 TIMES                              NC4014.2
008600                     INDEXED BY CODE-INDEX.                       NC4014.2
008700                                                                  NC4014.2
008800 01 CUST-REC.                                                     NC4014.2
008900     03 CUST-CODES PIC X.                                         NC4014.2
009000         88 CUST-PAID VALUE "A".                                  NC4014.2
009100*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
009200                                                                  NC4014.2
009300                                                                  NC4014.2
009400 01 GROUP-1.                                                      NC4014.2
009500     03 ITEM-1 PIC 99 VALUE 10.                                   NC4014.2
009600     03 ITEM-2 PIC 99 VALUE 12.                                   NC4014.2
009700     03 ITEM-3 PIC 99 VALUE 14.                                   NC4014.2
009800                                                                  NC4014.2
009900 01 GROUP-2.                                                      NC4014.2
010000     03 ITEM-1 PIC 99 VALUE 10.                                   NC4014.2
010100     03 ITEM-2 PIC 99 VALUE 12.                                   NC4014.2
010200     03 ITEM-3 PIC 99 VALUE 14.                                   NC4014.2
010300                                                                  NC4014.2
010400 01 SALES-DATA.                                                   NC4014.2
010500     03 STORE-INFO  PIC X(30).                                    NC4014.2
010600     03 MON-SALES OCCURS 2 TIMES.                                 NC4014.2
010700         05 AM-SALES PIC 9(3).                                    NC4014.2
010800         05 TUE-SALES OCCURS 2 TIMES.                             NC4014.2
010900             07 AM-SALES PIC 9(3).                                NC4014.2
011000             07 WED-SALES OCCURS 2 TIMES.                         NC4014.2
011100                 09 AM-SALES PIC 9(3).                            NC4014.2
011200                 09 THU-SALES OCCURS 2 TIMES.                     NC4014.2
011300                     11 AM-SALES PIC 9(3).                        NC4014.2
011400                     11 FRI-SALES OCCURS 2 TIMES.                 NC4014.2
011500                         13 PM-SALES PIC 9(3).                    NC4014.2
011600                                                                  NC4014.2
011700 01 VARIABLES.                                                    NC4014.2
011800     03 EDFIELD PIC Z,ZZZ.99.                                     NC4014.2
011900                                                                  NC4014.2
012000                                                                  NC4014.2
012100     03 STATE PIC X(4) VALUE ALL "A".                             NC4014.2
012200*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
012300                                                                  NC4014.2
012400                                                                  NC4014.2
012500     03 RKEY PIC 9(8) VALUE ZERO.                                 NC4014.2
012600                                                                  NC4014.2
012700                                                                  NC4014.2
012800                                                                  NC4014.2
012900     03 GRANDTOTAL PIC 9(7)V99 VALUE ZERO.                        NC4014.2
013000     03 BOX-A PIC 99 VALUE ZERO.                                  NC4014.2
013100     03 BOX-B PIC 99 VALUE ZERO.                                  NC4014.2
013200     03 BOX-C PIC 999 VALUE ZERO.                                 NC4014.2
013300     03 BOX-D PIC 999 VALUE ZERO.                                 NC4014.2
013400                                                                  NC4014.2
013500                                                                  NC4014.2
013600     03 MARYPOPPINS PIC X(34) VALUE "SUPERCALIFRAGILISTICEXPIALIDONC4014.2
013700-         "CIOUS".                                                NC4014.2
013800     03 MP-1 REDEFINES MARYPOPPINS.                               NC4014.2
013900        04  MP-1-A  PICTURE X(5).                                 NC4014.2
014000        04  MP-1-A-1  REDEFINES MP-1-A.                           NC4014.2
014100*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
014200             05  MP-1-A-1-A  PICTURE X(3).                        NC4014.2
014300             05  FILLER  PICTURE X(2).                            NC4014.2
014400        04  FILLER  PICTURE X(29).                                NC4014.2
014500                                                                  NC4014.2
014600     03 VARA PIC X(4).                                            NC4014.2
014700     03 VARB PIC X(4).                                            NC4014.2
014800     03 VARC PIC X(4).                                            NC4014.2
014900         66 VARA                                                  NC4014.2
015000*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
015100                 RENAMES VARB THRU VARC.                          NC4014.2
015200*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
015300                                                                  NC4014.2
015400                                                                  NC4014.2
015500 01 DDAY PIC 9(5).                                                NC4014.2
015600                                                                  NC4014.2
015700 01 VARD PIC X(4).                                                NC4014.2
015800                                                                  NC4014.2
015900 01 VARE PIC X(4).                                                NC4014.2
016000                                                                  NC4014.2
016100 01 VARF PIC 9(7)V99.                                             NC4014.2
016200                                                                  NC4014.2
016300 PROCEDURE DIVISION.                                              NC4014.2
016400                                                                  NC4014.2
016500                                                                  NC4014.2
016600 NC401M-CONTROL.                                                  NC4014.2
016700     OPEN INPUT TFIL.                                             NC4014.2
016800     PERFORM NC401M-COLON THRU NC401M-END 1 TIMES.                NC4014.2
016900     ALTER NC401M-GOTO TO PROCEED TO NC401M-GOTO-2,               NC4014.2
017000         NC401M-GOTO-2 TO PROCEED TO NC401M-NESTIF.               NC4014.2
017100*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
017200     CLOSE TFIL.                                                  NC4014.2
017300     STOP RUN.                                                    NC4014.2
017400                                                                  NC4014.2
017500                                                                  NC4014.2
017600 NC401M-COLON.                                                    NC4014.2
017700     DISPLAY COLONTEST(1:20).                                     NC4014.2
017800*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
017900                                                                  NC4014.2
018000                                                                  NC4014.2
018100 NC401M-QUALIF.                                                   NC4014.2
018200     MOVE GUBBINS OF FREC TO GUBBINS OF FREC-2.                   NC4014.2
018300*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
018400                                                                  NC4014.2
018500                                                                  NC4014.2
018600 NC401M-SUBSCR.                                                   NC4014.2
018700     MOVE ZERO TO                                                 NC4014.2
018800        PM-SALES (BOX-A, BOX-B, BOX-C, BOX-D, 1).                 NC4014.2
018900*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
019000                                                                  NC4014.2
019100 NC401M-CHARBR1.                                                  NC4014.2
019200     MUL                                                          NC4014.2
019300-    TIPLY BOX-A BY BOX-B GIVING BOX-C.                           NC4014.2
019400*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
019500                                                                  NC4014.2
019600                                                                  NC4014.2
019700 NC401M-CHARBR2.                                                  NC4014.2
019800     MOVE 2                                                       NC4014.2
019900-    0 TO BOX-A.                                                  NC4014.2
020000*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
020100                                                                  NC4014.2
020200 NC401M-ARITHEXP.                                                 NC4014.2
020300     IF BOX-A + 1 IS NOT GREATER THAN BOX-B + 2                   NC4014.2
020400         DISPLAY "ARITHEXP-TEST".                                 NC4014.2
020500*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
020600                                                                  NC4014.2
020700                                                                  NC4014.2
020800 NC401M-SIGCOND.                                                  NC4014.2
020900     IF BOX-A IS NOT NEGATIVE                                     NC4014.2
021000         DISPLAY "SIGCOND-TEST".                                  NC4014.2
021100*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
021200                                                                  NC4014.2
021300                                                                  NC4014.2
021400 NC401M-COMPCOND.                                                 NC4014.2
021500     IF BOX-A IS GREATER THAN BOX-B AND NOT BOX-C IS GREATER      NC4014.2
021600         THAN BOX-A THEN MOVE 7 TO BOX-B.                         NC4014.2
021700*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
021800                                                                  NC4014.2
021900                                                                  NC4014.2
022000 NC401M-CORRESADD.                                                NC4014.2
022100     ADD CORRESPONDING GROUP-1 TO GROUP-2.                        NC4014.2
022200*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
022300                                                                  NC4014.2
022400                                                                  NC4014.2
022500 NC401M-CORRESMOVE.                                               NC4014.2
022600     MOVE CORRESPONDING GROUP-1 TO GROUP-2.                       NC4014.2
022700*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
022800                                                                  NC4014.2
022900                                                                  NC4014.2
023000 NC401M-CORRESSUB.                                                NC4014.2
023100     SUBTRACT CORRESPONDING GROUP-2 FROM GROUP-1.                 NC4014.2
023200*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
023300                                                                  NC4014.2
023400                                                                  NC4014.2
023500 NC401M-COMPUTE.                                                  NC4014.2
023600     COMPUTE BOX-A = 10 + 6.                                      NC4014.2
023700*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
023800                                                                  NC4014.2
023900                                                                  NC4014.2
024000 NC401M-GETDAY.                                                   NC4014.2
024100     ACCEPT DDAY FROM DAY-OF-WEEK.                                NC4014.2
024200*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
024300                                                                  NC4014.2
024400                                                                  NC4014.2
024500 NC401M-DISPUPON.                                                 NC4014.2
024600     DISPLAY "PFILE" UPON VDUNIT.                                 NC4014.2
024700*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
024800                                                                  NC4014.2
024900                                                                  NC4014.2
025000 NC401-DIVREMAINDER.                                              NC4014.2
025100     DIVIDE BOX-A INTO BOX-B GIVING BOX-C REMAINDER BOX-D.        NC4014.2
025200*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
025300                                                                  NC4014.2
025400                                                                  NC4014.2
025500 NC401M-EVAL.                                                     NC4014.2
025600     EVALUATE BOX-A                                               NC4014.2
025700         WHEN 1 MOVE "A" TO VARC                                  NC4014.2
025800         WHEN 2 MOVE "B" TO VARC.                                 NC4014.2
025900*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
026000                                                                  NC4014.2
026100                                                                  NC4014.2
026200 NC401M-GOTO.                                                     NC4014.2
026300     GO TO.                                                       NC4014.2
026400*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
026500                                                                  NC4014.2
026600 NC401M-GOTO-2.                                                   NC4014.2
026700     GO TO.                                                       NC4014.2
026800*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
026900                                                                  NC4014.2
027000                                                                  NC4014.2
027100 NC401M-NESTIF.                                                   NC4014.2
027200     IF BOX-A IS GREATER THAN BOX-B THEN                          NC4014.2
027300         MOVE "AAAA" TO VARD                                      NC4014.2
027400     ELSE                                                         NC4014.2
027500     IF BOX-B IS GREATER THAN BOX-C THEN                          NC4014.2
027600         MOVE "BBBB" TO VARD                                      NC4014.2
027700     ELSE                                                         NC4014.2
027800         MOVE "CCCC" TO VARD.                                     NC4014.2
027900*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
028000                                                                  NC4014.2
028100                                                                  NC4014.2
028200 NC401M-INIT.                                                     NC4014.2
028300     INITIALIZE VARB.                                             NC4014.2
028400*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
028500                                                                  NC4014.2
028600                                                                  NC4014.2
028700 NC401M-INSCT.                                                    NC4014.2
028800     INSPECT MARYPOPPINS CONVERTING "A" TO "Z".                   NC4014.2
028900*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
029000                                                                  NC4014.2
029100                                                                  NC4014.2
029200                                                                  NC4014.2
029300 NC401M-PWT.                                                      NC4014.2
029400     PERFORM NC401M-NESTIF THRU NC401M-INIT WITH TEST AFTER       NC4014.2
029500         UNTIL BOX-B IS EQUAL TO BOX-A.                           NC4014.2
029600*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
029700                                                                  NC4014.2
029800                                                                  NC4014.2
029900 NC401M-PWV.                                                      NC4014.2
030000     PERFORM NC401M-NESTIF THRU NC401M-INIT                       NC4014.2
030100     VARYING BOX-A FROM BOX-B BY BOX-C                            NC4014.2
030200         UNTIL GRANDTOTAL IS EQUAL TO VARF.                       NC4014.2
030300*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
030400                                                                  NC4014.2
030500                                                                  NC4014.2
030600 NC401M-SEARCH.                                                   NC4014.2
030700     SEARCH TEST-CODE                                             NC4014.2
030800         WHEN BOX-A IS EQUAL TO BOX-B                             NC4014.2
030900             NEXT SENTENCE.                                       NC4014.2
031000*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
031100                                                                  NC4014.2
031200                                                                  NC4014.2
031300 NC401M-STT.                                                      NC4014.2
031400     SET CUST-PAID TO TRUE.                                       NC4014.2
031500*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
031600                                                                  NC4014.2
031700                                                                  NC4014.2
031800 NC401M-ST.                                                       NC4014.2
031900     STRING VARD DELIMITED BY VARB INTO VARC.                     NC4014.2
032000*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
032100                                                                  NC4014.2
032200                                                                  NC4014.2
032300 NC401M-UST.                                                      NC4014.2
032400     UNSTRING VARD INTO VARE.                                     NC4014.2
032500*Message expected for above statement: NON-CONFORMING STANDARD    NC4014.2
032600                                                                  NC4014.2
032700                                                                  NC4014.2
032800 NC401M-END.                                                      NC4014.2
032900                                                                  NC4014.2
033000*TOTAL NUMBER OF FLAGS EXPECTED = 40.                             NC4014.2
033100*Message expected for following statement: NON-CONFORMING STANDARDNC4014.2
033200 END PROGRAM NC401M.                                              NC4014.2
      *END-OF,NC401M                                                                  
