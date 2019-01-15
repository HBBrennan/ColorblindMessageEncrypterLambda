SRC_DIR = src/main/java/ColorblindMessageEncrypter
SRC = $(wildcard $(SRC_DIR)/*.java)
TARGET = target/output.jar

.PHONY: all test debug

all: test

build: $(TARGET)

$(TARGET): $(SRC)
	mvn package shade:shade
	mv target/colorblind-message-encrypter-createplatehandler-1.0-SNAPSHOT.jar $(TARGET)

test: $(TARGET)
	sam local invoke -e test.json

debug: $(TARGET)
	sam local invoke -e test.json -d 5858
