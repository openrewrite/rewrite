      *HEADER,CLBRY,K2PRA                                                             
000100     MOVE     FALSE-DATA-1 TO AREA-1.                             K2PRA4.2
000200     MOVE     FALSE-DATA-2 TO AREA-2.                             K2PRA4.2
000300     MOVE     FALSE-DATA-3 TO AREA-3.                             K2PRA4.2
000400     MOVE     FALSE-DATA-4 TO AREA-4.                             K2PRA4.2
000500     IF       TOTAL-AREA EQUAL TO "TRUE  TWO + 2 =    4"          K2PRA4.2
000600              PERFORM PASS ELSE PERFORM FAIL.                     K2PRA4.2
000700     GO       TO COPY-WRITE-16.                                   K2PRA4.2
      *END-OF,K2PRA                                                                   
