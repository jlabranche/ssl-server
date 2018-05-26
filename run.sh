ps ux | grep java | grep -v javac | awk {'print $2'} | xargs kill  -9
sleep 1;
rm nohup.out
nohup ~/opt/java/jdk1.8.0_162/bin/java -Xmx800m -cp '.:bin:/usr/shared/java/junit4.jar:deps/c3p0-0.9.5.1.jar:deps/c3p0-0.9.5-pre10.jar:deps/commons-io-2.4.jar:deps/commons-lang3-3.3.2.jar:deps/everythingrs-api.jar:deps/gson-2.2.4.jar:deps/GTLVote.jar:deps/netty.jar:deps/json-lib-2.4-jdk15.jar:deps/json-simple-1.1.1.jar:deps/guava-18.0.jar:deps/everythingrs-api.jar:deps/Motivote-server.jar:deps/mvgate3.jar:d' ethos.Server &
sleep 1;
tail -f nohup.out
#java -Xmx800m -cp '.:bin:deps/poi.jar:deps/mysql.jar:deps/mina.jar:deps/slf4j.jar:deps/slf4j-nop.jar:deps/jython.jar:log4j-1.2.15.jar' server.Server
