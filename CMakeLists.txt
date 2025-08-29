cmake_minimum_required(VERSION 3.31)
project(MeldInstaller)

set(CMAKE_CXX_STANDARD 20)

# Find required packages
find_package(CURL REQUIRED)
find_package(nlohmann_json REQUIRED)
find_package(tinyxml2 REQUIRED)

# Get FLTK configuration using fltk-config
execute_process(COMMAND fltk-config --cxxflags OUTPUT_VARIABLE FLTK_CXXFLAGS OUTPUT_STRIP_TRAILING_WHITESPACE)
execute_process(COMMAND fltk-config --ldflags OUTPUT_VARIABLE FLTK_LDFLAGS OUTPUT_STRIP_TRAILING_WHITESPACE)

# Extract include directories from FLTK_CXXFLAGS
string(REGEX MATCHALL "-I[^ ]*" FLTK_INCLUDE_FLAGS ${FLTK_CXXFLAGS})
set(FLTK_INCLUDE_DIRS "")
foreach(INCLUDE_FLAG ${FLTK_INCLUDE_FLAGS})
    string(REGEX REPLACE "^-I" "" INCLUDE_DIR ${INCLUDE_FLAG})
    list(APPEND FLTK_INCLUDE_DIRS ${INCLUDE_DIR})
endforeach()

# Extract libraries from FLTK_LDFLAGS
string(REGEX MATCHALL "-l[^ ]*" FLTK_LIB_FLAGS ${FLTK_LDFLAGS})
set(FLTK_LIBRARIES "")
foreach(LIB_FLAG ${FLTK_LIB_FLAGS})
    string(REGEX REPLACE "^-l" "" LIB_NAME ${LIB_FLAG})
    list(APPEND FLTK_LIBRARIES ${LIB_NAME})
endforeach()

add_executable(MeldInstaller main.cpp)

target_link_libraries(MeldInstaller PRIVATE
        CURL::libcurl
        nlohmann_json::nlohmann_json
        tinyxml2::tinyxml2
        ${FLTK_LIBRARIES}
)

target_include_directories(MeldInstaller PRIVATE ${FLTK_INCLUDE_DIRS})

# Try to link statically where possible for smaller dependency footprint
target_link_options(MeldInstaller PRIVATE -static-libgcc -static-libstdc++)
