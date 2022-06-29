      *HEADER,COBOL,ST301M                                                            
000100 IDENTIFICATION DIVISION.                                         ST3014.2
000200 PROGRAM-ID.                                                      ST3014.2
000300      ST301M.                                                     ST3014.2
000400*The following program tests the flagging of intermediate         ST3014.2
000500*subset  features that are used in sort-merge functions           ST3014.2
000600 ENVIRONMENT DIVISION.                                            ST3014.2
000700 CONFIGURATION SECTION.                                           ST3014.2
000800 SOURCE-COMPUTER.                                                 ST3014.2
000900     XXXXX082.                                                    ST3014.2
001000 OBJECT-COMPUTER.                                                 ST3014.2
001100     XXXXX083.                                                    ST3014.2
001200 INPUT-OUTPUT SECTION.                                            ST3014.2
001300 FILE-CONTROL.                                                    ST3014.2
001400     SELECT TFIL ASSIGN                                           ST3014.2
001500     XXXXX027.                                                    ST3014.2
001600     SELECT TFIL-2 ASSIGN                                         ST3014.2
001700     XXXXX001                                                     ST3014.2
001800     ACCESS MODE IS SEQUENTIAL.                                   ST3014.2
001900     SELECT TFIL-3 ASSIGN                                         ST3014.2
002000     XXXXX002                                                     ST3014.2
002100     ACCESS MODE IS SEQUENTIAL.                                   ST3014.2
002200     SELECT TFIL-4 ASSIGN                                         ST3014.2
002300     XXXXX003                                                     ST3014.2
002400     ACCESS MODE IS SEQUENTIAL.                                   ST3014.2
002500     SELECT TFIL-5 ASSIGN                                         ST3014.2
002600     XXXXX004                                                     ST3014.2
002700     ACCESS MODE IS SEQUENTIAL.                                   ST3014.2
002800                                                                  ST3014.2
002900 I-O-CONTROL.                                                     ST3014.2
003000     SAME SORT-MERGE AREA FOR TFIL-5, TFIL.                       ST3014.2
003100*Message expected for above statement: NON-CONFORMING STANDARD    ST3014.2
003200 DATA DIVISION.                                                   ST3014.2
003300 FILE SECTION.                                                    ST3014.2
003400 SD  TFIL.                                                        ST3014.2
003500*Message expected for above statement: NON-CONFORMING STANDARD    ST3014.2
003600 01  FREC.                                                        ST3014.2
003700     03 DATA-1  PIC X(80).                                        ST3014.2
003800                                                                  ST3014.2
003900 FD TFIL-2.                                                       ST3014.2
004000 01 FREC-2.                                                       ST3014.2
004100     03 DATA-2  PIC X(80).                                        ST3014.2
004200                                                                  ST3014.2
004300 FD TFIL-3.                                                       ST3014.2
004400 01 FREC-3.                                                       ST3014.2
004500     03 DATA-3 PIC X(80).                                         ST3014.2
004600                                                                  ST3014.2
004700 FD TFIL-4.                                                       ST3014.2
004800 01 FREC-4.                                                       ST3014.2
004900     03 DATA-4 PIC X(80).                                         ST3014.2
005000                                                                  ST3014.2
005100 FD TFIL-5.                                                       ST3014.2
005200 01 FREC-5.                                                       ST3014.2
005300    03  DATA-5 PIC X(80).                                         ST3014.2
005400                                                                  ST3014.2
005500 PROCEDURE DIVISION.                                              ST3014.2
005600                                                                  ST3014.2
005700 ST301M-CONTROL.                                                  ST3014.2
005800     PERFORM ST301M-MERGE THRU ST301M-SORT 1 TIMES.               ST3014.2
005900     STOP RUN.                                                    ST3014.2
006000                                                                  ST3014.2
006100 ST301M-MERGE.                                                    ST3014.2
006200     MERGE TFIL ON ASCENDING KEY DATA-1                           ST3014.2
006300     USING TFIL-2 TFIL-3                                          ST3014.2
006400     OUTPUT PROCEDURE IS ST301M-RETURN.                           ST3014.2
006500                                                                  ST3014.2
006600*Message expected for above statement: NON-CONFORMING STANDARD    ST3014.2
006700                                                                  ST3014.2
006800 ST301M-RELEASE.                                                  ST3014.2
006900     RELEASE FREC.                                                ST3014.2
007000*Message expected for above statement: NON-CONFORMING STANDARD    ST3014.2
007100                                                                  ST3014.2
007200 ST301M-RETURN.                                                   ST3014.2
007300     RETURN TFIL RECORD                                           ST3014.2
007400         AT END DISPLAY "AT END".                                 ST3014.2
007500*Message expected for above statement: NON-CONFORMING STANDARD    ST3014.2
007600                                                                  ST3014.2
007700 ST301M-SORT.                                                     ST3014.2
007800     SORT TFIL ON ASCENDING KEY DATA-1                            ST3014.2
007900     INPUT PROCEDURE IS ST301M-RELEASE                            ST3014.2
008000     GIVING TFIL-4.                                               ST3014.2
008100*Message expected for above statement: NON-CONFORMING STANDARD    ST3014.2
008200                                                                  ST3014.2
008300                                                                  ST3014.2
008400*TOTAL NUMBER OF FLAGS EXPECTED = 6.                              ST3014.2
      *END-OF,ST301M                                                                  
