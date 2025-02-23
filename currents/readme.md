    /*
      eri bucketiin kun halutaan
      - eri retentiot
      - eri accessit
      - toisistaan riippumaton suorituskyky
      - jos luennan tai kirjoittamisen luonne on erilainen
      - eri kyttötarkoitus ja hallinta


      - Ripples
        - jatkuvaluonteinen liukuva ikkuna ostohalukkuudesta tilatuille osakkeille
        - täydennä
          - sama myyntihalukkuudelle
          - tee myös summaarinen kummallekin
          - tee spreadi ratjousten osto- ja myyntierotukselle
          - ja spreadi noille voluumeille
          - keino muuttaa tilattuja osakkeita - tehdään ingestiin

      - Currents
        - päivittäiset
          - trendianalyysi omalle indeksille
          - drawdownanalyysi omalle indeksille
        - täydennä
          - trendianalyysi
            - jokaiselle osakkeelle
            - vielä avoinna mitä datalla tehdään mutta tulokset tulee olla haettavissa annotaatioiksi muihin kaavioihin
          - drawdown
            - jokaiselle osakkeelle
            - ddippilaskuri - toistuvuus, koko ja kesto
          - 50 toteutuneilta volyymeiltään päivän suurinta osaketta, volyymeineen

        - Waves


      should do
      - discover the right params for trend regressions
      - freq table of quantics
        - trends
        - drawdown
      - trigger exec by event, maybe rest api
      - run the results also for all individual tickers
      - make a daily run or 5 times a day that always runs at least the last day, ie., drawdown must save latest up to prev bank day only
     */