      *HEADER,COBOL,SG302M                                                            
000100 IDENTIFICATION DIVISION.                                         SG3024.2
000200 PROGRAM-ID.                                                      SG3024.2
000300     SG302M.                                                      SG3024.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      SG3024.2
000500*OBSOLETE FEATURES THAT ARE USED IN SEGMENTATION LEVEL 1.         SG3024.2
000600 ENVIRONMENT DIVISION.                                            SG3024.2
000700 CONFIGURATION SECTION.                                           SG3024.2
000800 SOURCE-COMPUTER.                                                 SG3024.2
000900     XXXXX082.                                                    SG3024.2
001000 OBJECT-COMPUTER.                                                 SG3024.2
001100     XXXXX083.                                                    SG3024.2
001200                                                                  SG3024.2
001300                                                                  SG3024.2
001400 DATA DIVISION.                                                   SG3024.2
001500 PROCEDURE DIVISION.                                              SG3024.2
001600 BEANO SECTION 1.                                                 SG3024.2
001700*Message expected for above statement: OBSOLETE                   SG3024.2
001800 SG302M-CONTROL.                                                  SG3024.2
001900     DISPLAY "THIS IS A DUMMY PARAGRAPH".                         SG3024.2
002000     STOP RUN.                                                    SG3024.2
002100*TOTAL NUMBER OF FLAGS EXPECTED = 1.                              SG3024.2
      *END-OF,SG302M                                                                  
