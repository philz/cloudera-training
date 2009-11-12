bard = LOAD '/user/hive/warehouse/shake_freq' USING PigStorage('\t') AS (freq, word);
kjv = LOAD '/user/hive/warehouse/bible_freq' USING PigStorage('\t') AS (freq, word);
inboth = JOIN bard BY word, kjv BY word;
STORE inboth INTO 'output';

