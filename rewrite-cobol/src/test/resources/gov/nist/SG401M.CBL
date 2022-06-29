      *HEADER,COBOL,SG401M                                                            
000100 IDENTIFICATION DIVISION.                                         SG4014.2
000200 PROGRAM-ID.                                                      SG4014.2
000300     SG401M.                                                      SG4014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF LEVEL 2              SG4014.2
000500*FEATURES OF THE SEGMENTATION MODULE.                             SG4014.2
000600 ENVIRONMENT DIVISION.                                            SG4014.2
000700 CONFIGURATION SECTION.                                           SG4014.2
000800 SOURCE-COMPUTER.                                                 SG4014.2
000900     XXXXX082.                                                    SG4014.2
001000 OBJECT-COMPUTER.                                                 SG4014.2
001100     XXXXX083                                                     SG4014.2
001200     SEGMENT-LIMIT IS 18.                                         SG4014.2
001300*Message expected for above statement: NON-CONFORMING STANDARD    SG4014.2
001400                                                                  SG4014.2
001500                                                                  SG4014.2
001600 DATA DIVISION.                                                   SG4014.2
001700 PROCEDURE DIVISION.                                              SG4014.2
001800                                                                  SG4014.2
001900 NUMBER1 SECTION 18.                                              SG4014.2
002000                                                                  SG4014.2
002100 SG401M-CONTROL.                                                  SG4014.2
002200     EXIT.                                                        SG4014.2
002300                                                                  SG4014.2
002400 NUMBER2 SECTION 19.                                              SG4014.2
002500                                                                  SG4014.2
002600 SG401M-DUMMY.                                                    SG4014.2
002700     DISPLAY "THIS IS A DUMMY PARAGRAPH".                         SG4014.2
002800                                                                  SG4014.2
002900 NUMBER3 SECTION 18.                                              SG4014.2
003000*Message expected for above statement: NON-CONFORMING STANDARD    SG4014.2
003100 SG401M-DUMMY2.                                                   SG4014.2
003200     DISPLAY "THIS IS A DUMMY PARAGRAPH TOO!".                    SG4014.2
003300                                                                  SG4014.2
003400                                                                  SG4014.2
003500* A MESSAGE IS EXPECTED FOR THE EXISTENCE OF TWO SECTIONS         SG4014.2
003600* WITH THE SAME SECTION NUMBER THAT ARE NOT                       SG4014.2
003700* "PHYSICALLY CONTIGUOUS IN THE SOURCE PROGRAM".                  SG4014.2
003800                                                                  SG4014.2
003900*TOTAL NUMBER OF FLAGS EXPECTED = 2.                              SG4014.2
      *END-OF,SG401M                                                                  
