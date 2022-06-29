      *HEADER,COBOL,CM401M                                                            
000100 IDENTIFICATION DIVISION.                                         CM4014.2
000200 PROGRAM-ID.                                                      CM4014.2
000300      CM401M.                                                     CM4014.2
000400*The following program tests the flagging of level 2              CM4014.2
000500*features of the communication module.                            CM4014.2
000600 ENVIRONMENT DIVISION.                                            CM4014.2
000700 CONFIGURATION SECTION.                                           CM4014.2
000800 SOURCE-COMPUTER.                                                 CM4014.2
000900     XXXXX082.                                                    CM4014.2
001000 OBJECT-COMPUTER.                                                 CM4014.2
001100     XXXXX083.                                                    CM4014.2
001200 DATA DIVISION.                                                   CM4014.2
001300 FILE SECTION.                                                    CM4014.2
001400 COMMUNICATION SECTION.                                           CM4014.2
001500 CD COMMNAME FOR INITIAL INPUT                                    CM4014.2
001600*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
001700     SYMBOLIC SUB-QUEUE-1 IS CQ.                                  CM4014.2
001800*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
001900 01 CREC.                                                         CM4014.2
002000     03 CNAME1   PIC X(8).                                        CM4014.2
002100     03 CQ       PIC 9(8).                                        CM4014.2
002200     03 FILLER   PIC X(62).                                       CM4014.2
002300     03 CINT     PIC 9.                                           CM4014.2
002400     03 FILLER   PIC X(8).                                        CM4014.2
002500                                                                  CM4014.2
002600 CD COMM2 FOR OUTPUT                                              CM4014.2
002700     DESTINATION TABLE OCCURS 7 TIMES.                            CM4014.2
002800*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
002900                                                                  CM4014.2
003000 PROCEDURE DIVISION.                                              CM4014.2
003100                                                                  CM4014.2
003200 CM401M-CONTROL.                                                  CM4014.2
003300     PERFORM CM401M-DISABLE THRU CM401M-SENDREP.                  CM4014.2
003400     STOP RUN.                                                    CM4014.2
003500                                                                  CM4014.2
003600 CM401M-DISABLE.                                                  CM4014.2
003700     DISABLE INPUT COMMNAME WITH KEY CNAME1.                      CM4014.2
003800*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
003900                                                                  CM4014.2
004000 CM401M-ENABLE.                                                   CM4014.2
004100     ENABLE INPUT COMMNAME WITH KEY CNAME1.                       CM4014.2
004200*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
004300                                                                  CM4014.2
004400                                                                  CM4014.2
004500 CM401M-PURGE.                                                    CM4014.2
004600     PURGE COMM2.                                                 CM4014.2
004700*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
004800                                                                  CM4014.2
004900 CM401M-SEND.                                                     CM4014.2
005000     SEND COMM2 FROM CNAME1.                                      CM4014.2
005100*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
005200                                                                  CM4014.2
005300 CM401M-SENDID.                                                   CM4014.2
005400     SEND COMM2 FROM CNAME1 WITH CINT.                            CM4014.2
005500*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
005600                                                                  CM4014.2
005700 CM401M-SENDESI.                                                  CM4014.2
005800     SEND COMM2 FROM CNAME1 WITH ESI.                             CM4014.2
005900*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
006000                                                                  CM4014.2
006100 CM401M-SENDREP.                                                  CM4014.2
006200     SEND COMM2 WITH EMI REPLACING LINE.                          CM4014.2
006300*Message expected for above statement: NON-CONFORMING STANDARD    CM4014.2
006400                                                                  CM4014.2
006500*TOTAL NUMBER OF FLAGS EXPECTED = 10.                             CM4014.2
      *END-OF,CM401M                                                                  
