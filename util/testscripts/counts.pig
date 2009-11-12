log = LOAD 'excite-small.log' AS (user, ts, q);
grpd = GROUP log by user;
cntd = FOREACH grpd GENERATE group, COUNT(log);
STORE cntd INTO 'output';

