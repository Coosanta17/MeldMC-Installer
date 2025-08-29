cmake_minimum_required(VERSION 3.31)
project(MeldInstaller)

set(CMAKE_CXX_STANDARD 20)

# Find dependencies
find_package(Qt6 REQUIRED COMPONENTS Core Widgets)
find_package(PkgConfig REQUIRED)
pkg_check_modules(LIBCURL REQUIRED libcurl)
find_package(nlohmann_json REQUIRED)
find_package(tinyxml2 REQUIRED)

# Enable Qt MOC
set(CMAKE_AUTOMOC ON)

add_executable(MeldInstaller main.cpp)

# Link libraries
target_link_libraries(MeldInstaller 
    Qt6::Core 
    Qt6::Widgets 
    ${LIBCURL_LIBRARIES}
    nlohmann_json::nlohmann_json 
    tinyxml2::tinyxml2
)

# Include directories
target_include_directories(MeldInstaller PRIVATE ${LIBCURL_INCLUDE_DIRS})

# Compile definitions
target_compile_definitions(MeldInstaller PRIVATE ${LIBCURL_CFLAGS_OTHER})
