log = LOAD 'excite-small.log' AS (user, ts, q);
grpd = GROUP log by user;
cntd = FOREACH grpd GENERATE group, COUNT(log) AS cnt;
fltrd = FILTER cntd BY cnt > 50;
srtd = ORDER fltrd BY cnt;
STORE srtd INTO 'output';

