cmake_minimum_required(VERSION 3.31)
project(MeldInstaller)

set(CMAKE_CXX_STANDARD 20)

find_package(CURL REQUIRED)
find_package(nlohmann_json REQUIRED)
find_package(tinyxml2 REQUIRED)
find_package(FLTK REQUIRED)

add_executable(MeldInstaller WIN32 main.cpp)

target_link_libraries(MeldInstaller PRIVATE
        CURL::libcurl
        nlohmann_json::nlohmann_json
        tinyxml2::tinyxml2
        fltk::fltk
)

if (APPLE)
    target_link_options(MeldInstaller PRIVATE -framework Cocoa)
elseif (WIN32 AND CMAKE_CXX_COMPILER_ID STREQUAL "MSVC")
    # MSVC doesn't need static linking flags
else ()
    target_link_options(MeldInstaller PRIVATE -static-libgcc -static-libstdc++)
endif ()
