#ps ux  | grep -P '(java|inotify)' | awk {'print $2'} | xargs kill -9
ps ux | grep -P '(watcher|inotifywait)' | awk {'print $2'} | xargs kill  -9
sleep 1;
nohup ../watcher bin/ --include='\.class$' --exec='@./run.sh' &
