#Example Usage;
#./test.sh CopyOfTest

#file location of CopyOfTest ./src/test/CopyOfTest.java

file="$1"
if [ -z "$file" ]; then
    file="model_players_Player"
fi
~/opt/java/jdk1.8.0_162/bin/java -Xmx800m -cp '.:bin:/usr/shared/java/junit4.jar:deps/c3p0-0.9.5.1.jar:deps/c3p0-0.9.5-pre10.jar:deps/commons-io-2.4.jar:deps/commons-lang3-3.3.2.jar:deps/everythingrs-api.jar:deps/gson-2.2.4.jar:deps/GTLVote.jar:deps/netty.jar:deps/json-lib-2.4-jdk15.jar:deps/json-simple-1.1.1.jar:deps/guava-18.0.jar:deps/everythingrs-api.jar:deps/Motivote-server.jar:deps/mvgate3.jar:d' test.$file
