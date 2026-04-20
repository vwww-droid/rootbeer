cmake_minimum_required(VERSION 3.4.1)

set(LLMOTIVATE_OMVLL_ENABLE OFF CACHE BOOL "Enable O-MVLL pass plugin for toolChecker.cpp")
set(LLMOTIVATE_OMVLL_PLUGIN "" CACHE FILEPATH "Path to O-MVLL pass plugin")
set(LLMOTIVATE_OMVLL_CONFIG "" CACHE FILEPATH "Path to O-MVLL Python config")
set(LLMOTIVATE_OMVLL_PYTHONPATH "" CACHE PATH "Path to bundled Python stdlib for O-MVLL")
set(LLMOTIVATE_OMVLL_OUTPUT_DIR "" CACHE PATH "Path to O-MVLL output directory")
set(LLMOTIVATE_OMVLL_COMPILER_LAUNCHER "" CACHE FILEPATH "Launcher script that injects O-MVLL environment variables")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fstack-protector-all")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,-z,relro -Wl,-z,now")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fstack-protector-all")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,-z,relro -Wl,-z,now -Wl,-z,max-page-size=16384")
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if (LLMOTIVATE_OMVLL_ENABLE)
    foreach(required_path
            LLMOTIVATE_OMVLL_PLUGIN
            LLMOTIVATE_OMVLL_CONFIG
            LLMOTIVATE_OMVLL_PYTHONPATH
            LLMOTIVATE_OMVLL_COMPILER_LAUNCHER)
        if (NOT EXISTS "${${required_path}}")
            message(FATAL_ERROR "LLMotivate O-MVLL 接入缺少路径: ${required_path}=${${required_path}}")
        endif()
    endforeach()

    if (LLMOTIVATE_OMVLL_OUTPUT_DIR STREQUAL "")
        message(FATAL_ERROR "LLMotivate O-MVLL 接入缺少输出目录: LLMOTIVATE_OMVLL_OUTPUT_DIR")
    endif()

    file(MAKE_DIRECTORY "${LLMOTIVATE_OMVLL_OUTPUT_DIR}")
endif()

add_library(toolChecker SHARED
        toolChecker.cpp)

if (LLMOTIVATE_OMVLL_ENABLE)
    target_compile_options(toolChecker PRIVATE
            "-fpass-plugin=${LLMOTIVATE_OMVLL_PLUGIN}")
    set_property(TARGET toolChecker PROPERTY CXX_COMPILER_LAUNCHER
            "${LLMOTIVATE_OMVLL_COMPILER_LAUNCHER};${LLMOTIVATE_OMVLL_CONFIG};${LLMOTIVATE_OMVLL_PYTHONPATH};${LLMOTIVATE_OMVLL_OUTPUT_DIR}")
endif()

target_link_libraries(toolChecker
        android
        log)
