cmake_minimum_required(VERSION 3.16)
project(media_service VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_BUILD_TYPE Release)

find_package(Threads REQUIRED)
find_package(OpenSSL REQUIRED)

add_executable(media_service src/main.cpp)

target_include_directories(media_service PRIVATE include ${OPENSSL_INCLUDE_DIR})
target_link_libraries(media_service PRIVATE Threads::Threads OpenSSL::SSL OpenSSL::Crypto)

if(UNIX)
  target_link_libraries(media_service PRIVATE dl)
endif()

install(TARGETS media_service DESTINATION bin)
