      *HEADER,COBOL,SG303M                                                            
000100 IDENTIFICATION DIVISION.                                         SG3034.2
000200 PROGRAM-ID.                                                      SG3034.2
000300     SG303M.                                                      SG3034.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      SG3034.2
000500*OBSOLETE FEATURES THAT ARE USED IN SEGMENTATION LEVEL 2.         SG3034.2
000600 ENVIRONMENT DIVISION.                                            SG3034.2
000700 CONFIGURATION SECTION.                                           SG3034.2
000800 SOURCE-COMPUTER.                                                 SG3034.2
000900     XXXXX082.                                                    SG3034.2
001000 OBJECT-COMPUTER.                                                 SG3034.2
001100     XXXXX083                                                     SG3034.2
001200     SEGMENT-LIMIT IS 20.                                         SG3034.2
001300*Message expected for above statement: OBSOLETE                   SG3034.2
001400 DATA DIVISION.                                                   SG3034.2
001500 PROCEDURE DIVISION.                                              SG3034.2
001600                                                                  SG3034.2
001700 NUMBER1 SECTION 18.                                              SG3034.2
001800*Message expected for above statement: OBSOLETE                   SG3034.2
001900                                                                  SG3034.2
002000 SG303M-CONTROL.                                                  SG3034.2
002100     EXIT.                                                        SG3034.2
002200                                                                  SG3034.2
002300 NUMBER2 SECTION 19.                                              SG3034.2
002400*Message expected for above statement: OBSOLETE                   SG3034.2
002500                                                                  SG3034.2
002600 SG303M-DUMMY1.                                                   SG3034.2
002700     DISPLAY "THIS IS A DUMMY PARAGRAPH".                         SG3034.2
002800                                                                  SG3034.2
002900 NUMBER3 SECTION 18.                                              SG3034.2
003000*Message expected for above statement: OBSOLETE                   SG3034.2
003100                                                                  SG3034.2
003200 SG303M-DUMMY2.                                                   SG3034.2
003300     DISPLAY "THIS IS A DUMMY PARAGRAPH TOO!".                    SG3034.2
003400                                                                  SG3034.2
003500                                                                  SG3034.2
003600                                                                  SG3034.2
003700*TOTAL NUMBER OF FLAGS EXPECTED = 4.                              SG3034.2
      *END-OF,SG303M                                                                  
