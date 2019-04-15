#!/bin/bash

let STARTTIME=$(date +%s)
typeset -i i=0
declare -a pid=()
while (( i < 100 )); do
 curl -s -u user:user -o ferrari.jpg http://localhost:8080/download-file/ferrari.jpg &
 pids[${i}]=$!
 let i+=1
done
for pid in ${pids[*]}; do
    wait $pid
done
let ENDTIME=$(date +%s)
let ELAPSED=$ENDTIME-$STARTTIME
echo "$ELAPSED seconds"

let STARTTIME=$(date +%s)
typeset -i i=0
declare -a pid=()
while (( i < 100 )); do
 curl -s -u user:user -o ferrari.jpg http://localhost:8080/download-file-async/ferrari.jpg &
 pids[${i}]=$!
 let i+=1
done
for pid in ${pids[*]}; do
    wait $pid
done
let ENDTIME=$(date +%s)
let ELAPSED=$ENDTIME-$STARTTIME
echo "$ELAPSED seconds"
