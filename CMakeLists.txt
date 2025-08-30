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
        ${FLTK_LIBRARIES}
)

target_include_directories(MeldInstaller PRIVATE ${FLTK_INCLUDE_DIRS})

# Try to link statically where possible for smaller dependency footprint
target_link_options(MeldInstaller PRIVATE -static-libgcc -static-libstdc++)
