SRC_DIR = src/main/java/ColorblindMessageEncrypter
SRC = $(wildcard $(SRC_DIR)/*.java)
TARGET = target/output.jar

.PHONY: all test debug deploy clean

all: test

build: $(TARGET)

$(TARGET): $(SRC)
	mvn package shade:shade -q
	mv target/colorblind-message-encrypter-createplatehandler-1.0-SNAPSHOT.jar $(TARGET)

test: $(TARGET)
	sam local invoke -e test.json --region us-west-2

# Attach debugger to process for debugging
debug: $(TARGET)
	sam local invoke -e test.json -d 5858 --region us-west-2 --debug

# Deploy Locally
deploy: $(TARGET)
	aws cloudformation package --template-file template.yml --s3-bucket colorblind-message-encrypter-lambda --output-template-file output.yml
	sam deploy --template-file ./output.yml --stack-name ColorBlindStack --capabilities CAPABILITY_IAM

clean:
	rm -f output.yml
	rm -f $(TARGET)
	rm -rf target/*
