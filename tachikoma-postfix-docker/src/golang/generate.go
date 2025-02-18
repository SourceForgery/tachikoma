package main

//go:generate /bin/sh -c "mkdir -p $(pwd -P)/build/generated"
//go:generate /bin/sh -c "GOBIN=$(pwd -P)/build go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.36.4"
//go:generate /bin/sh -c "GOBIN=$(pwd -P)/build go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.2"
//go:generate /bin/sh -c "protoc --plugin=$(pwd -P)/build/protoc-gen-go --go_out=build/generated -I=../../../tachikoma-backend-api-proto/src/main/proto/ ../../../tachikoma-backend-api-proto/src/main/proto/com/sourceforgery/tachikoma/mta/delivery_notifications.proto ../../../tachikoma-backend-api-proto/src/main/proto/com/sourceforgery/tachikoma/mta/message_queue.proto"
//go:generate /bin/sh -c "protoc --plugin=$(pwd -P)/build/protoc-gen-go-grpc --go-grpc_out=build/generated -I=../../../tachikoma-backend-api-proto/src/main/proto/ ../../../tachikoma-backend-api-proto/src/main/proto/com/sourceforgery/tachikoma/mta/delivery_notifications.proto ../../../tachikoma-backend-api-proto/src/main/proto/com/sourceforgery/tachikoma/mta/message_queue.proto"
