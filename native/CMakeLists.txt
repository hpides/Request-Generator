# CMakeList.txt: CMake-Projektdatei der obersten Ebene. Führen Sie hier die globale Konfiguration aus,
# und schließen Sie Unterprojekte ein.
#
cmake_minimum_required (VERSION 3.8)

project ("JNI")
#SET(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -mtune=native -march=native")
#SET(JAVA_INCLUDEDIRS  ". C:\\Program Files\\Java\\jdk-10.0.1\\include C:\\Program Files\\Java\\jdk-10.0.1\\include\\win32")
#set(SOURCES "de_hpi_tdgt_fibers_Fiber.cpp  C:\\Program Files\\Java\\jdk-10.0.1\\include\\jni.h  C:\\Program Files\\Java\\jdk-10.0.1\\include\\win32\\jni_md.h")
find_package(Java COMPONENTS Development)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
set(SOURCE_FILES "de_hpi_tdgt_fibers_Fiber.cpp" "de_hpi_tdgt_fibers_Fiber.h")
add_library(native SHARED ${SOURCE_FILES})
set_target_properties(native PROPERTIES LIBRARY_OUTPUT_DIRECTORY   "D:\\GitHub\\request-generator\\target")
add_custom_command(
        TARGET native POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy
                ${CMAKE_CURRENT_BINARY_DIR}/native.dll
                "D:\\GitHub\\request-generator")