      *HEADER,CLBRY,K2SEA                                                             
000100 PARA-1.                                                          K2SEA4.2
000200     ALTER    PARA-X TO PROCEED TO PARA-4.                        K2SEA4.2
000300 PARA-2.                                                          K2SEA4.2
000400     GO       TO 12345.                                           K2SEA4.2
000500 12345.                                                           K2SEA4.2
000600     PERFORM  FAIL.                                               K2SEA4.2
000700     GO       TO COPY-WRITE-15.                                   K2SEA4.2
000800 PARA-4.                                                          K2SEA4.2
000900     PERFORM  DUMMY-PASS.                                         K2SEA4.2
001000     GO       TO COPY-WRITE-15.                                   K2SEA4.2
      *END-OF,K2SEA                                                                   
