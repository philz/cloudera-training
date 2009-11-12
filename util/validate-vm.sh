#!/usr/bin/env bash
#
# (c) Copyright 2009 Cloudera, Inc.
#
# This script will validate that the virtual machine works as intended
# and that all exercises function correctly. It does this by executing
# a number of example programs and solutions.
#
# Running this script will result in a reset of your ~/git directory


function exitmethod() {
  echo "TESTS FAILED"
  exit 1
}

function reset_git_dir() {
  # Get the git directory looking like it should.
  echo "Resetting git repository state..."
  pushd "${bin}/.."
  git reset --hard HEAD
  git clean -f -d .
  popd
}

function clear_hadoop_logs() {
  echo "Cleaning up Hadoop log files..."
  sudo rm -rf /var/log/hadoop/*
}

function clear_hdfs_dirs() {
  echo "Resetting HDFS directories..."
  hadoop fs -rmr /user/training
  hadoop fs -rmr /tmp
  hadoop fs -mkdir /user/training
  hadoop fs -mkdir /tmp
}

function test_hdfs() {
  echo "Testing HDFS"
  hadoop fs -touchz tmpfile
  hadoop fs -rm tmpfile
  hadoop fs -ls
  hadoop fs -ls /
}

function test_mapred() {
  echo "Testing MR Basics (pi)"
  hadoop jar /usr/lib/hadoop/hadoop-*-examples.jar pi 5 5000

  echo "Testing MR Basics (wordcount)"
  pushd ${bin}/../data/
  tar xzf shakespeare.tar.gz
  hadoop fs -put input input
  popd
  hadoop jar /usr/lib/hadoop/hadoop-*-examples.jar grep input grep_output "[Ww]herefore"
  hadoop fs -rmr grep_output

  echo "Testing job list command"
  hadoop job -list
}

function test_streaming() {
  echo "Testing streaming basics"
  hadoop fs -put ${bin}/../data/excite-small.log excite
  hadoop jar /usr/lib/hadoop/contrib/streaming/hadoop-*-streaming.jar \
      -input excite -output exciteout \
      -mapper cat -reducer 'wc -l' \
      -numReduceTasks 1
  hadoop fs -rmr excite
  hadoop fs -rmr exciteout
}

function test_line_index_mapred() {
  echo "Testing index exercise"
  # input directory is already in place from wordcount test.
  pushd ${bin}/../exercises/shakespeare
  cp solution/java/index/* stub-src/src/index
  ant clean jar
  ant test
  hadoop jar indexer.jar index.LineIndexer
  hadoop fs -test -d output
  hadoop fs -test -e output
  hadoop fs -rmr output
  hadoop fs -rmr input
  popd
}

function test_line_index_streaming() {
  echo "Testing index exercise (streaming)"
  pushd ${bin}/../data/
  rm -rf input
  tar zxf shakespeare-streaming.tar.gz
  hadoop fs -put input input
  popd
  PY_INDEX_MAP=${bin}/../exercises/shakespeare/solution/python/mapper.py
  PY_INDEX_REDUCE=${bin}/../exercises/shakespeare/solution/python/reducer.py
  pushd ${bin}/../exercises/shakespeare/test/streaming
  ./test-mapper.sh ${PY_INDEX_MAP}
  ./test-reducer.sh ${PY_INDEX_REDUCE}
  hadoop jar /usr/lib/hadoop/contrib/streaming/hadoop-*-streaming.jar \
      -input input -output output \
      -file ${PY_INDEX_MAP} -file ${PY_INDEX_REDUCE} \
      -mapper `basename ${PY_INDEX_MAP}` -reducer `basename ${PY_INDEX_REDUCE}` \
      -inputformat org.apache.hadoop.mapred.KeyValueTextInputFormat
  hadoop fs -cat output/part-00000 > /dev/null
  popd
}

function test_sqoop() {
  echo "Testing Sqoop"
  sqoop --connect jdbc:mysql://localhost/training --username training --list-tables
  sqoop --connect jdbc:mysql://localhost/training --username training --table bible_freq \
      --hive-import --direct --fields-terminated-by '\t' --lines-terminated-by '\n'
  hadoop fs -ls /user/hive/warehouse
  hadoop fs -test -d /user/hive/warehouse/bible_freq
  hadoop fs -test -e /user/hive/warehouse/bible_freq/data-00000
  hive -e 'SHOW TABLES'
  hive -e 'DESCRIBE bible_freq'
  hive -e 'SELECT * FROM bible_freq LIMIT 2'
  sqoop --connect jdbc:mysql://localhost/training --username training --table shake_freq \
      --hive-import --direct --fields-terminated-by '\t' --lines-terminated-by '\n'
}

function test_hive() {
  echo "Testing Hive"
  hive -e 'SELECT * FROM shake_freq LIMIT 2'
  hive -e 'EXPLAIN SELECT * FROM shake_freq LIMIT 2' > /dev/null
  hive -e 'SELECT * FROM shake_freq WHERE freq > 100 SORT BY freq ASC LIMIT 1'
  hive -e 'SELECT freq, COUNT(1) AS f2 FROM shake_freq GROUP BY freq SORT BY f2 DESC LIMIT 1'
  hive -e 'SELECT avg(freq) FROM shake_freq'
  hive -e 'CREATE TABLE merged (word STRING, shake_f INT, kjv_f INT)'
  hive -e 'INSERT OVERWRITE TABLE merged SELECT s.word, s.freq, b.freq FROM shake_freq s
      JOIN bible_freq b ON (s.word = b.word) WHERE s.freq >= 1 AND b.freq >= 1'
  hive -e 'SELECT * FROM merged LIMIT 1'
  hive -e 'DROP TABLE merged'

  pushd ${bin}/../exercises/shakespeare/sentence-idx
  ./run-indexer input
  popd
  hadoop fs -test -d input_idx
}

function test_pig() {
  echo "Testing pig"
  pushd ${bin}/../data
  pig -x local -f ${bin}/testscripts/counts.pig
  [[ -f output ]]
  rm output

  pig -x local -f ${bin}/testscripts/filtered.pig
  [[ -f output ]]
  rm output

  if hadoop fs -test -d output; then
    hadoop fs -rmr output
  fi
  pig -f ${bin}/testscripts/join.pig
  hadoop fs -test -d output
  hadoop fs -rmr output

  pig -f ${bin}/testscripts/antijoin.pig
  hadoop fs -test -d output
  hadoop fs -rmr output

  popd

  pushd ${bin}/../exercises/intro-to-pig/udf
  ant
  pig -f ${bin}/../exercises/intro-to-pig/solution/high-freq-single-source.pig
  hadoop fs -test -d output
  hadoop fs -rmr output

  pig -f ${bin}/../exercises/intro-to-pig/solution/proper_nouns.pig
  hadoop fs -test -d proper_nouns
  hadoop fs -rmr proper_nouns
  popd
}

function drop_hive_tables() {
  echo "Cleaning up Hive tables..."

  set +e
  hive -e 'DROP TABLE shake_freq'
  hive -e 'DROP TABLE bible_freq'
  hive -e 'DROP TABLE merged'
  hadoop fs -test -d /user/hive/warehouse/shake_freq || \
      hadoop fs -rmr /user/hive/warehouse/shake_freq
  hadoop fs -test -d /user/hive/warehouse/bible_freq || \
      hadoop fs -rmr /user/hive/warehouse/bible_freq
  hadoop fs -rmr /user/hive/warehouse/merged
  set -e
}

set -e
set -x

trap exitmethod ERR

bin=`dirname $0`
bin=`cd ${bin} && pwd`

cd ${bin}

echo "Starting training VM self test"
date
echo "VM exercise version:"
cat "${bin}/../.update-branch"

echo "Some setup..."
reset_git_dir
hadoop dfsadmin -safemode wait
clear_hdfs_dirs
drop_hive_tables

test_hdfs
test_mapred
test_streaming
test_line_index_mapred
test_line_index_streaming
test_sqoop
test_hive
test_pig

echo "Cleaning up our dust..."
drop_hive_tables
reset_git_dir
clear_hdfs_dirs
clear_hadoop_logs

echo "ALL TESTS PASSED"

