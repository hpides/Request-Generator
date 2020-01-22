# CMakeList.txt: CMake-Projektdatei der obersten Ebene. Führen Sie hier die globale Konfiguration aus,
# und schließen Sie Unterprojekte ein.
#
cmake_minimum_required (VERSION 3.8)

project ("FiberEvaluation")

# Schließen Sie Unterprojekte ein.
add_subdirectory ("FiberEvaluation")
SET(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -mtune=native -march=native")

