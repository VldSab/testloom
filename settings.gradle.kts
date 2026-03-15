rootProject.name = "testloom"

include(
    "testloom-core",
    "testloom-spring-boot-starter",
    "testloom-cli",
    "testloom-examples:mvc-postgres-demo",
    "testloom-examples:grpc-postgres-demo"
)

include("testloom-examples:mvc-hello-recorder-demo")