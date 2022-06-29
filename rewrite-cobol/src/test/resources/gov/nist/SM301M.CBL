      *HEADER,COBOL,SM301M                                                            
000100 IDENTIFICATION DIVISION.                                         SM3014.2
000200 PROGRAM-ID.                                                      SM3014.2
000300      SM301M.                                                     SM3014.2
000400*The following program tests the flagging of the intermediate     SM3014.2
000500*subset COPY feature.                                             SM3014.2
000600 ENVIRONMENT DIVISION.                                            SM3014.2
000700 CONFIGURATION SECTION.                                           SM3014.2
000800 SOURCE-COMPUTER.                                                 SM3014.2
000900     XXXXX082.                                                    SM3014.2
001000 OBJECT-COMPUTER.                                                 SM3014.2
001100     XXXXX083.                                                    SM3014.2
001200                                                                  SM3014.2
001300                                                                  SM3014.2
001400 DATA DIVISION.                                                   SM3014.2
001500                                                                  SM3014.2
001600 PROCEDURE DIVISION.                                              SM3014.2
001700                                                                  SM3014.2
001800 SM301M-CONTROL.                                                  SM3014.2
001900     PERFORM SM301M-COPY.                                         SM3014.2
002000     STOP RUN.                                                    SM3014.2
002100                                                                  SM3014.2
002200 SM301M-COPY.                                                     SM3014.2
002300*Message expected for following statement: NON-CONFORMING STANDARDSM3014.2
002400     COPY KSM31.                                                  SM3014.2
002500                                                                  SM3014.2
002600                                                                  SM3014.2
002700*TOTAL NUMBER OF FLAGS EXPECTED = 1.                              SM3014.2
      *END-OF,SM301M                                                                  
