#!/bin/bash
mvn package shade:shade
mv target/colorblind-message-encrypter-createplatehandler-1.0-SNAPSHOT.jar target/output.jar
sam local invoke -e test.json
