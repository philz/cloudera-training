bard   = LOAD '/user/hive/warehouse/shake_freq' USING PigStorage('\t') AS (freq, word);
kjv    = LOAD '/user/hive/warehouse/bible_freq' USING PigStorage('\t') AS (freq, word);
grpd   = COGROUP bard BY word, kjv BY word;
nobard = FILTER grpd BY COUNT(bard) == 0;
out    = FOREACH nobard GENERATE FLATTEN(kjv);
STORE out INTO 'output';

